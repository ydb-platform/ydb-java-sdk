package tech.ydb.topic.description;

import java.time.Duration;
import java.time.Instant;

/**
 * @author Nikolay Perfilov
 */
public class ConsumerStats {
    private final Instant minPartitionsLastTime;
    private final Duration maxReadTimeLag;
    private final Duration maxWriteTimeLag;
    private final MultipleWindowsStat bytesRead;

    private ConsumerStats(Builder builder) {
        this.minPartitionsLastTime = builder.minPartitionsLastTime;
        this.maxReadTimeLag = builder.maxReadTimeLag;
        this.maxWriteTimeLag = builder.maxWriteTimeLag;
        this.bytesRead = builder.bytesRead;
    }

    public Instant getMinPartitionsLastTime() {
        return minPartitionsLastTime;
    }

    public Duration getMaxReadTimeLag() {
        return maxReadTimeLag;
    }

    public Duration getMaxWriteTimeLag() {
        return maxWriteTimeLag;
    }

    public MultipleWindowsStat getBytesRead() {
        return bytesRead;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private Instant minPartitionsLastTime = Instant.EPOCH;
        private Duration maxReadTimeLag = Duration.ZERO;
        private Duration maxWriteTimeLag = Duration.ZERO;
        private MultipleWindowsStat bytesRead;

        public Builder setMinPartitionsLastTime(Instant minPartitionsLastTime) {
            this.minPartitionsLastTime = minPartitionsLastTime;
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
    }
}
