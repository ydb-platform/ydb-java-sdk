package tech.ydb.topic.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 * @author Nikolay Perfilov
 */
public class CommitOffsetSettings extends OperationSettings {
    private final long partitionId;
    private final String consumer;
    private final long offset;
    private final String readSessionId;

    private CommitOffsetSettings(Builder builder) {
        super(builder);
        this.partitionId = builder.partitionId;
        this.consumer = builder.consumer;
        this.offset = builder.offset;
        this.readSessionId = builder.readSessionId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public long getPartitionId() {
        return partitionId;
    }

    public String getConsumer() {
        return consumer;
    }

    public long getOffset() {
        return offset;
    }

    public String getReadSessionId() {
        return readSessionId;
    }

    /*
     * BUILDER
     */
    public static class Builder extends OperationBuilder<Builder> {
        private long partitionId = -1;
        private String consumer = null;
        private long offset = 0;
        private String readSessionId = null;

        public Builder setPartitionId(long partitionId) {
            this.partitionId = partitionId;
            return this;
        }

        public Builder setConsumer(String consumer) {
            this.consumer = consumer;
            return this;
        }

        public Builder setOffset(long offset) {
            this.offset = offset;
            return this;
        }

        public Builder setReadSessionId(String sessionId) {
            this.readSessionId = sessionId;
            return this;
        }

        @Override
        public CommitOffsetSettings build() {
            return new CommitOffsetSettings(this);
        }
    }
}
