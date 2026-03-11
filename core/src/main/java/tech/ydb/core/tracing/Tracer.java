package tech.ydb.core.tracing;

/**
 * Tracer is an entry point to create spans.
 *
 * <p>This is a minimal abstraction over tracing API (e.g. OpenTelemetry),
 * intentionally kept dependency-free for the core module.
 *
 * <p>Implementations must be thread-safe unless stated otherwise.
 */
public interface Tracer {

    /**
     * Creates a new {@link Span} for the given span name.
     *
     * @param spanName logical span name (for example, ydb.ExecuteQuery)
     * @param spanKind span kind that defines operation role
     * @return created span instance
     */
    Span startSpan(String spanName, SpanKind spanKind);

    /**
     * Returns the currently active span.
     *
     * @return active span or {@link Span#NOOP} when unavailable
     */
    default Span currentSpan() {
        return Span.NOOP;
    }
}
