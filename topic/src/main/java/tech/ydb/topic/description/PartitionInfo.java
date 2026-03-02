package tech.ydb.topic.description;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import tech.ydb.proto.topic.YdbTopic;

/**
 * @author Nikolay Perfilov
 */
public class PartitionInfo {
    private final long partitionId;
    private final boolean active;
    private final List<Long> childPartitionIds;
    private final List<Long> parentPartitionIds;
    private final PartitionStats partitionStats;

    public PartitionInfo(YdbTopic.DescribeTopicResult.PartitionInfo info) {
        this.partitionId = info.getPartitionId();
        this.active = info.getActive();
        this.childPartitionIds = ImmutableList.copyOf(info.getChildPartitionIdsList());
        this.parentPartitionIds = ImmutableList.copyOf(info.getParentPartitionIdsList());
        this.partitionStats = info.hasPartitionStats() ? new PartitionStats(info.getPartitionStats()) : null;
    }

    public long getPartitionId() {
        return partitionId;
    }

    public boolean isActive() {
        return active;
    }

    public List<Long> getChildPartitionIds() {
        return childPartitionIds;
    }

    public List<Long> getParentPartitionIds() {
        return parentPartitionIds;
    }

    @Nullable
    public PartitionStats getPartitionStats() {
        return partitionStats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PartitionInfo that = (PartitionInfo) o;
        return partitionId == that.partitionId &&
                active == that.active &&
                Objects.equals(childPartitionIds, that.childPartitionIds) &&
                Objects.equals(parentPartitionIds, that.parentPartitionIds) &&
                Objects.equals(partitionStats, that.partitionStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partitionId, active, childPartitionIds, parentPartitionIds, partitionStats);
    }
}
