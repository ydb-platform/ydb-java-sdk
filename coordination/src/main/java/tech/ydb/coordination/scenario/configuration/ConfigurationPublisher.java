package tech.ydb.coordination.scenario.configuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.observer.CoordinationSessionObserver;
import tech.ydb.coordination.scenario.WorkingScenario;
import tech.ydb.coordination.settings.ScenarioSettings;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 * @author Kirill Kurdyukov
 */
public class ConfigurationPublisher extends WorkingScenario {

    /**
     * Semaphores must have limit more than zero
     */
    public static final int SEMAPHORE_LIMIT = 1;

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationPublisher.class);
    private final String semaphoreName;
    private final ConcurrentHashMap<Long, CompletableFuture<Status>> reqIdToStatus = new ConcurrentHashMap<>();

    public ConfigurationPublisher(
            CoordinationClient client,
            ScenarioSettings settings,
            String coordinationNodePath
    ) {
        super(client, settings, coordinationNodePath);

        start(
                new CoordinationSessionObserver() {
                    @Override
                    public void onUpdateSemaphoreResult(long reqId, Status status) {
                        CompletableFuture<Status> statusCompletableFuture = reqIdToStatus.remove(reqId);

                        if (statusCompletableFuture != null) {
                            statusCompletableFuture.complete(status);
                        }
                    }

                    @Override
                    public void onSessionStarted() {
                        logger.info("Starting coordination publisher session, sessionId: {}",
                                currentCoordinationSession.get().getSessionId());

                        currentCoordinationSession.get().sendCreateSemaphore(
                                SessionRequest.CreateSemaphore.newBuilder()
                                        .setName(settings.getSemaphoreName())
                                        .setLimit(SEMAPHORE_LIMIT)
                                        .build()
                        );
                    }

                    @Override
                    public void onFailure(Status status) {
                        logger.error("Fail from publisher session: {}", status);
                    }
                }
        );

        this.semaphoreName = settings.getSemaphoreName();
    }

    public CompletableFuture<Status> publishData(byte[] bytes) {
        long reqId = ThreadLocalRandom.current().nextLong();
        CompletableFuture<Status> resultFuture = new CompletableFuture<>();

        reqIdToStatus.put(reqId, resultFuture);

        currentCoordinationSession.get().sendUpdateSemaphore(
                SessionRequest.UpdateSemaphore.newBuilder()
                        .setReqId(reqId)
                        .setName(semaphoreName)
                        .setData(ByteString.copyFrom(bytes))
                        .build()
        );

        return resultFuture;
    }

    @Override
    public void stop() {
        super.stop();

        for (CompletableFuture<Status> futures : reqIdToStatus.values()) {
            futures.complete(Status.of(StatusCode.CANCELLED));
        }
    }
}
