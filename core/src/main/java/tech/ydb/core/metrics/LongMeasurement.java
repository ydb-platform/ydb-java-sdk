package tech.ydb.core.metrics;

import io.grpc.ExperimentalApi;

/**
 * Per-observation handle passed to {@link Meter#createLongGauge} callbacks.
 *
 * <p>A single callback invocation may call {@link #record} multiple times with different
 * attributes to emit several measurements per collection cycle.
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public interface LongMeasurement {
    /**
     * Records the current value of the gauge for the given attribute set.
     *
     * @param value observed value
     * @param attrs measurement attributes
     */
    void record(long value, Attr... attrs);
}
