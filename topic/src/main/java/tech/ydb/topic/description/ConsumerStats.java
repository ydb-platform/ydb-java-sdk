package tech.ydb.topic.description;

import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nullable;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;

/**
 * @author Nikolay Perfilov
 */
public class ConsumerStats {
    private final Instant minPartitionsLastReadTime;
    @Nullable
    private final Duration maxReadTimeLag;
    @Nullable
    private final Duration maxWriteTimeLag;
    private final MultipleWindowsStat bytesRead;

    @Deprecated
    private ConsumerStats(Builder builder) {
        this.minPartitionsLastReadTime = builder.minPartitionsLastReadTime;
        this.maxReadTimeLag = builder.maxReadTimeLag;
        this.maxWriteTimeLag = builder.maxWriteTimeLag;
        this.bytesRead = builder.bytesRead;
    }

    public ConsumerStats(YdbTopic.Consumer.ConsumerStats stats) {
        this.minPartitionsLastReadTime = ProtobufUtils.protoToInstant(stats.getMinPartitionsLastReadTime());
        this.maxReadTimeLag = ProtobufUtils.protoToDuration(stats.getMaxReadTimeLag());
        this.maxWriteTimeLag = ProtobufUtils.protoToDuration(stats.getMaxWriteTimeLag());
        this.bytesRead = new MultipleWindowsStat(
                stats.getBytesRead().getPerMinute(),
                stats.getBytesRead().getPerHour(),
                stats.getBytesRead().getPerDay()
        );
    }

    @Deprecated
    public static Builder newBuilder() {
        return new Builder();
    }

    public Instant getMinPartitionsLastReadTime() {
        return minPartitionsLastReadTime;
    }

    @Nullable
    public Duration getMaxReadTimeLag() {
        return maxReadTimeLag;
    }

    @Nullable
    public Duration getMaxWriteTimeLag() {
        return maxWriteTimeLag;
    }

    public MultipleWindowsStat getBytesRead() {
        return bytesRead;
    }

    /**
     * BUILDER
     */
    @Deprecated
    public static class Builder {
        private Instant minPartitionsLastReadTime = Instant.EPOCH;
        private Duration maxReadTimeLag = null;
        private Duration maxWriteTimeLag = null;
        private MultipleWindowsStat bytesRead;

        public Builder setMinPartitionsLastReadTime(Instant minPartitionsLastReadTime) {
            this.minPartitionsLastReadTime = minPartitionsLastReadTime;
            return this;
        }

        public Builder setMaxReadTimeLag(Duration maxReadTimeLag) {
            this.maxReadTimeLag = maxReadTimeLag;
            return this;
        }

        public Builder setMaxWriteTimeLag(Duration maxWriteTimeLag) {
            this.maxWriteTimeLag = maxWriteTimeLag;
            return this;
        }

        public Builder setBytesRead(MultipleWindowsStat bytesRead) {
            this.bytesRead = bytesRead;
            return this;
        }

        public ConsumerStats build() {
            return new ConsumerStats(this);
        }
    }
}
