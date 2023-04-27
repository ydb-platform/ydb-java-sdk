package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.scenario.WorkingScenario;
import tech.ydb.coordination.settings.ScenarioSettings;

public class Utils {

    private Utils() {

    }

    public static <T extends WorkingScenario> CompletableFuture<T> getStart(ScenarioSettings.Builder<T> builder) {
        return builder
                .setCoordinationNodeName("test")
                .setSemaphoreName("test")
                .setDescription("Test scenario")
                .start();
    }
}
