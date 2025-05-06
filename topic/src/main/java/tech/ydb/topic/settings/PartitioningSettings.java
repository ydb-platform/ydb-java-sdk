package tech.ydb.topic.settings;

import java.util.Objects;

/**
 * @author Nikolay Perfilov
 */
public class PartitioningSettings {
    private final long minActivePartitions;
    private final long partitionCountLimit;
    private final AutoPartitioningStrategy autoPartitioningStrategy;
    private final AutoPartitioningWriteStrategySettings writeStrategySettings;

    private PartitioningSettings(Builder builder) {
        this.minActivePartitions = builder.minActivePartitions;
        this.partitionCountLimit = builder.partitionCountLimit;
        this.autoPartitioningStrategy = builder.autoPartitioningStrategy;
        this.writeStrategySettings = builder.writeStrategySettings;
    }

    /**
     * @return  minimum partition count auto merge would stop working at.
     *                             Zero value means default - 1.
     */
    public long getMinActivePartitions() {
        return minActivePartitions;
    }

    /**
     * @return  Limit for total partition count, including active (open for write) and
     *                             read-only partitions.
     *                             Zero value means default - 100.
     */
    public long getPartitionCountLimit() {
        return partitionCountLimit;
    }

    /**
     * @return  Auto partitioning strategy. Disabled by default
     */
    public AutoPartitioningStrategy getAutoPartitioningStrategy() {
        return autoPartitioningStrategy;
    }

    /**
     * @return  Auto partitioning write strategy settings. Does not have effect until the auto partitioning is enabled
     */
    public AutoPartitioningWriteStrategySettings getWriteStrategySettings() {
        return writeStrategySettings;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private long minActivePartitions = 0;
        private long partitionCountLimit = 0;
        private AutoPartitioningStrategy autoPartitioningStrategy = AutoPartitioningStrategy.DISABLED;
        private AutoPartitioningWriteStrategySettings writeStrategySettings = null;

        /**
         * @param minActivePartitions  minimum partition count auto merge would stop working at.
         *                             Zero value means default - 1.
         * @return settings builder
         */
        public Builder setMinActivePartitions(long minActivePartitions) {
            this.minActivePartitions = minActivePartitions;
            return this;
        }

        /**
         * @param partitionCountLimit  Limit for total partition count, including active (open for write) and
         *                             read-only partitions.
         *                             Zero value means default - 100.
         * @return settings builder
         */
        public Builder setPartitionCountLimit(long partitionCountLimit) {
            this.partitionCountLimit = partitionCountLimit;
            return this;
        }

        /**
         * @param autoPartitioningStrategy  Strategy for auto partitioning.
         *                                  Auto partitioning is disabled by default.
         * @return settings builder
         * @see AutoPartitioningStrategy#DISABLED
         */
        public Builder setAutoPartitioningStrategy(AutoPartitioningStrategy autoPartitioningStrategy) {
            this.autoPartitioningStrategy = autoPartitioningStrategy;
            return this;
        }

        /**
         * @param writeStrategySettings     Settings for auto partitioning write strategy.
         *                                  Does not have any effect if auto partitioning is disabled.
         *                                  See {@link AutoPartitioningWriteStrategySettings} for defaults
         * @return settings builder
         */
        public Builder setWriteStrategySettings(AutoPartitioningWriteStrategySettings writeStrategySettings) {
            this.writeStrategySettings = writeStrategySettings;
            return this;
        }

        public PartitioningSettings build() {
            return new PartitioningSettings(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PartitioningSettings that = (PartitioningSettings) o;
        return minActivePartitions == that.minActivePartitions &&
                partitionCountLimit == that.partitionCountLimit &&
                autoPartitioningStrategy == that.autoPartitioningStrategy &&
                Objects.equals(writeStrategySettings, that.writeStrategySettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minActivePartitions, partitionCountLimit, autoPartitioningStrategy, writeStrategySettings);
    }
}
