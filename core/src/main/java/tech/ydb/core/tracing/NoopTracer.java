package tech.ydb.core.tracing;

/**
 * Noop tracer used when tracing is disabled.
 *
 * <p>Designed to have near-zero overhead:
 * singletons, no allocations per call (except caller code).
 */
public final class NoopTracer implements Tracer {
    public static final NoopTracer INSTANCE = new NoopTracer();

    private NoopTracer() {
    }

    public static NoopTracer getInstance() {
        return INSTANCE;
    }

    @Override
    public Span startSpan(String spanName, SpanKind spanKind) {
        return null;
    }
}
