package tech.ydb.topic.settings;

/**
 * @author Nikolay Perfilov
 */
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

        /**
         * Reads in this partition session will start from offset no less than readOffset.
         * If readOffset is set, server will check if that readOffset is not less than the actual committed offset.
         * If the check fails then server will send an error message (status != SUCCESS) and close the stream.
         * If readOffset is not set or is null (which is default), no check will be made.
         *
         * InitRequest.max_lag and InitRequest.read_from could lead to skip of more messages.
         * Server will return data starting from offset that is maximum of actual committed offset, read_offset (if set)
         * and offsets calculated from InitRequest.max_lag and InitRequest.read_from.
         *
         * @param readOffset Offset to read from. Default: null
         * @return Builder
         */
        public Builder setReadOffset(Long readOffset) {
            this.readOffset = readOffset;
            return this;
        }

        /**
         * Make server know that all messages with offsets less than commitOffset were fully processed by client.
         * Server will commit this position if it is not already done.
         *
         * @param commitOffset Commit offset, following the offset of last processed (committed) message. Default: null
         * @return Builder
         */
        public Builder setCommitOffset(Long commitOffset) {
            this.commitOffset = commitOffset;
            return this;
        }

        public StartPartitionSessionSettings build() {
            return new StartPartitionSessionSettings(this);
        }

    }
}
