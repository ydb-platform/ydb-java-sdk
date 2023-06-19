package tech.ydb.topic.settings;

import javax.annotation.Nullable;

/**
 * @author Nikolay Perfilov
 */
public class AlterPartitioningSettings {
    @Nullable
    private final Long minActivePartitions;
    @Nullable
    private final Long partitionCountLimit;

    private AlterPartitioningSettings(Builder builder) {
        this.minActivePartitions = builder.minActivePartitions;
        this.partitionCountLimit = builder.partitionCountLimit;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Nullable
    public Long getMinActivePartitions() {
        return minActivePartitions;
    }

    @Nullable
    public Long getPartitionCountLimit() {
        return partitionCountLimit;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private Long minActivePartitions = null;
        private Long partitionCountLimit = null;

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

        public AlterPartitioningSettings build() {
            return new AlterPartitioningSettings(this);
        }
    }
}
