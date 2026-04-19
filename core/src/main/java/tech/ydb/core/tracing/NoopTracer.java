package tech.ydb.core.tracing;

import io.grpc.ExperimentalApi;

/**
 * Noop tracer used when tracing is disabled.
 *
 * <p>Designed to have near-zero overhead:
 * singletons, no allocations per call (except caller code).
 */
@ExperimentalApi("YDB Tracer is experimental and API may change without notice")
public final class NoopTracer implements Tracer {
    private static final NoopTracer INSTANCE = new NoopTracer();

    private NoopTracer() {
    }

    public static NoopTracer getInstance() {
        return INSTANCE;
    }

    @Override
    public Span startSpan(String spanName, SpanKind spanKind) {
        return Span.NOOP;
    }
}
