package tech.ydb.topic.settings;

import java.time.Duration;
import java.util.Objects;

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

        public AutoPartitioningWriteStrategySettings build() {
            return new AutoPartitioningWriteStrategySettings(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AutoPartitioningWriteStrategySettings that = (AutoPartitioningWriteStrategySettings) o;
        return upUtilizationPercent == that.upUtilizationPercent &&
                downUtilizationPercent == that.downUtilizationPercent &&
                Objects.equals(stabilizationWindow, that.stabilizationWindow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stabilizationWindow, upUtilizationPercent, downUtilizationPercent);
    }
}
