package tech.ydb.topic.settings;

import java.time.Duration;
import java.time.Instant;

public class WriteSettings {
    private final Duration timeout;
    private final Long seqNo;
    private final Instant createTimestamp;
    private final Duration blockingTimeout;

    private WriteSettings(Builder builder) {
        this.timeout = builder.timeout;
        this.seqNo = builder.seqNo;
        this.createTimestamp = builder.createTimestamp;
        this.blockingTimeout = builder.blockingTimeout;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Long getSeqNo() {
        return seqNo;
    }

    public Instant getCreateTimestamp() {
        return createTimestamp;
    }

    public Duration getBlockingTimeout() {
        return blockingTimeout;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private Duration timeout = Duration.ZERO;
        private Long seqNo = null;
        private Instant createTimestamp = null;
        private Duration blockingTimeout = null;

        public Builder setTimeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder setSeqNo(long seqNo) {
            this.seqNo = seqNo;
            return this;
        }

        public Builder setCreateTimestamp(Instant createTimestamp) {
            this.createTimestamp = createTimestamp;
            return this;
        }

        public Builder setBlockingTimeout(Duration blockingTimeout) {
            this.blockingTimeout = blockingTimeout;
            return this;
        }

        public WriteSettings build() {
            return new WriteSettings(this);
        }
    }
}
