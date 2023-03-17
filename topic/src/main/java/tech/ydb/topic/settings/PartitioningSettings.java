package tech.ydb.topic.settings;

/**
 * @author Nikolay Perfilov
 */
public class PartitioningSettings {
    private final long minActivePartitions;
    private final long partitionCountLimit;

    public PartitioningSettings(long minActivePartitions, long partitionCountLimit) {
        this.minActivePartitions = minActivePartitions;
        this.partitionCountLimit = partitionCountLimit;
    }

    public long getMinActivePartitions() {
        return minActivePartitions;
    }

    public long getPartitionCountLimit() {
        return partitionCountLimit;
    }
}
