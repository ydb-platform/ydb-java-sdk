package tech.ydb.topic.description;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

/**
 * @author Nikolay Perfilov
 */
public class PartitionInfo {
    private final long partitionId;
    private final boolean active;
    private final List<Long> childPartitionIds;
    private final List<Long> parentPartitionIds;
    private final PartitionStats partitionStats;

    private PartitionInfo(Builder builder) {
        this.partitionId = builder.partitionId;
        this.active = builder.active;
        this.childPartitionIds = ImmutableList.copyOf(builder.childPartitionIds);
        this.parentPartitionIds = ImmutableList.copyOf(builder.parentPartitionIds);
        this.partitionStats = builder.partitionStats;
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

    public PartitionStats getPartitionStats() {
        return partitionStats;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private long partitionId;
        private boolean active;
        private List<Long> childPartitionIds = new ArrayList<>();
        private List<Long> parentPartitionIds = new ArrayList<>();
        private PartitionStats partitionStats;

        public Builder setPartitionId(long partitionId) {
            this.partitionId = partitionId;
            return this;
        }

        public Builder setActive(boolean active) {
            this.active = active;
            return this;
        }

        public Builder setChildPartitionIds(List<Long> childPartitionIds) {
            this.childPartitionIds = childPartitionIds;
            return this;
        }

        public Builder setParentPartitionIds(List<Long> parentPartitionIds) {
            this.parentPartitionIds = parentPartitionIds;
            return this;
        }

        public Builder setPartitionStats(PartitionStats partitionStats) {
            this.partitionStats = partitionStats;
            return this;
        }

        public PartitionInfo build() {
            return new PartitionInfo(this);
        }
    }

    @Override
    public boolean equals(Object o) {
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
