package tech.ydb.core.tracing;

/**
 * Closeable scope returned by {@link Span#makeCurrent()}.
 */
public interface SpanScope extends AutoCloseable {
    @Override
    void close();
}
