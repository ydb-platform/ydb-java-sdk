package tech.ydb.core.tracing;

/**
 * A span represents a timed operation.
 */
public interface Span {


    String getId();

    /** Sets a string attribute on the span (ignored by Noop implementation). */
    void setAttribute(String key, String value);

    /** Sets a long attribute on the span (ignored by Noop implementation). */
    void setAttribute(String key, long value);

    /**
     * Records an exception on the span.
     *
     * <p>Implementation is responsible for mapping this to:
     * <ul>
     *   <li>exception event (OTel),</li>
     *   <li>and (optionally) setting span status to ERROR internally.</li>
     * </ul>
     *
     * <p>Core does not require status API: status can be set inside implementation.
     */
    Span recordException(Throwable error);

    void end();
}
