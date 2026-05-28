package tech.ydb.core.metrics;

import io.grpc.ExperimentalApi;

/**
 * A monotonic counter that records non-negative {@code long} deltas.
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public interface LongCounter {
    LongCounter NOOP = (value, keyValues) -> { };

    /**
     * Adds the given value with optional pairs of attribute key/value.
     *
     * @param value     non-negative delta
     * @param keyValues alternating {@code key, value} pairs; must have even length
     */
    void add(long value, String... keyValues);
}
