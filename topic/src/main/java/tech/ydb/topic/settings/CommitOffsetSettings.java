package tech.ydb.topic.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 * @author Nikolay Perfilov
 */
public class CommitOffsetSettings extends OperationSettings {
    private final long partitionId;
    private final String consumer;
    private final long offset;

    private CommitOffsetSettings(Builder builder) {
        super(builder);
        this.partitionId = builder.partitionId;
        this.consumer = builder.consumer;
        this.offset = builder.offset;
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

    /**
     * BUILDER
     */
    public static class Builder extends OperationBuilder<DescribeTopicSettings.Builder> {
        private long partitionId = -1;
        private String consumer = null;
        private long offset = 0;

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

        @Override
        public CommitOffsetSettings build() {
            return new CommitOffsetSettings(this);
        }
    }
}
