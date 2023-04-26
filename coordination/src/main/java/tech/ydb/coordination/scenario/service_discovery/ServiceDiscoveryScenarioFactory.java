package tech.ydb.coordination.scenario.service_discovery;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.scenario.ScenarioFactory;
import tech.ydb.coordination.settings.ScenarioSettings;

/**
 * @author Kirill Kurdyukov
 */
public class ServiceDiscoveryScenarioFactory extends ScenarioFactory {

    public ServiceDiscoveryScenarioFactory(CoordinationClient client) {
        super(client);
    }

    public CompletableFuture<ServiceDiscoveryPublisher> serviceDiscoveryPublisher(
            ScenarioSettings settings,
            String endpoint
    ) {
        return createScenario(
                settings.getCoordinationNodeName(),
                path -> new ServiceDiscoveryPublisher(client, settings, path, endpoint)
        );
    }

    public CompletableFuture<ServiceDiscoverySubscriber> serviceDiscoverySubscriber(
            ScenarioSettings settings,
            ServiceDiscoveryObserver observer
    ) {
        return createScenario(
                settings.getCoordinationNodeName(),
                path -> new ServiceDiscoverySubscriber(client, settings, path, observer)
        );
    }
}
