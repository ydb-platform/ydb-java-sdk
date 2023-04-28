package tech.ydb.coordination.settings;

import com.google.common.base.Preconditions;

import tech.ydb.core.settings.OperationSettings;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationNodeSettings extends OperationSettings {

    /**
     * Period in milliseconds for self-checks (default 1 second)
     */
    private final int selfCheckPeriodMillis;

    /**
     * Grace period for sessions on leader change (default 10 seconds)
     */
    private final int sessionGracePeriodMillis;

    /**
     * Consistency mode for read operations
     */
    private final ConsistencyMode readConsistencyMode;

    /**
     * Consistency mode for attach operations
     */
    private final ConsistencyMode attachConsistencyMode;

    /**
     * Rate limiter counters mode
     */
    private final RateLimiterCountersMode rateLimiterCountersMode;

    private CoordinationNodeSettings(
            Builder builder
    ) {
        super(builder);
        Preconditions.checkArgument(
                builder.selfCheckPeriodMillis < builder.sessionGracePeriodMillis,
                "SessionGracePeriod must be strictly more than SelfCheckPeriod"
        );

        this.selfCheckPeriodMillis = builder.selfCheckPeriodMillis;
        this.sessionGracePeriodMillis = builder.sessionGracePeriodMillis;
        this.readConsistencyMode = builder.readConsistencyMode;
        this.attachConsistencyMode = builder.attachConsistencyMode;
        this.rateLimiterCountersMode = builder.rateLimiterCountersMode;
    }

    public int getSelfCheckPeriodMillis() {
        return selfCheckPeriodMillis;
    }

    public int getSessionGracePeriodMillis() {
        return sessionGracePeriodMillis;
    }

    public ConsistencyMode getReadConsistencyMode() {
        return readConsistencyMode;
    }

    public ConsistencyMode getAttachConsistencyMode() {
        return attachConsistencyMode;
    }

    public RateLimiterCountersMode getRateLimiterCountersMode() {
        return rateLimiterCountersMode;
    }

    public enum ConsistencyMode {
        /**
         * Strict mode makes sure operations may only complete on current leader
         */
        CONSISTENCY_MODE_STRICT,

        /**
         * Relaxed mode allows operations to complete on stale masters
         */
        CONSISTENCY_MODE_RELAXED
    }

    public enum RateLimiterCountersMode {
        /**
         * Aggregated counters for resource tree
         */
        RATE_LIMITER_COUNTERS_MODE_AGGREGATED,

        /**
         * Counters on every resource
         */
        RATE_LIMITER_COUNTERS_MODE_DETAILED
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationSettings.OperationBuilder<Builder> {
        private int selfCheckPeriodMillis = 1_000;
        private int sessionGracePeriodMillis = 10_000;

        private ConsistencyMode readConsistencyMode = ConsistencyMode.CONSISTENCY_MODE_RELAXED;
        private ConsistencyMode attachConsistencyMode = ConsistencyMode.CONSISTENCY_MODE_STRICT;

        private RateLimiterCountersMode rateLimiterCountersMode = null;

        public Builder setSelfCheckPeriodMillis(int selfCheckPeriodMillis) {
            Preconditions.checkArgument(
                    selfCheckPeriodMillis > 0,
                    "SelfCheckPeriod must be strictly greater than zero"
            );
            this.selfCheckPeriodMillis = selfCheckPeriodMillis;

            return this;
        }

        public Builder setSessionGracePeriodMillis(int sessionGracePeriodMillis) {
            Preconditions.checkArgument(
                    sessionGracePeriodMillis > 0,
                    "SessionGracePeriod must be strictly greater than zero"
            );
            this.sessionGracePeriodMillis = sessionGracePeriodMillis;

            return this;
        }

        public Builder setReadConsistencyMode(ConsistencyMode readConsistencyMode) {
            this.readConsistencyMode = readConsistencyMode;

            return this;
        }

        public Builder setAttachConsistencyMode(ConsistencyMode attachConsistencyMode) {
            this.attachConsistencyMode = attachConsistencyMode;

            return this;
        }

        public Builder setRateLimiterCountersMode(RateLimiterCountersMode rateLimiterCountersMode) {
            this.rateLimiterCountersMode = rateLimiterCountersMode;

            return this;
        }

        @Override
        public CoordinationNodeSettings build() {
            return new CoordinationNodeSettings(this);
        }
    }
}
