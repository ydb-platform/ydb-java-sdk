package tech.ydb.core.metrics;

import io.grpc.ExperimentalApi;

/**
 * Per-observation handle passed to {@link Meter#registerDoubleGauge} callbacks.
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public interface DoubleMeasurement {
    /**
     * Records the current value of the gauge for the given attribute set.
     *
     * @param value observed value
     * @param attrs measurement attributes
     */
    void record(double value, Attr... attrs);
}
