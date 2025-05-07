package tech.ydb.topic.settings;

public enum AutoPartitioningStrategy {
    /**
     * The auto partitioning is disabled.
     * You cannot disable the auto partitioning after it has been enabled.
     * @see AutoPartitioningStrategy#PAUSED
     */
    DISABLED,
    /**
     * The auto partitioning algorithm will increase the partition count depending on the load characteristics.
     * The auto partitioning algorithm will never decrease the number of partitions.
     * @see AlterAutoPartitioningWriteStrategySettings
     */
    SCALE_UP,
    /**
     * The auto partitioning algorithm will both increase and decrease partitions count depending on the load characteristics.
     * @see AlterAutoPartitioningWriteStrategySettings
     */
    SCALE_UP_AND_DOWN,
    /**
     * The auto partitioning is paused.
     */
    PAUSED;
}
