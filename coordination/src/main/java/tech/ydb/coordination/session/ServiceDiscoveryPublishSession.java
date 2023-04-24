package tech.ydb.coordination.session;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.StatusCodesProtos;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.SessionResponse;
import tech.ydb.coordination.observer.Observer;
import tech.ydb.coordination.settings.SessionSettings;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 * @author Kirill Kurdyukov
 */
public class ServiceDiscoveryPublishSession {

    public static final String SERVICE_DISCOVERY_SEMAPHORE_PREFIX = "service-discovery-";

    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryPublishSession.class);

    private final CoordinationClient coordinationClient;
    private final SessionSettings settings;
    private final String coordinationNodeName;
    private final String semaphoreName;
    private final AtomicReference<CoordinationSession> currentCoordinationSession = new AtomicReference<>();
    private final AtomicBoolean isWorking = new AtomicBoolean(true);


    public ServiceDiscoveryPublishSession(
            CoordinationClient coordinationClient,
            SessionSettings settings,
            String coordinationNodeName
    ) {
        this.coordinationClient = coordinationClient;
        this.settings = settings;
        this.coordinationNodeName = coordinationNodeName;
        this.semaphoreName = SERVICE_DISCOVERY_SEMAPHORE_PREFIX + settings.getSessionNum();
    }

    public CompletableFuture<Status> publish(String endpoint) {
        if (isWorking.get()) {
            return CompletableFuture.completedFuture(Status.of(StatusCode.CANCELLED));
        }

        logger.info("Starting publish session, semaphoreName = {}", semaphoreName);

        CoordinationSession prevCoordinationSession = currentCoordinationSession.get();
        if (prevCoordinationSession != null) {
            prevCoordinationSession.stop();
        }

        final CoordinationSession coordinationSession = coordinationClient.createSession();
        currentCoordinationSession.set(coordinationSession);

        if (!isWorking.get()) {
            currentCoordinationSession.get().stop();
            return CompletableFuture.completedFuture(Status.of(StatusCode.NOT_FOUND));
        }

        CompletableFuture<Status> publishResultFuture = new CompletableFuture<>();

        coordinationSession.start(
                new Observer() {
                    @Override
                    public void onNext(SessionResponse sessionResponse) {
                        if (sessionResponse.hasAcquireSemaphoreResult()) {
                            logger.info("Endpoint publish: {}!", endpoint);

                            publishResultFuture.complete(
                                    Status.of(
                                            StatusCode.fromProto(
                                                    sessionResponse
                                                            .getAcquireSemaphoreResult()
                                                            .getStatus()
                                            )
                                    )
                            );
                        }
                    }

                    @Override
                    public void onFailure(StatusCodesProtos.StatusIds.StatusCode statusCode) {
                        logger.error("Failed publish session with status: {}", statusCode);

                        if (isWorking.get()) {
                            logger.info("Restarting publish session ...");
                            publish(endpoint);
                        }
                    }
                }
        ).whenComplete(
                (status, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed publish session", throwable);
                    }

                    logger.info("Stopping publish session with status: {}", status);

                    if (isWorking.get()) {
                        logger.info("Restarting publish session ...");
                        publish(endpoint);
                    }
                }
        );

        coordinationSession.sendStartSession(
                SessionRequest.SessionStart.newBuilder()
                        .setPath(coordinationNodeName)
                        .setSessionId(SessionSettings.START_SESSION_ID)
                        .setDescription(settings.getDescription())
                        .build()
        );

        coordinationSession.sendCreateSemaphore(
                SessionRequest.CreateSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setLimit(Long.MAX_VALUE)
                        .build()
        );

        coordinationSession.sendAcquireSemaphore(
                SessionRequest.AcquireSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setCount(1)
                        .setData(ByteString.copyFrom(endpoint.getBytes(StandardCharsets.UTF_8)))
                        .build()
        );

        return publishResultFuture;
    }

    @PreDestroy
    public synchronized void stop() {
        if (isWorking.compareAndSet(true, false)) {
            while (true) {
                CoordinationSession coordinationSession = currentCoordinationSession.get();

                if (coordinationSession != null) {
                    coordinationSession.stop();
                }

                if (currentCoordinationSession.compareAndSet(coordinationSession, coordinationSession)) {
                    break;
                }
            }
        }
    }
}
