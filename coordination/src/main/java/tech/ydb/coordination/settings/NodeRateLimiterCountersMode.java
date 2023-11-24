package tech.ydb.coordination.settings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public enum NodeRateLimiterCountersMode {
    /** The default or current value */
    UNSET,
    /** Aggregated counters for resource tree */
    AGGREGATED,
    /** Counters on every resource */
    DETAILED
}
