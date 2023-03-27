package tech.ydb.topic.settings;

public class StartPartitionSessionSettings {
    private final Long readOffset;
    private final Long commitOffset;

    private StartPartitionSessionSettings(Builder builder) {
        this.readOffset = builder.readOffset;
        this.commitOffset = builder.commitOffset;
    }

    public Long getReadOffset() {
        return readOffset;
    }

    public Long getCommitOffset() {
        return commitOffset;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private Long readOffset;
        private Long commitOffset;

        public void setReadOffset(Long readOffset) {
            this.readOffset = readOffset;
        }

        public void setCommitOffset(Long commitOffset) {
            this.commitOffset = commitOffset;
        }

        public StartPartitionSessionSettings build() {
            return new StartPartitionSessionSettings(this);
        }

    }
}
