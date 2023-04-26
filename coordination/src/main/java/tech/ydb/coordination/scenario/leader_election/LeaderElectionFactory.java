package tech.ydb.coordination.scenario.leader_election;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.scenario.ScenarioFactory;
import tech.ydb.coordination.settings.ScenarioSettings;

/**
 * @author Kirill Kurdyukov
 */
public class LeaderElectionFactory extends ScenarioFactory {

    public LeaderElectionFactory(CoordinationClient client) {
        super(client);
    }

    public CompletableFuture<LeaderElection> leaderElection(
            ScenarioSettings settings,
            String ticket,
            LeaderElectionObserver observer
    ) {
        return createScenario(
                settings.getCoordinationNodeName(),
                path -> new LeaderElection(client, settings, path, ticket, observer)
        );
    }
}
