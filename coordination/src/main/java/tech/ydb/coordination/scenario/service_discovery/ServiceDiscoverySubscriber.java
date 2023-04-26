package tech.ydb.coordination.scenario.service_discovery;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

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
public class ServiceDiscoverySubscriber extends WorkingScenario {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoverySubscriber.class);

    public ServiceDiscoverySubscriber(
            CoordinationClient client,
            ScenarioSettings settings,
            String coordinationNodePath,
            ServiceDiscoveryObserver observer
    ) {
        super(client, settings, coordinationNodePath);

        start(
                new CoordinationSessionObserver() {
                    @Override
                    public void onSessionStarted() {
                        logger.info("Starting service discovery subscriber session, sessionId: {}",
                                currentCoordinationSession.get().getSessionId());

                        currentCoordinationSession.get().sendCreateSemaphore(
                                SessionRequest.CreateSemaphore.newBuilder()
                                        .setName(settings.getSemaphoreName())
                                        .setLimit(Long.MAX_VALUE)
                                        .build()
                        );

                        describeSemaphore();
                    }

                    @Override
                    public void onDescribeSemaphoreResult(SemaphoreDescription semaphoreDescription, Status status) {
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
                            describeSemaphore();
                        }
                    }
                }
        );
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
}
