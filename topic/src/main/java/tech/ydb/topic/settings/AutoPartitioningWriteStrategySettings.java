package tech.ydb.topic.settings;

import java.time.Duration;

public class AutoPartitioningWriteStrategySettings {
    private final Duration stabilizationWindow;
    private final int upUtilizationPercent;
    private final int downUtilizationPercent;

    public AutoPartitioningWriteStrategySettings(Builder builder) {
        this.stabilizationWindow = builder.stabilizationWindow;
        this.upUtilizationPercent = builder.upUtilizationPercent;
        this.downUtilizationPercent = builder.downUtilizationPercent;
    }

    public Duration getStabilizationWindow() {
        return stabilizationWindow;
    }

    public int getUpUtilizationPercent() {
        return upUtilizationPercent;
    }

    public int getDownUtilizationPercent() {
        return downUtilizationPercent;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Duration stabilizationWindow = Duration.ofMinutes(5);
        private int upUtilizationPercent = 90;
        private int downUtilizationPercent = 30;

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

        public AutoPartitioningWriteStrategySettings build() {
            return new AutoPartitioningWriteStrategySettings(this);
        }
    }
}
