package tech.ydb.core.metrics;

import io.grpc.ExperimentalApi;

/**
 * Registration of an observable metric callback. Close it when its source is no longer used.
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public interface MetricRegistration extends AutoCloseable {
    MetricRegistration NOOP = () -> { };

    /**
     * Unregisters the callback. Repeated calls have no effect.
     */
    @Override
    void close();
}
