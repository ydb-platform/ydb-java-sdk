package tech.ydb.core.tracing;

import tech.ydb.core.Status;

/**
 * A span represents a timed operation.
 */
public interface Span {

    String getId();

    /**
     * Sets a string attribute on the span (ignored by Noop implementation).
     */
    void setAttribute(String key, String value);

    /**
     * Sets a long attribute on the span (ignored by Noop implementation).
     */
    void setAttribute(String key, long value);

    /**
     * Sets span status to error with human-readable message.
     */
    void setError(Status status);

    /**
     * Sets span status to error from exception.
     */
    void setError(Throwable error);

    void end();
}
