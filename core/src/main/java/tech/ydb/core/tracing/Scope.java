package tech.ydb.core.tracing;

/**
 * Lightweight context scope for active span propagation.
 */
public interface Scope extends AutoCloseable {
    @Override
    void close();
}
