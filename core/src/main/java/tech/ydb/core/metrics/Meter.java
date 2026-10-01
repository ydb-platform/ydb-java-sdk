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
     * Registers a gauge callback. The caller must close the registration when its source is closed.
     */
    default MetricRegistration createLongGauge(
            String name, String unit, String description, Consumer<LongMeasurement> callback) {
        return MetricRegistration.NOOP;
    }
}
