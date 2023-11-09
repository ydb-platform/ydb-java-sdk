package tech.ydb.coordination.settings;

import java.time.Duration;

import com.google.common.base.Preconditions;

import tech.ydb.core.settings.OperationSettings;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationNodeSettings extends OperationSettings {
    private final Duration selfCheckPeriod;
    private final Duration sessionGracePeriod;
    private final NodeConsistenteMode readConsistencyMode;
    private final NodeConsistenteMode attachConsistencyMode;
    private final NodeRateLimiterCountersMode rateLimiterCountersMode;

    private CoordinationNodeSettings(Builder builder) {
        super(builder);
        Preconditions.checkArgument(
                builder.selfCheckPeriod.compareTo(builder.sessionGracePeriod) < 0,
                "SessionGracePeriod must be strictly more than SelfCheckPeriod"
        );

        this.selfCheckPeriod = builder.selfCheckPeriod;
        this.sessionGracePeriod = builder.sessionGracePeriod;
        this.readConsistencyMode = builder.readConsistencyMode;
        this.attachConsistencyMode = builder.attachConsistencyMode;
        this.rateLimiterCountersMode = builder.rateLimiterCountersMode;
    }

    public Duration getSelfCheckPeriod() {
        return selfCheckPeriod;
    }

    public Duration getSessionGracePeriod() {
        return sessionGracePeriod;
    }

    public NodeConsistenteMode getReadConsistencyMode() {
        return readConsistencyMode;
    }

    public NodeConsistenteMode getAttachConsistencyMode() {
        return attachConsistencyMode;
    }

    public NodeRateLimiterCountersMode getRateLimiterCountersMode() {
        return rateLimiterCountersMode;
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationSettings.OperationBuilder<Builder> {
        private Duration selfCheckPeriod = Duration.ofSeconds(1);
        private Duration sessionGracePeriod = Duration.ofSeconds(10);

        private NodeConsistenteMode readConsistencyMode = NodeConsistenteMode.UNSET;
        private NodeConsistenteMode attachConsistencyMode = NodeConsistenteMode.UNSET;

        private NodeRateLimiterCountersMode rateLimiterCountersMode = NodeRateLimiterCountersMode.UNSET;

        public Builder withSelfCheckPeriod(Duration period) {
            Preconditions.checkArgument(
                    !period.isNegative() && !period.isZero(),
                    "SelfCheckPeriod must be strictly greater than zero"
            );
            this.selfCheckPeriod = period;
            return this;
        }

        public Builder withSessionGracePeriod(Duration period) {
            Preconditions.checkArgument(
                    !period.isNegative() && !period.isZero(),
                    "SessionGracePeriod must be strictly greater than zero"
            );
            this.sessionGracePeriod = period;
            return this;
        }

        public Builder withReadConsistencyMode(NodeConsistenteMode mode) {
            this.readConsistencyMode = mode;
            return this;
        }

        public Builder withAttachConsistencyMode(NodeConsistenteMode mode) {
            this.attachConsistencyMode = mode;
            return this;
        }

        public Builder withRateLimiterCountersMode(NodeRateLimiterCountersMode mode) {
            this.rateLimiterCountersMode = mode;
            return this;
        }

        @Override
        public CoordinationNodeSettings build() {
            return new CoordinationNodeSettings(this);
        }
    }
}
