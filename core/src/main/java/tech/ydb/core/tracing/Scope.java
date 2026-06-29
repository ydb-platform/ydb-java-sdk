package tech.ydb.core.tracing;

import io.grpc.ExperimentalApi;

/**
 * Closeable scope returned by context-switching span operations.
 *
 * <p>Use with try-with-resources to avoid leaking context across async callbacks.
 */
@ExperimentalApi("YDB Tracer is experimental and API may change without notice")
public interface Scope extends AutoCloseable {

    @Override
    void close();
}
