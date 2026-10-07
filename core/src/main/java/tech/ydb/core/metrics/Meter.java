package tech.ydb.core.metrics;

import java.util.function.Consumer;

import io.grpc.ExperimentalApi;

/**
 * Entry point to create metric instruments. The interface is dependency-free, so the SDK core does
 * not require an OpenTelemetry runtime to compile or run. Implementations must be thread-safe.
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public interface Meter {
    Meter NOOP = new Meter() { };

    default LongCounter createCounter(String name, String unit, String description) {
        return LongCounter.NOOP;
    }

    default DoubleHistogram createHistogram(String name, String unit, String description) {
        return DoubleHistogram.NOOP;
    }

    /**
     * Creates a gauge callback without a registration handle.
     *
     * @deprecated Use {@link #registerLongGauge} to manage the callback lifecycle.
     */
    @Deprecated
    default void createLongGauge(String name, String unit, String description, Consumer<LongMeasurement> callback) {
        // noop: the backend never queries the callback
    }

    /**
     * Registers a gauge callback. Close the returned handle when its source is closed.
     * Backends implementing only {@link #createLongGauge} retain their existing lifecycle;
     * override this method to support unregistering callbacks.
     */
    default MetricRegistration registerLongGauge(
            String name, String unit, String description, Consumer<LongMeasurement> callback) {
        createLongGauge(name, unit, description, callback);
        return MetricRegistration.NOOP;
    }

    /**
     * Registers a floating-point gauge callback. Close the returned handle when its source is closed.
     */
    default MetricRegistration registerDoubleGauge(
            String name, String unit, String description, Consumer<DoubleMeasurement> callback) {
        return MetricRegistration.NOOP;
    }
}
