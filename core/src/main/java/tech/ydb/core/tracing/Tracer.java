package tech.ydb.core.tracing;

import javax.annotation.Nullable;

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
     */
    @Nullable
    Span startSpan(String spanName, SpanKind spanKind);
}
