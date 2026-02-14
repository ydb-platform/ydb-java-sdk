package tech.ydb.observability;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import tech.ydb.table.query.Params;

public interface YdbTracing {
    TracingSpan createSpan(String operationName, String query, Params params);

    <T> T trace(String operationName, String query, Params params, Supplier<T> operation);

    <T> CompletableFuture<T> traceAsync(String operationName, String query, Params params,
                                        Supplier<CompletableFuture<T>> operation);

    static YdbTracing global() {
        return GlobalYdbTracing.INSTANCE;
    }

    static void setGlobal(YdbTracing tracing) {
        GlobalYdbTracing.INSTANCE = tracing;
    }
}
