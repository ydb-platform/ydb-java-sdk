package tech.ydb.coordination.scenario;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.exceptions.CreateSessionException;
import tech.ydb.coordination.settings.CoordinationNodeSettings;

/**
 * @author Kirill Kurdyukov
 */
public abstract class ScenarioFactory {

    protected final CoordinationClient client;

    protected ScenarioFactory(CoordinationClient client) {
        this.client = client;
    }

    protected <T> CompletableFuture<T> createScenario(
            String coordinationNodeName,
            Function<String, T> converterStatusToSession
    ) {
        final String path;

        if (coordinationNodeName.startsWith(client.getDatabase())) {
            path = coordinationNodeName;
        } else {
            path = client.getDatabase() + "/" + coordinationNodeName;
        }

        return client.createNode(
                path,
                CoordinationNodeSettings.newBuilder().build()
        ).thenApply(
                status -> {
                    if (status.isSuccess()) {
                        return converterStatusToSession.apply(path);
                    } else {
                        throw new CreateSessionException(status);
                    }
                }
        );
    }
}
