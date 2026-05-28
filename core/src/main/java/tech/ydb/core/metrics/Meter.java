package tech.ydb.core.metrics;

import java.util.function.Consumer;

import io.grpc.ExperimentalApi;

/**
 * Entry point to create metric instruments (counters, histograms, gauges).
 *
 * <p>The interface is dependency-free, so the SDK core does not require an OpenTelemetry runtime
 * to compile or run. Each call site (session pool, RPC service, etc.) is expected to create and
 * own its private instruments via this {@code Meter} in its constructor and call them on the
 * hot path.
 *
 * <p>Implementations must be thread-safe.
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public interface Meter {
    Meter NOOP = new Meter() { };

    /**
     * Creates a monotonic {@code long} counter.
     *
     * @param name metric name
     * @param unit metric unit (for example, {@code {operation}})
     * @param description human-readable metric description
     * @return created counter
     */
    default LongCounter createCounter(String name, String unit, String description) {
        return LongCounter.NOOP;
    }

    /**
     * Creates a {@code double} histogram.
     *
     * @param name metric name
     * @param unit metric unit (for example, {@code s})
     * @param description human-readable metric description
     * @return created histogram
     */
    default DoubleHistogram createHistogram(String name, String unit, String description) {
        return DoubleHistogram.NOOP;
    }

    /**
     * Registers an asynchronous {@code long} gauge. The {@code callback} is invoked by the metrics
     * backend on every collection cycle; the supplied {@link LongMeasurement} may emit any number
     * of measurements with different attributes.
     *
     * @param name metric name
     * @param unit metric unit (for example, {@code {session}})
     * @param description human-readable metric description
     * @param callback callback invoked by the metrics backend to collect gauge values
     */
    default void createLongGauge(String name, String unit, String description, Consumer<LongMeasurement> callback) {
        // noop: the backend never queries the callback
    }
}
