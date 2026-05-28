package tech.ydb.core.metrics;

import io.grpc.ExperimentalApi;

/**
 * Histogram of {@code double} values (typically used for durations in seconds).
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public interface DoubleHistogram {
    DoubleHistogram NOOP = (value, keyValues) -> { };

    /**
     * Records the given value with optional pairs of attribute key/value.
     *
     * @param value     value to record
     * @param keyValues alternating {@code key, value} pairs; must have even length
     */
    void record(double value, String... keyValues);
}
