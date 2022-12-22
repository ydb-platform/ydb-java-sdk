package tech.ydb.topic.settings;

import java.time.Duration;
import java.util.concurrent.Executor;

import com.google.common.util.concurrent.MoreExecutors;

import tech.ydb.topic.description.Codec;

/**
 * @author Nikolay Perfilov
 */
public class WriterSettings {
    private static final long MAX_MEMORY_USAGE_BYTES_DEFAULT = 20 * 1024 * 1024; // 20 MB
    private static final int MAX_INFLIGHT_COUNT_DEFAULT = 100000; // 20 MB

    private final String topicPath;
    private final String producerId;
    private final String messageGroupId;
    private final Long partitionId;
    private final Codec codec;
    private final int compressionLevel;
    private final long maxMemoryUsageBytes;
    private final Duration maxLag;
    private final int maxInflightCount;
    private final Duration batchFlushInterval;
    private final long batchFlushSiseBytes;
    private final Executor compressionExecutor;

    private WriterSettings(Builder builder) {
        this.topicPath = builder.topicPath;
        this.producerId = builder.producerId;
        this.messageGroupId = builder.messageGroupId;
        this.partitionId = builder.partitionId;
        this.codec = builder.codec;
        this.compressionLevel = builder.compressionLevel;
        this.maxMemoryUsageBytes = builder.maxMemoryUsageBytes;
        this.maxLag = builder.maxLag;
        this.maxInflightCount = builder.maxInflightCount;
        this.batchFlushInterval = builder.batchFlushInterval;
        this.batchFlushSiseBytes = builder.batchFlushSiseBytes;
        this.compressionExecutor = builder.compressionExecutor;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getTopicPath() {
        return topicPath;
    }

    public String getProducerId() {
        return producerId;
    }

    public String getMessageGroupId() {
        return messageGroupId;
    }

    public Long getPartitionId() {
        return partitionId;
    }

    public Codec getCodec() {
        return codec;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public long getMaxMemoryUsageBytes() {
        return maxMemoryUsageBytes;
    }

    public Duration getMaxLag() {
        return maxLag;
    }

    public int getMaxInflightCount() {
        return maxInflightCount;
    }

    public Duration getBatchFlushInterval() {
        return batchFlushInterval;
    }

    public long getBatchFlushSiseBytes() {
        return batchFlushSiseBytes;
    }

    public Executor getCompressionExecutor() {
        return compressionExecutor;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private String topicPath = null;
        private String producerId = null;
        private String messageGroupId = null;
        private Long partitionId = null;
        private Codec codec = Codec.GZIP;
        private int compressionLevel = 4;
        private long maxMemoryUsageBytes = MAX_MEMORY_USAGE_BYTES_DEFAULT;
        private Duration maxLag;
        private int maxInflightCount = MAX_INFLIGHT_COUNT_DEFAULT;
        private Duration batchFlushInterval;
        private long batchFlushSiseBytes;
        private Executor compressionExecutor = MoreExecutors.directExecutor();

        public Builder setTopicPath(String topicPath) {
            this.topicPath = topicPath;
            return this;
        }

        public Builder setProducerId(String producerId) {
            this.producerId = producerId;
            return this;
        }

        public Builder setMessageGroupId(String messageGroupId) {
            this.messageGroupId = messageGroupId;
            return this;
        }

        public Builder setPartitionId(long partitionId) {
            this.partitionId = partitionId;
            return this;
        }

        public Builder setCodec(Codec codec) {
            this.codec = codec;
            return this;
        }

        public Builder setCompressionLevel(int compressionLevel) {
            this.compressionLevel = compressionLevel;
            return this;
        }

        public Builder setMaxMemoryUsageBytes(long maxMemoryUsageBytes) {
            this.maxMemoryUsageBytes = maxMemoryUsageBytes;
            return this;
        }

        public Builder setMaxLag(Duration maxLag) {
            this.maxLag = maxLag;
            return this;
        }

        public Builder setMaxInflightCount(int maxInflightCount) {
            this.maxInflightCount = maxInflightCount;
            return this;
        }

        public Builder setBatchFlushInterval(Duration batchFlushInterval) {
            this.batchFlushInterval = batchFlushInterval;
            return this;
        }

        public Builder setBatchFlushSiseBytes(long batchFlushSiseBytes) {
            this.batchFlushSiseBytes = batchFlushSiseBytes;
            return this;
        }

        public Builder setCompressionExecutor(Executor compressionExecutor) {
            this.compressionExecutor = compressionExecutor;
            return this;
        }

        public WriterSettings build() {
            return new WriterSettings(this);
        }

    }
}
