package tech.ydb.core.tracing;

import javax.annotation.Nullable;

import io.grpc.ExperimentalApi;

import tech.ydb.core.Status;

/**
 * A span represents a timed operation.
 */
@ExperimentalApi("YDB Tracer is experimental and API may change without notice")
public interface Span {
    Span NOOP = new Span() {
    };

    /**
     * Returns W3C traceparent value for request propagation.
     *
     * <p>For {@link #NOOP} this returns an empty string. Check {@link #isValid()} to decide whether
     * trace headers should be sent to server.
     *
     * @return traceparent value
     */
    default String getId() {
        return "";
    }

    /**
     * Indicates whether this span carries a real tracing context.
     *
     * @return true for real spans, false for noop span
     */
    default boolean isValid() {
        return false;
    }

    /**
     * Sets a string attribute on the span.
     *
     * @param key attribute key
     * @param value attribute value, may be null
     */
    default void setAttribute(String key, @Nullable String value) {
    }

    /**
     * Sets a long attribute on the span.
     *
     * @param key attribute key
     * @param value attribute value
     */
    default void setAttribute(String key, long value) {
    }

    /**
     * Sets span status (success or error) with human-readable message.
     *
     * @param status operation status used to map span attributes
     * @param error operation exception used to map span attributes
     */
    default void setStatus(@Nullable Status status, @Nullable Throwable error) {
    }

    /**
     * Makes this span current in the active execution context.
     *
     * @return closeable scope handle
     */
    default Scope makeCurrent() {
        return () -> {
        };
    }

    /**
     * Restores context captured when this span was created.
     *
     * <p>The returned scope must be closed, usually via try-with-resources.
     *
     * @return closeable scope handle for restored context
     */
    default Scope restoreContext() {
        return () -> {
        };
    }

    /**
     * Ends (finishes) this span.
     */
    default void end() {
    }
}
