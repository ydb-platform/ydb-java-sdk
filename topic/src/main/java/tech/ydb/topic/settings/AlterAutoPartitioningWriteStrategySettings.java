package tech.ydb.topic.settings;

import javax.annotation.Nullable;

import java.time.Duration;

// TODO add proper javadocs
public class AlterAutoPartitioningWriteStrategySettings {
    @Nullable
    private final Duration stabilizationWindow;
    @Nullable
    private final Integer upUtilizationPercent;
    @Nullable
    private final Integer downUtilizationPercent;

    public AlterAutoPartitioningWriteStrategySettings(Builder builder) {
        this.stabilizationWindow = builder.stabilizationWindow;
        this.upUtilizationPercent = builder.upUtilizationPercent;
        this.downUtilizationPercent = builder.downUtilizationPercent;
    }

    @Nullable
    public Duration getStabilizationWindow() {
        return stabilizationWindow;
    }

    @Nullable
    public Integer getUpUtilizationPercent() {
        return upUtilizationPercent;
    }

    @Nullable
    public Integer getDownUtilizationPercent() {
        return downUtilizationPercent;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Duration stabilizationWindow = null;
        private Integer upUtilizationPercent = null;
        private Integer downUtilizationPercent = null;

        /**
         * @param stabilizationWindow
         * @return strategy builder
         */
        public Builder setStabilizationWindow(Duration stabilizationWindow) {
            this.stabilizationWindow = stabilizationWindow;
            return this;
        }

        /**
         * @param upUtilizationPercent
         * @return strategy builder
         */
        public Builder setUpUtilizationPercent(int upUtilizationPercent) {
            this.upUtilizationPercent = upUtilizationPercent;
            return this;
        }

        /**
         * @param downUtilizationPercent
         * @return strategy builder
         */
        public Builder setDownUtilizationPercent(int downUtilizationPercent) {
            this.downUtilizationPercent = downUtilizationPercent;
            return this;
        }

        public AlterAutoPartitioningWriteStrategySettings build() {
            return new AlterAutoPartitioningWriteStrategySettings(this);
        }
    }
}
