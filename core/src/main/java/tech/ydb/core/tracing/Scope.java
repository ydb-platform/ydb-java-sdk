package tech.ydb.core.tracing;

/**
 * Closeable scope returned by context-switching span operations.
 *
 * <p>Use with try-with-resources to avoid leaking context across async callbacks.
 */
public interface Scope extends AutoCloseable {

    @Override
    void close();
}
