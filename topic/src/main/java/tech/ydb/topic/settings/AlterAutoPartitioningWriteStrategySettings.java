package tech.ydb.topic.settings;

import javax.annotation.Nullable;

import java.time.Duration;

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
         * @param stabilizationWindow       Duration used by the auto partitioning algorithm to define if the partition must be split.
         *                                  Default value is 5 minutes.
         * @return strategy builder
         */
        public Builder setStabilizationWindow(Duration stabilizationWindow) {
            this.stabilizationWindow = stabilizationWindow;
            return this;
        }

        /**
         * @param upUtilizationPercent      Upper level of partition quota utilization after which the partition should be split.
         *                                  Default value is 90%.
         * @return strategy builder
         */
        public Builder setUpUtilizationPercent(int upUtilizationPercent) {
            this.upUtilizationPercent = upUtilizationPercent;
            return this;
        }

        /**
         * @param downUtilizationPercent    Lower level of partition quota utilization
         *                                  after which the partition should be merged with the other one.
         *                                  Default value is 30%.
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
