package tech.ydb.table.settings;

import javax.annotation.Nullable;

public class PartitioningSettings {
    @Nullable
    private Boolean partitioningBySize;
    @Nullable
    private Boolean partitioningByLoad;

    @Nullable
    public Boolean getPartitioningBySize() {
        return partitioningBySize;
    }

    @Nullable
    public Boolean getPartitioningByLoad() {
        return partitioningByLoad;
    }

    public PartitioningSettings setPartitioningBySize(boolean partitioningBySize) {
        this.partitioningBySize = partitioningBySize;
        return this;
    }

    public PartitioningSettings setPartitioningByLoad(boolean partitioningByLoad) {
        this.partitioningByLoad = partitioningByLoad;
        return this;
    }
}
