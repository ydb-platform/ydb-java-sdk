package tech.ydb.table.settings;

import javax.annotation.Nullable;

public class PartitioningSettings {
    @Nullable
    private Boolean partitioningBySize;
    @Nullable
    private Boolean partitioningByLoad;
    @Nullable
    private Long partitionSizeMb;
    @Nullable
    private Long minPartitionsCount;
    @Nullable
    private Long maxPartitionsCount;

    @Nullable
    public Boolean getPartitioningBySize() {
        return partitioningBySize;
    }

    @Nullable
    public Boolean getPartitioningByLoad() {
        return partitioningByLoad;
    }

    @Nullable
    public Long getPartitionSizeMb() {
        return partitionSizeMb;
    }

    @Nullable
    public Long getMinPartitionsCount() {
        return minPartitionsCount;
    }

    @Nullable
    public Long getMaxPartitionsCount() {
        return maxPartitionsCount;
    }

    public PartitioningSettings setPartitioningBySize(boolean partitioningBySize) {
        this.partitioningBySize = partitioningBySize;
        return this;
    }

    public PartitioningSettings setPartitioningByLoad(boolean partitioningByLoad) {
        this.partitioningByLoad = partitioningByLoad;
        return this;
    }

    public PartitioningSettings setPartitionSize(long partitionSizeMb) {
        this.partitionSizeMb = partitionSizeMb;
        return this;
    }

    public PartitioningSettings setMinPartitionsCount(long partitionsCount) {
        this.minPartitionsCount = partitionsCount;
        return this;
    }

    public PartitioningSettings setMaxPartitionsCount(long partitionsCount) {
        this.maxPartitionsCount = partitionsCount;
        return this;
    }
}
