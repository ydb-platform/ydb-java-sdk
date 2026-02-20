package tech.ydb.observability.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import tech.ydb.core.Status;
import tech.ydb.observability.TracingSpan;
import tech.ydb.observability.YdbTracing;
import tech.ydb.table.query.Params;

public final class NoopYdbTracing implements YdbTracing {
    public static final NoopYdbTracing INSTANCE = new NoopYdbTracing();
    private static final NoopTracingSpan NOOP_SPAN = new NoopTracingSpan();

    private NoopYdbTracing() {
        // No operations.
    }

    @Override
    public TracingSpan createSpan(String operationName, String query, Params params) {
        return NOOP_SPAN;
    }

    @Override
    public <T> T trace(String operationName, String query, Params params, Supplier<T> operation) {
        return operation.get();
    }

    @Override
    public <T> CompletableFuture<T> traceAsync(String operationName,
           String query, Params params, Supplier<CompletableFuture<T>> operation) {
        return operation.get();
    }

    private static final class NoopTracingSpan implements TracingSpan {
        @Override
        public TracingSpan setAttribute(String key, String value) {
            return this;
        }

        @Override
        public TracingSpan setAttribute(String key, long value) {
            return this;
        }

        @Override
        public TracingSpan setAttribute(String key, boolean value) {
            return this;
        }

        @Override
        public TracingSpan setStatus(Status status) {
            return this;
        }

        @Override
        public TracingSpan recordException(Throwable error) {
            return this;
        }

        @Override
        public void end() {
            // todo...
        }
    }
}
