package tech.ydb.coordination.scenario.configuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.scenario.WorkingScenario;
import tech.ydb.coordination.settings.ScenarioSettings;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 * @author Kirill Kurdyukov
 */
public class ConfigurationPublisher extends WorkingScenario {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationPublisher.class);

    /**
     * Semaphores must have limit more than zero
     */
    public static final int SEMAPHORE_LIMIT = 1;

    private final ConcurrentHashMap<Long, CompletableFuture<Status>> reqIdToStatus = new ConcurrentHashMap<>();

    private ConfigurationPublisher(CoordinationClient client, ScenarioSettings settings) {
        super(client, settings, SEMAPHORE_LIMIT);
    }

    public static Builder newBuilder(CoordinationClient client) {
        return new Builder(client);
    }

    public CompletableFuture<Status> publishData(byte[] bytes) {
        long reqId = ThreadLocalRandom.current().nextLong();
        CompletableFuture<Status> resultFuture = new CompletableFuture<>();

        reqIdToStatus.put(reqId, resultFuture);

        currentCoordinationSession.get().sendUpdateSemaphore(
                SessionRequest.UpdateSemaphore.newBuilder()
                        .setReqId(reqId)
                        .setName(settings.getSemaphoreName())
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
        reqIdToStatus.clear();
    }

    public static class Builder extends ScenarioSettings.Builder<ConfigurationPublisher> {
        public Builder(CoordinationClient client) {
            super(client);
        }

        @Override
        protected ConfigurationPublisher buildScenario(ScenarioSettings settings) {
            ConfigurationPublisher publisher = new ConfigurationPublisher(client, settings);

            publisher.start(
                    new CoordinationSession.Observer() {
                        @Override
                        public void onUpdateSemaphoreResult(long reqId, Status status) {
                            CompletableFuture<Status> statusCompletableFuture = publisher
                                    .reqIdToStatus.remove(reqId);

                            if (statusCompletableFuture != null) {
                                statusCompletableFuture.complete(status);
                            }
                        }

                        @Override
                        public void onSessionStarted() {
                            logger.info("Starting coordination publisher session, sessionId: {}",
                                    publisher.currentCoordinationSession.get().getSessionId());
                        }

                        @Override
                        public void onFailure(Status status) {
                            logger.error("Fail from publisher session: {}", status);
                        }
                    }
            );

            return publisher;
        }
    }
}
