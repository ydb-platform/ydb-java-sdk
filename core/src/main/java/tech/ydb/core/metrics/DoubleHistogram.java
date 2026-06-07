package tech.ydb.core.metrics;

import io.grpc.ExperimentalApi;

/**
 * Histogram of {@code double} values (typically used for durations in seconds).
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public interface DoubleHistogram {
    DoubleHistogram NOOP = (value, attrs) -> { };

    /**
     * Records the given value with optional attributes.
     *
     * @param value value to record
     * @param attrs measurement attributes
     */
    void record(double value, Attr... attrs);
}
