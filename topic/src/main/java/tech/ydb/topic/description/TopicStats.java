package tech.ydb.topic.description;

import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nullable;

/**
 * @author Nikolay Perfilov
 */
public class TopicStats {
    private final long storeSizeBytes;
    private final Instant minLastWriteTime;
    @Nullable
    private final Duration maxWriteTimeLag;
    private final MultipleWindowsStat bytesWritten;

    private TopicStats(Builder builder) {
        this.storeSizeBytes = builder.storeSizeBytes;
        this.minLastWriteTime = builder.minLastWriteTime;
        this.maxWriteTimeLag = builder.maxWriteTimeLag;
        this.bytesWritten = builder.bytesWritten;
    }

    public long getStoreSizeBytes() {
        return storeSizeBytes;
    }

    public Instant getMinLastWriteTime() {
        return minLastWriteTime;
    }

    @Nullable
    public Duration getMaxWriteTimeLag() {
        return maxWriteTimeLag;
    }

    public MultipleWindowsStat getBytesWritten() {
        return bytesWritten;
    }

    public static TopicDescription.Builder newBuilder() {
        return new TopicDescription.Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private long storeSizeBytes;
        private Instant minLastWriteTime;
        private Duration maxWriteTimeLag = null;
        private MultipleWindowsStat bytesWritten;

        public Builder setStoreSizeBytes(long storeSizeBytes) {
            this.storeSizeBytes = storeSizeBytes;
            return this;
        }

        public Builder setMinLastWriteTime(Instant minLastWriteTime) {
            this.minLastWriteTime = minLastWriteTime;
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
    }
}
