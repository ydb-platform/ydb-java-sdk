package tech.ydb.topic.settings;

/**
 * @author Nikolay Perfilov
 */
public class PartitioningSettings {
    private final long minActivePartitions;
    private final long partitionCountLimit;

    private PartitioningSettings(Builder builder) {
        this.minActivePartitions = builder.minActivePartitions;
        this.partitionCountLimit = builder.partitionCountLimit;
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
     *                             Zero value means default - 1.
     */
    public long getPartitionCountLimit() {
        return partitionCountLimit;
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
         *                             Zero value means default - 1.
         * @return settings builder
         */
        public Builder setPartitionCountLimit(long partitionCountLimit) {
            this.partitionCountLimit = partitionCountLimit;
            return this;
        }

        public PartitioningSettings build() {
            return new PartitioningSettings(this);
        }
    }
}
