package tech.ydb.table.settings;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import tech.ydb.table.values.Type;
import tech.ydb.table.values.TypedValue;
import tech.ydb.table.values.Value;


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
    private List<TypedValue<?>> explicitPartitioningPoints;


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

    public PartitioningPolicy setExplicitPartitioningPoints(@Nullable List<TypedValue<?>> explicitPartitioningPoints) {
        this.explicitPartitioningPoints = explicitPartitioningPoints;
        return this;
    }

    public <T extends Type> PartitioningPolicy addExplicitPartitioningPoint(T type, Value<T> value) {
        if (this.explicitPartitioningPoints == null) {
            this.explicitPartitioningPoints = new ArrayList<>(2);
        }
        this.explicitPartitioningPoints.add(new TypedValue<>(type, value));
        return this;
    }

    @Nullable
    public List<TypedValue<?>> getExplicitPartitioningPoints() {
        return explicitPartitioningPoints;
    }
}
