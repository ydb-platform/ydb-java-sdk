package tech.ydb.table.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * @author Sergey Polovko
 */
public class PartitioningPolicy {

    @Nullable
    private String presetName;
    @Nullable
    private AutoPartitioningPolicy autoPartitioning;
    private long uniformPartitions;

    @Nullable
    public String getPresetName() {
        return presetName;
    }

    public PartitioningPolicy setPresetName(@Nonnull String presetName) {
        this.presetName = presetName;
        return this;
    }

    @Nullable
    public AutoPartitioningPolicy getAutoPartitioning() {
        return autoPartitioning;
    }

    public PartitioningPolicy setAutoPartitioning(@Nonnull AutoPartitioningPolicy autoPartitioning) {
        this.autoPartitioning = autoPartitioning;
        return this;
    }

    public long getUniformPartitions() {
        return uniformPartitions;
    }

    public PartitioningPolicy setUniformPartitions(long uniformPartitions) {
        this.uniformPartitions = uniformPartitions;
        return this;
    }
}
