package tech.ydb.topic.settings;

import javax.annotation.Nullable;

/**
 * @author Nikolay Perfilov
 */
public class AlterPartitioningSettings {
    @Nullable
    private final Long minActivePartitions;
    @Nullable
    private final Long maxActivePartitions;
    @Nullable
    private final Long partitionCountLimit;
    @Nullable
    private final AutoPartitioningStrategy autoPartitioningStrategy;
    @Nullable
    private final AlterAutoPartitioningWriteStrategySettings writeStrategySettings;

    private AlterPartitioningSettings(Builder builder) {
        this.minActivePartitions = builder.minActivePartitions;
        this.maxActivePartitions = builder.maxActivePartitions;
        this.partitionCountLimit = builder.partitionCountLimit;
        this.autoPartitioningStrategy = builder.autoPartitioningStrategy;
        this.writeStrategySettings = builder.writeStrategySettings;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Nullable
    public Long getMinActivePartitions() {
        return minActivePartitions;
    }

    @Nullable
    public Long getMaxActivePartitions() {
        return maxActivePartitions;
    }

    @Nullable
    public Long getPartitionCountLimit() {
        return partitionCountLimit;
    }

    @Nullable
    public AutoPartitioningStrategy getAutoPartitioningStrategy() {
        return autoPartitioningStrategy;
    }

    @Nullable
    public AlterAutoPartitioningWriteStrategySettings getWriteStrategySettings() {
        return writeStrategySettings;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private Long minActivePartitions = null;
        private Long maxActivePartitions = null;
        private Long partitionCountLimit = null;
        private AutoPartitioningStrategy autoPartitioningStrategy = null;
        private AlterAutoPartitioningWriteStrategySettings writeStrategySettings = null;

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
         * @param maxActivePartitions  maximum partition count auto merge would stop working at.
         *                             Zero value means default - 1.
         * @return settings builder
         */
        public Builder setMaxActivePartitions(long maxActivePartitions) {
            this.maxActivePartitions = maxActivePartitions;
            return this;
        }

        /**
         * @param partitionCountLimit  Limit for total partition count, including active (open for write) and
         *                             read-only partitions.
         *                             Zero value means default - 100.
         * @return settings builder
         * @deprecated Use {@link #setMaxActivePartitions(long)} instead
         */
        @Deprecated
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
         *                                  See {@link AlterAutoPartitioningWriteStrategySettings} for defaults
         * @return settings builder
         */
        public Builder setWriteStrategySettings(AlterAutoPartitioningWriteStrategySettings writeStrategySettings) {
            this.writeStrategySettings = writeStrategySettings;
            return this;
        }

        public AlterPartitioningSettings build() {
            return new AlterPartitioningSettings(this);
        }
    }
}
