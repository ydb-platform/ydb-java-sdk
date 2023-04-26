package tech.ydb.coordination.scenario.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.SemaphoreDescription;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.observer.CoordinationSessionObserver;
import tech.ydb.coordination.scenario.WorkingScenario;
import tech.ydb.coordination.settings.ScenarioSettings;
import tech.ydb.core.Status;

/**
 * @author Kirill Kurdyukov
 */
public class ConfigurationSubscriber extends WorkingScenario {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationSubscriber.class);

    public ConfigurationSubscriber(
            CoordinationClient client,
            ScenarioSettings settings,
            String coordinationNodePath,
            ConfigurationSubscriberObserver subscriberObserver
    ) {
        super(client, settings, coordinationNodePath);

        start(
                new CoordinationSessionObserver() {
                    @Override
                    public void onSessionStarted() {
                        logger.info("Starting subscriber coordination session, sessionId: {}",
                                currentCoordinationSession.get().getSessionId());

                        currentCoordinationSession.get().sendCreateSemaphore(
                                SessionRequest.CreateSemaphore.newBuilder()
                                        .setName(settings.getSemaphoreName())
                                        .setLimit(ConfigurationPublisher.SEMAPHORE_LIMIT)
                                        .build()
                        );

                        describeSemaphore();
                    }

                    @Override
                    public void onDescribeSemaphoreResult(SemaphoreDescription semaphoreDescription, Status status) {
                        if (status.isSuccess()) {
                            subscriberObserver.onNext(
                                    semaphoreDescription.getData()
                                            .toByteArray()
                            );
                        } else {
                            logger.error(
                                    "Error describer result from configuration subscriber session, status: {}",
                                    status
                            );
                        }
                    }

                    @Override
                    public void onDescribeSemaphoreChanged(boolean dataChanged, boolean ownersChanged) {
                        if (dataChanged) {
                            describeSemaphore();
                        }
                    }

                    @Override
                    public void onFailure(Status status) {
                        logger.error("Failed from subscriber session: {}", status);
                    }
                }
        );
    }

    private void describeSemaphore() {
        currentCoordinationSession.get().sendDescribeSemaphore(
                SessionRequest.DescribeSemaphore.newBuilder()
                        .setName(settings.getSemaphoreName())
                        .setWatchData(true)
                        .build()
        );
    }
}
