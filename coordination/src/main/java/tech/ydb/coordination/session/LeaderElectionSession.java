package tech.ydb.coordination.session;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.StatusCodesProtos;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.SemaphoreSession;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.SessionResponse;
import tech.ydb.coordination.observer.Observer;
import tech.ydb.coordination.settings.SessionSettings;
import tech.ydb.core.grpc.GrpcReadStream;

/**
 * @author Kirill Kurdyukov
 */
public class LeaderElectionSession {

    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionSession.class);

    private static final int START_SESSION_ID = 0;
    private static final int SESSION_KEEP_ALIVE_TIMEOUT_MS = 0;
    private static final int LIMIT_TOKENS_SEMAPHORE = 1;
    private static final int COUNT_TOKENS = 1;

    private final CoordinationClient coordinationClient;
    private final SessionSettings session;
    private final String coordinationNodePath;
    private final String semaphoreName;
    private final AtomicBoolean isWorking = new AtomicBoolean(false);
    private final AtomicReference<CoordinationSession> currentCoordinationSession = new AtomicReference<>();
    private final AtomicLong epochLeader = new AtomicLong();

    public LeaderElectionSession(
            CoordinationClient coordinationClient,
            SessionSettings settings,
            String coordinationNodePath
    ) {
        this.coordinationClient = coordinationClient;
        this.session = settings;
        this.coordinationNodePath = coordinationNodePath;
        this.semaphoreName = "leader-election-" + session.getSessionNum();
    }

    public long getEpoch() {
        return epochLeader.get();
    }

    public String semaphoreName() {
        return semaphoreName;
    }

    public void start(
            final String publishEndpoint,
            final GrpcReadStream.Observer<String> observerOnEndpointLeader
    ) {
        logger.info("Starting leader election session, semaphoreName = {}", semaphoreName);

        final CoordinationSession coordinationSession = coordinationClient.createSession();
        currentCoordinationSession.set(coordinationSession);

        coordinationSession.start(
                new Observer() {
                    @Override
                    public void onNext(SessionResponse sessionResponse) {
                        switch (sessionResponse.getResponseCase()) {
                            case ACQUIRE_SEMAPHORE_RESULT:
                                observerOnEndpointLeader.onNext(publishEndpoint);
                                break;
                            case ACQUIRE_SEMAPHORE_PENDING:
                            case DESCRIBE_SEMAPHORE_CHANGED:
                                coordinationSession.sendDescribeSemaphore(
                                        SessionRequest.DescribeSemaphore.newBuilder()
                                                .setName(semaphoreName)
                                                .setWatchOwners(true)
                                                .setIncludeOwners(true)
                                                .build()
                                );
                                break;
                            case DESCRIBE_SEMAPHORE_RESULT:
                                SemaphoreSession semaphoreSessionLeader = sessionResponse
                                        .getDescribeSemaphoreResult()
                                        .getSemaphoreDescription()
                                        .getOwnersList()
                                        .get(0);

                                epochLeader.set(semaphoreSessionLeader.getSessionId());

                                observerOnEndpointLeader.onNext(
                                        semaphoreSessionLeader.getData()
                                                .toString(StandardCharsets.UTF_8)
                                );
                                break;
                        }
                    }

                    @Override
                    public void onFailure(Long sessionId, StatusCodesProtos.StatusIds.StatusCode statusCode) {
                        logger.error("Coordination session closing, sessionId = {}", sessionId);

                        coordinationSession.stop();

                        if (isWorking.get()) {
                            logger.info(
                                    "Restarting connecting new coordination session... SemaphoreName: {}",
                                    semaphoreName
                            );

                            start(publishEndpoint, observerOnEndpointLeader);
                        }
                    }
                }
        ).whenComplete(
                (status, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed closing coordination session", throwable);
                    }

                    logger.info("Coordination session closing with status: {}", status);

                    if (isWorking.get()) {
                        start(publishEndpoint, observerOnEndpointLeader);
                    }
                }
        );

        coordinationSession.sendStartSession(
                SessionRequest.SessionStart.newBuilder()
                        .setSessionId(START_SESSION_ID)
                        .setPath(coordinationNodePath)
                        .setDescription(session.getDescription())
                        .setTimeoutMillis(SESSION_KEEP_ALIVE_TIMEOUT_MS)
                        .build()
        );

        coordinationSession.sendCreateSemaphore(
                SessionRequest.CreateSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setLimit(LIMIT_TOKENS_SEMAPHORE)
                        .build()
        );

        coordinationSession.sendAcquireSemaphore(
                SessionRequest.AcquireSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setCount(COUNT_TOKENS)
                        .setTimeoutMillis(-1)
                        .setData(ByteString.copyFrom(publishEndpoint.getBytes(StandardCharsets.UTF_8)))
                        .build()
        );
    }

    public void stop() {
        if (isWorking.compareAndSet(false, true)) {
            logger.info("Stopping leader election session...");

            while (true) {
                CoordinationSession coordinationSession = currentCoordinationSession.get();

                coordinationSession.stop();

                if (currentCoordinationSession.compareAndSet(coordinationSession, coordinationSession)) {
                    break;
                }
            }
        }
    }
}
