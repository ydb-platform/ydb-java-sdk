package tech.ydb.coordination.session;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.StatusCodesProtos;
import tech.ydb.coordination.CoordinationClient;
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

    public void start(
            final String publishEndpoint,
            final GrpcReadStream.Observer<String> observerOnEndpointLeader
    ) {
        logger.info("Starting leader election session, semaphoreName = {}", semaphoreName);

        final CoordinationSession coordinationSession = coordinationClient.createSession();

        coordinationSession.start(
                new Observer() {
                    @Override
                    public void onNext(SessionResponse sessionResponse) {
                        if (sessionResponse.hasDescribeSemaphoreChanged()) {
                            String endpointLeader = sessionResponse.getDescribeSemaphoreResult()
                                    .getSemaphoreDescription()
                                    .getOwnersList()
                                    .get(0)
                                    .getData()
                                    .toString(StandardCharsets.UTF_8);

                            observerOnEndpointLeader.onNext(endpointLeader);
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
                        .setData(ByteString.copyFrom(publishEndpoint.getBytes(StandardCharsets.UTF_8)))
                        .setTimeoutMillis(0) // Try acquire
                        .build()
        );

        coordinationSession.sendDescribeSemaphore(
                SessionRequest.DescribeSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setWatchOwners(true)
                        .setIncludeOwners(true)
                        .build()
        );
    }

    public void stop() {
        if (isWorking.compareAndSet(false, true)) {
            logger.info("Stopping leader election session...");
        }
    }
}
