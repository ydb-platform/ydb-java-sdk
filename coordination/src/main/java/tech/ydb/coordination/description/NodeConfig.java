package tech.ydb.coordination.description;

import java.time.Duration;

import com.google.common.base.Preconditions;

import tech.ydb.proto.coordination.Config;
import tech.ydb.proto.coordination.DescribeNodeResult;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class NodeConfig {
    public enum ConsistencyMode {
        /** The default or current value */
        UNSET,
        /** Strict mode makes sure operations may only complete on current leader */
        STRICT,
        /** Relaxed mode allows operations to complete on stale masters */
        RELAXED
    }

    public enum RateLimiterCountersMode {
        /** The default or current value */
        UNSET,
        /** Aggregated counters for resource tree */
        AGGREGATED,
        /** Counters on every resource */
        DETAILED
    }

    private final Duration selfCheckPeriod;
    private final Duration sessionGracePeriod;

    private final ConsistencyMode readConsistencyMode;
    private final ConsistencyMode attachConsistencyMode;

    private final RateLimiterCountersMode rateLimiterCountersMode;

    private NodeConfig() {
        selfCheckPeriod = Duration.ofSeconds(1);
        sessionGracePeriod = Duration.ofSeconds(10);
        readConsistencyMode = ConsistencyMode.UNSET;
        attachConsistencyMode = ConsistencyMode.UNSET;
        rateLimiterCountersMode = RateLimiterCountersMode.UNSET;
    }

    private NodeConfig(DescribeNodeResult result) {
        Preconditions.checkNotNull(result, "DescriptionNodeResult must be not null");
        Preconditions.checkNotNull(result.getConfig(), "DescriptionNodeResult config must be not null");
        selfCheckPeriod = Duration.ofMillis(result.getConfig().getSelfCheckPeriodMillis());
        sessionGracePeriod = Duration.ofMillis(result.getConfig().getSessionGracePeriodMillis());
        readConsistencyMode = fromProto(result.getConfig().getReadConsistencyMode());
        attachConsistencyMode = fromProto(result.getConfig().getAttachConsistencyMode());
        rateLimiterCountersMode = fromProto(result.getConfig().getRateLimiterCountersMode());
    }

    private NodeConfig(Duration selfCheck, Duration sessionGrace, ConsistencyMode read, ConsistencyMode attach,
            RateLimiterCountersMode rateLimiter) {
        Preconditions.checkArgument(!selfCheck.isNegative() && !selfCheck.isZero(),
                "SelfCheckPeriod must be strictly greater than zero"
        );
        Preconditions.checkArgument(!sessionGrace.isNegative() && !sessionGrace.isZero(),
                "SessionGracePeriod must be strictly greater than zero"
        );
        Preconditions.checkArgument(sessionGrace.compareTo(selfCheck) > 0,
                "SessionGracePeriod must be strictly more than SelfCheckPeriod"
        );
        selfCheckPeriod = selfCheck;
        sessionGracePeriod = sessionGrace;
        readConsistencyMode = read;
        attachConsistencyMode = attach;
        rateLimiterCountersMode = rateLimiter;
    }

    /** @return Period for self-checks (default 1 second) */
    public Duration getSelfCheckPeriod() {
        return selfCheckPeriod;
    }

    /** @return Grace period for sessions on leader change (default 10 seconds) */
    public Duration getSessionGracePeriod() {
        return sessionGracePeriod;
    }

    /** @return Consistency mode for read operations */
    public ConsistencyMode getReadConsistencyMode() {
        return readConsistencyMode;
    }

    /** @return Consistency mode for attach operations */
    public ConsistencyMode getAttachConsistencyMode() {
        return attachConsistencyMode;
    }

    /** @return Rate limiter counters mode */
    public RateLimiterCountersMode getRateLimiterCountersMode() {
        return rateLimiterCountersMode;
    }

    public NodeConfig withDurationsConfig(Duration selfCheck, Duration sessionGrace) {
        return new NodeConfig(
                selfCheck, sessionGrace, readConsistencyMode, attachConsistencyMode, rateLimiterCountersMode
        );
    }

    public NodeConfig withReadConsistencyMode(ConsistencyMode mode) {
        return new NodeConfig(
                selfCheckPeriod, sessionGracePeriod, mode, attachConsistencyMode, rateLimiterCountersMode
        );
    }

    public NodeConfig withAttachConsistencyMode(ConsistencyMode mode) {
        return new NodeConfig(
                selfCheckPeriod, sessionGracePeriod, readConsistencyMode, mode, rateLimiterCountersMode
        );
    }

    public NodeConfig withRateLimiterCountersMode(RateLimiterCountersMode mode) {
        return new NodeConfig(
                selfCheckPeriod, sessionGracePeriod, readConsistencyMode, attachConsistencyMode, mode
        );
    }

    public Config toProto() {
        return Config.newBuilder()
                .setSelfCheckPeriodMillis((int) selfCheckPeriod.toMillis())
                .setSessionGracePeriodMillis((int) sessionGracePeriod.toMillis())
                .setReadConsistencyMode(toProto(readConsistencyMode))
                .setAttachConsistencyMode(toProto(attachConsistencyMode))
                .setRateLimiterCountersMode(toProto(rateLimiterCountersMode))
                .build();
    }

    public static NodeConfig create() {
        return new NodeConfig();
    }

    public static NodeConfig fromProto(DescribeNodeResult result) {
        return new NodeConfig(result);
    }

    private static tech.ydb.proto.coordination.ConsistencyMode toProto(ConsistencyMode mode) {
        switch (mode) {
            case STRICT:
                return tech.ydb.proto.coordination.ConsistencyMode.CONSISTENCY_MODE_STRICT;
            case RELAXED:
                return tech.ydb.proto.coordination.ConsistencyMode.CONSISTENCY_MODE_RELAXED;
            case UNSET:
            default:
                return tech.ydb.proto.coordination.ConsistencyMode.CONSISTENCY_MODE_UNSET;
        }
    }

    private static tech.ydb.proto.coordination.RateLimiterCountersMode toProto(RateLimiterCountersMode mode) {
        switch (mode) {
            case DETAILED:
                return tech.ydb.proto.coordination.RateLimiterCountersMode.RATE_LIMITER_COUNTERS_MODE_DETAILED;
            case AGGREGATED:
                return tech.ydb.proto.coordination.RateLimiterCountersMode.RATE_LIMITER_COUNTERS_MODE_AGGREGATED;
            case UNSET:
            default:
                return tech.ydb.proto.coordination.RateLimiterCountersMode.RATE_LIMITER_COUNTERS_MODE_UNSET;
        }
    }

    private static ConsistencyMode fromProto(tech.ydb.proto.coordination.ConsistencyMode mode) {
        switch (mode) {
            case CONSISTENCY_MODE_RELAXED:
                return ConsistencyMode.RELAXED;
            case CONSISTENCY_MODE_STRICT:
                return ConsistencyMode.STRICT;
            case CONSISTENCY_MODE_UNSET:
            default:
                return ConsistencyMode.UNSET;
        }
    }

    private static RateLimiterCountersMode fromProto(tech.ydb.proto.coordination.RateLimiterCountersMode mode) {
        switch (mode) {
            case RATE_LIMITER_COUNTERS_MODE_AGGREGATED:
                return RateLimiterCountersMode.AGGREGATED;
            case RATE_LIMITER_COUNTERS_MODE_DETAILED:
                return RateLimiterCountersMode.DETAILED;
            case RATE_LIMITER_COUNTERS_MODE_UNSET:
            default:
                return RateLimiterCountersMode.UNSET;
        }
    }
}
