package tech.ydb.topic.settings;

import javax.annotation.Nullable;

import tech.ydb.proto.topic.YdbTopic;

public enum AutoPartitioningStrategy {
    /**
     * The auto partitioning is disabled.
     * You cannot disable the auto partitioning after it has been enabled.
     * @see AutoPartitioningStrategy#PAUSED
     */
    DISABLED(YdbTopic.AutoPartitioningStrategy.AUTO_PARTITIONING_STRATEGY_DISABLED),
    /**
     * The auto partitioning algorithm will increase the partition count depending on the load characteristics.
     * The auto partitioning algorithm will never decrease the number of partitions.
     * @see AlterAutoPartitioningWriteStrategySettings
     */
    SCALE_UP(YdbTopic.AutoPartitioningStrategy.AUTO_PARTITIONING_STRATEGY_SCALE_UP),
    /**
     * The auto partitioning algorithm will both increase and decrease partitions count depending on the load characteristics.
     * @see AlterAutoPartitioningWriteStrategySettings
     */
    SCALE_UP_AND_DOWN(YdbTopic.AutoPartitioningStrategy.AUTO_PARTITIONING_STRATEGY_SCALE_UP_AND_DOWN),
    /**
     * The auto partitioning is paused.
     */
    PAUSED(YdbTopic.AutoPartitioningStrategy.AUTO_PARTITIONING_STRATEGY_PAUSED);

    private final YdbTopic.AutoPartitioningStrategy protoReference;

    AutoPartitioningStrategy(YdbTopic.AutoPartitioningStrategy protoReference) {
        this.protoReference = protoReference;
    }

    public YdbTopic.AutoPartitioningStrategy getProtoReference() {
        return protoReference;
    }

    public static @Nullable AutoPartitioningStrategy fromProto(YdbTopic.AutoPartitioningStrategy protoReference) {
        for (AutoPartitioningStrategy value : values()) {
            if (value.getProtoReference() == protoReference) {
                return value;
            }
        }
        return null;
    }
}
