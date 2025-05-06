package tech.ydb.topic.description;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;

/**
 * @author Nikolay Perfilov
 */
public class ConsumerPartitionInfo {
    private final long partitionId;
    private final boolean active;
    private final List<Long> childPartitionIds;
    private final List<Long> parentPartitionIds;
    private final PartitionStats partitionStats;
    private final ConsumerStats consumerStats;
    private final PartitionLocation location;

    public ConsumerPartitionInfo(YdbTopic.DescribeConsumerResult.PartitionInfo result) {
        this.partitionId = result.getPartitionId();
        this.active = result.getActive();
        this.childPartitionIds = result.getChildPartitionIdsList();
        this.parentPartitionIds = result.getParentPartitionIdsList();
        this.partitionStats = new PartitionStats(result.getPartitionStats());
        this.consumerStats = new ConsumerStats(result.getPartitionConsumerStats());
        this.location = new PartitionLocation(result.getPartitionLocation());
    }

    /**
     * @return Partition identifier.
     */
    public long getPartitionId() {
        return partitionId;
    }

    /**
     * @return Is partition open for write.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @return Ids of partitions which was formed when this partition was split or merged.
     */
    public List<Long> getChildPartitionIds() {
        return childPartitionIds;
    }

    /**
     * @return Ids of partitions from which this partition was formed by split or merge.
     */
    public List<Long> getParentPartitionIds() {
        return parentPartitionIds;
    }

    /**
     * @return Stats for partition, filled only when include_stats in request is true.
     */
    public PartitionStats getPartitionStats() {
        return partitionStats;
    }

    /**
     * @return Stats for consumer of this partition, filled only when include_stats in request is true.
     */
    public ConsumerStats getConsumerStats() {
        return consumerStats;
    }

    /**
     * @return Partition location, filled only when include_location in request is true.
     */
    public PartitionLocation getPartitionLocation() {
        return location;
    }

    public static class ConsumerStats {
        private final long lastReadOffset;
        private final long committedOffset;
        private final String readSessionId;
        private final Instant partitionReadSessionCreateTime;
        private final Instant lastReadTime;
        private final Duration maxReadTimeLag;
        private final Duration maxWriteTimeLag;

        private final MultipleWindowsStat bytesRead;
        private final String readerName;
        private final int connectionNodeId;

        public ConsumerStats(YdbTopic.DescribeConsumerResult.PartitionConsumerStats stats) {
            this.lastReadOffset = stats.getLastReadOffset();
            this.committedOffset = stats.getCommittedOffset();
            this.readSessionId = stats.getReadSessionId();
            this.partitionReadSessionCreateTime = ProtobufUtils.protoToInstant(
                    stats.getPartitionReadSessionCreateTime()
            );
            this.lastReadTime = ProtobufUtils.protoToInstant(stats.getLastReadTime());
            this.maxReadTimeLag = ProtobufUtils.protoToDuration(stats.getMaxReadTimeLag());
            this.maxWriteTimeLag = ProtobufUtils.protoToDuration(stats.getMaxWriteTimeLag());
            this.bytesRead = new MultipleWindowsStat(stats.getBytesRead());
            this.readerName = stats.getReaderName();
            this.connectionNodeId = stats.getConnectionNodeId();
        }

        /**
         * @return Last read offset from this partition.
         */
        public long getLastReadOffset() {
            return lastReadOffset;
        }

        /**
         * @return Committed offset for this partition.
         */
        public long getCommittedOffset() {
            return committedOffset;
        }

        /**
         * @return Reading this partition read session identifier.
         */
        public String getReadSessionId() {
            return readSessionId;
        }

        /**
         * @return Timestamp of providing this partition to this session by server.
         */
        public Instant getPartitionReadSessionCreateTime() {
            return partitionReadSessionCreateTime;
        }

        /**
         * @return Timestamp of last read from this partition.
         */
        public Instant getLastReadTime() {
            return lastReadTime;
        }

        /**
         * @return Maximum of differences between timestamp of read and write timestamp for all messages, read during last
         * minute.
         */
        public Duration getMaxReadTimeLag() {
            return maxReadTimeLag;
        }

        /**
         * @return Maximum of differences between write timestamp and create timestamp for all messages, read during last
         * minute.
         */
        public Duration getMaxWriteTimeLag() {
            return maxWriteTimeLag;
        }

        /**
         * @return How much bytes were read during several windows statistics from this partition.
         */
        public MultipleWindowsStat getBytesRead() {
            return bytesRead;
        }

        /**
         * @return Read session name, provided by client.
         */
        public String getReaderName() {
            return readerName;
        }

        /**
         * @return Host where read session connected.
         */
        public int getConnectionNodeId() {
            return connectionNodeId;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ConsumerPartitionInfo that = (ConsumerPartitionInfo) o;
        return partitionId == that.partitionId &&
                active == that.active &&
                Objects.equals(childPartitionIds, that.childPartitionIds) &&
                Objects.equals(parentPartitionIds, that.parentPartitionIds) &&
                Objects.equals(partitionStats, that.partitionStats) &&
                Objects.equals(consumerStats, that.consumerStats) &&
                Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partitionId, active, childPartitionIds, parentPartitionIds, partitionStats, consumerStats, location);
    }
}
