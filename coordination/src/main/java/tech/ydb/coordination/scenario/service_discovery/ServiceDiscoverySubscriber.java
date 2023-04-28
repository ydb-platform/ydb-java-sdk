package tech.ydb.coordination.scenario.service_discovery;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreDescription;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.scenario.WorkingScenario;
import tech.ydb.coordination.settings.ScenarioSettings;
import tech.ydb.core.Status;

/**
 * @author Kirill Kurdyukov
 */
public class ServiceDiscoverySubscriber extends WorkingScenario {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoverySubscriber.class);

    private ServiceDiscoverySubscriber(CoordinationClient client, ScenarioSettings settings) {
        super(client, settings, Long.MAX_VALUE);
    }

    public static Builder newBuilder(CoordinationClient client, Observer observer) {
        return new Builder(client, observer);
    }

    private void describeSemaphore() {
        currentCoordinationSession.get().sendDescribeSemaphore(
                SessionRequest.DescribeSemaphore.newBuilder()
                        .setName(settings.getSemaphoreName())
                        .setIncludeOwners(true)
                        .setWatchOwners(true)
                        .build()
        );
    }

    public interface Observer {

        void onNext(List<String> endpoints);
    }

    public static class Builder extends ScenarioSettings.Builder<ServiceDiscoverySubscriber> {

        private final Observer observer;

        public Builder(CoordinationClient client, Observer observer) {
            super(client);

            this.observer = observer;
        }

        @Override
        protected ServiceDiscoverySubscriber buildScenario(ScenarioSettings settings) {
            ServiceDiscoverySubscriber subscriber = new ServiceDiscoverySubscriber(client, settings);

            subscriber.start(
                    new CoordinationSession.Observer() {
                        @Override
                        public void onSessionStarted() {
                            logger.info("Starting service discovery subscriber session, sessionId: {}",
                                    subscriber.currentCoordinationSession.get().getSessionId());

                            subscriber.describeSemaphore();
                        }

                        @Override
                        public void onDescribeSemaphoreResult(SemaphoreDescription semaphoreDescription,
                                                              Status status) {
                            if (status.isSuccess()) {
                                observer.onNext(
                                        semaphoreDescription.getOwnersList()
                                                .stream()
                                                .map(semaphoreSession -> semaphoreSession
                                                        .getData()
                                                        .toString(StandardCharsets.UTF_8))
                                                .collect(Collectors.toList())
                                );
                            } else {
                                logger.error(
                                        "Error describer result from service discovery subscriber session, status: {}",
                                        status
                                );
                            }
                        }

                        @Override
                        public void onDescribeSemaphoreChanged(boolean dataChanged, boolean ownersChanged) {
                            if (ownersChanged) {
                                subscriber.describeSemaphore();
                            }
                        }
                    }
            );

            return subscriber;
        }
    }
}
