package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.scenario.WorkingScenario;

public class Utils {

    private Utils() {

    }

    public static <T extends WorkingScenario> CompletableFuture<T> getStart(
            WorkingScenario.Builder<T> builder,
            String semaphoreName
    ) {
        return builder
                .setCoordinationNodeName("test")
                .setSemaphoreName(semaphoreName)
                .setDescription("Test scenario")
                .start();
    }
}
