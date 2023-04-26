package tech.ydb.coordination.scenario.configuration;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.scenario.ScenarioFactory;
import tech.ydb.coordination.settings.ScenarioSettings;

/**
 * @author Kirill Kurdyukov
 */
public class ConfigurationScenarioFactory extends ScenarioFactory  {

    public ConfigurationScenarioFactory(CoordinationClient client) {
        super(client);
    }

    public CompletableFuture<ConfigurationPublisher> configurationPublisher(
            ScenarioSettings settings
    ) {
        return createScenario(
                settings.getCoordinationNodeName(),
                path -> new ConfigurationPublisher(client, settings, path)
        );
    }

    public CompletableFuture<ConfigurationSubscriber> configurationSubscriber(
            ScenarioSettings settings,
            ConfigurationSubscriberObserver observer
    ) {
        return createScenario(
                settings.getCoordinationNodeName(),
                path -> new ConfigurationSubscriber(client, settings, path, observer)
        );
    }
}
