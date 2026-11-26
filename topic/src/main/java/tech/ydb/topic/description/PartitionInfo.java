package tech.ydb.topic.description;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

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
    private final boolean keyRange;
    @Nullable
    private final ByteString fromBound;
    @Nullable
    private final ByteString toBound;

    public PartitionInfo(YdbTopic.DescribeTopicResult.PartitionInfo info) {
        this.partitionId = info.getPartitionId();
        this.active = info.getActive();
        this.childPartitionIds = ImmutableList.copyOf(info.getChildPartitionIdsList());
        this.parentPartitionIds = ImmutableList.copyOf(info.getParentPartitionIdsList());
        this.partitionStats = info.hasPartitionStats() ? new PartitionStats(info.getPartitionStats()) : null;
        this.keyRange = info.hasKeyRange();
        YdbTopic.PartitionKeyRange range = info.getKeyRange();
        this.fromBound = range.hasFromBound() ? range.getFromBound() : null;
        this.toBound = range.hasToBound() ? range.getToBound() : null;
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

    /**
     * Whether the server supplied a key range, including a range with both ends unbounded.
     * @return true if key-range metadata is present
     */
    public boolean hasKeyRange() {
        return keyRange;
    }

    public boolean hasFromBound() {
        return fromBound != null;
    }

    public boolean hasToBound() {
        return toBound != null;
    }

    /**
     * Inclusive lower bound, compared as unsigned bytes. Empty means unbounded.
     * @return a defensive copy of the bound
     */
    public byte[] getFromBound() {
        return fromBound == null ? new byte[0] : fromBound.toByteArray();
    }

    /**
     * Exclusive upper bound, compared as unsigned bytes. Empty means unbounded.
     * @return a defensive copy of the bound
     */
    public byte[] getToBound() {
        return toBound == null ? new byte[0] : toBound.toByteArray();
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
                Objects.equals(partitionStats, that.partitionStats) &&
                keyRange == that.keyRange &&
                Objects.equals(fromBound, that.fromBound) &&
                Objects.equals(toBound, that.toBound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partitionId, active, childPartitionIds, parentPartitionIds, partitionStats,
                keyRange, fromBound, toBound);
    }
}
