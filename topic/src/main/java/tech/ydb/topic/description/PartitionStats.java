package tech.ydb.topic.description;

import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nullable;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.read.impl.OffsetsRangeImpl;

/**
 * @author Nikolay Perfilov
 */
public class PartitionStats {
    private final OffsetsRange partitionOffsets;
    private final long storeSizeBytes;
    private final Instant lastWriteTime;
    @Nullable
    private final Duration maxWriteTimeLag;
    private final MultipleWindowsStat bytesWritten;
    private final int partitionNodeId;

    @SuppressWarnings("deprecation")
    public PartitionStats(YdbTopic.PartitionStats stats) {
        this.partitionOffsets = new OffsetsRangeImpl(
                stats.getPartitionOffsets().getStart(),
                stats.getPartitionOffsets().getEnd()
        );
        this.storeSizeBytes = stats.getStoreSizeBytes();
        this.lastWriteTime = ProtobufUtils.protoToInstant(stats.getLastWriteTime());
        this.maxWriteTimeLag = ProtobufUtils.protoToDuration(stats.getMaxWriteTimeLag());
        this.bytesWritten = new MultipleWindowsStat(
                stats.getBytesWritten().getPerMinute(),
                stats.getBytesWritten().getPerHour(),
                stats.getBytesWritten().getPerDay()
        );

        this.partitionNodeId = stats.getPartitionNodeId();
    }

    @Deprecated
    private PartitionStats(Builder builder) {
        this.partitionOffsets = builder.partitionOffsets;
        this.storeSizeBytes = builder.storeSizeBytes;
        this.lastWriteTime = builder.lastWriteTime;
        this.maxWriteTimeLag = builder.maxWriteTimeLag;
        this.bytesWritten = builder.bytesWritten;
        this.partitionNodeId = builder.partitionNodeId;
    }

    public OffsetsRange getPartitionOffsets() {
        return partitionOffsets;
    }

    public long getStoreSizeBytes() {
        return storeSizeBytes;
    }

    public Instant getLastWriteTime() {
        return lastWriteTime;
    }

    @Nullable
    public Duration getMaxWriteTimeLag() {
        return maxWriteTimeLag;
    }

    public MultipleWindowsStat getBytesWritten() {
        return bytesWritten;
    }

    @Deprecated
    public int getPartitionNodeId() {
        return partitionNodeId;
    }

    @Deprecated
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    @Deprecated
    public static class Builder {
        private OffsetsRange partitionOffsets;
        private long storeSizeBytes;
        private Instant lastWriteTime;
        private Duration maxWriteTimeLag = null;
        private MultipleWindowsStat bytesWritten;
        private int partitionNodeId;

        public Builder setPartitionOffsets(OffsetsRange partitionOffsets) {
            this.partitionOffsets = partitionOffsets;
            return this;
        }

        public Builder setStoreSizeBytes(long storeSizeBytes) {
            this.storeSizeBytes = storeSizeBytes;
            return this;
        }

        public Builder setLastWriteTime(Instant lastWriteTime) {
            this.lastWriteTime = lastWriteTime;
            return this;
        }

        public Builder setMaxWriteTimeLag(Duration maxWriteTimeLag) {
            this.maxWriteTimeLag = maxWriteTimeLag;
            return this;
        }

        public Builder setBytesWritten(MultipleWindowsStat bytesWritten) {
            this.bytesWritten = bytesWritten;
            return this;
        }

        public Builder setPartitionNodeId(int partitionNodeId) {
            this.partitionNodeId = partitionNodeId;
            return this;
        }

        public PartitionStats build() {
            return new PartitionStats(this);
        }
    }
}
