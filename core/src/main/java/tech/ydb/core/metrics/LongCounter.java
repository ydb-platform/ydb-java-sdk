package tech.ydb.core.metrics;

import io.grpc.ExperimentalApi;

/**
 * A monotonic counter that records non-negative {@code long} deltas.
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public interface LongCounter {
    LongCounter NOOP = (value, attrs) -> { };

    /**
     * Adds the given value with optional attributes.
     *
     * @param value non-negative delta
     * @param attrs measurement attributes
     */
    void add(long value, Attr... attrs);
}
