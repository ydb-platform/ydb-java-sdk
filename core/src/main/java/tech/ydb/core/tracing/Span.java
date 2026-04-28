package tech.ydb.core.tracing;

import javax.annotation.Nullable;

import tech.ydb.core.Status;

/**
 * A span represents a timed operation.
 */
public interface Span {

    String getId();

    /**
     * Sets a string attribute on the span (ignored by Noop implementation).
     *
     * @param key attribute key
     * @param value attribute value, may be null
     */
    void setAttribute(String key, @Nullable String value);

    /**
     * Sets a long attribute on the span (ignored by Noop implementation).
     *
     * @param key attribute key
     * @param value attribute value
     */
    void setAttribute(String key, long value);

    /**
     * Sets span status to error with human-readable message.
     *
     * @param status operation status used to map error attributes
     */
    void setError(Status status);

    /**
     * Sets span status to error from exception.
     *
     * @param error exception used to map error attributes
     */
    void setError(Throwable error);

    void end();
}
