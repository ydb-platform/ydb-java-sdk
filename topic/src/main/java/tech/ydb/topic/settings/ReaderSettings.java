package tech.ydb.topic.settings;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

/**
 * @author Nikolay Perfilov
 */
public class ReaderSettings {
    private static final long MAX_MEMORY_USAGE_BYTES_DEFAULT = 100 * 1024 * 1024; // 100 MB

    private final String consumerName;
    private final String readerName;
    private final List<TopicReadSettings> topics;
    private final long maxMemoryUsageBytes;
    private final Duration maxLag;
    private final Instant readFrom;
    private final boolean decompress;
    private final Executor decompressionExecutor;

    private ReaderSettings(Builder builder) {
        this.consumerName = builder.consumerName;
        this.readerName = builder.readerName;
        this.topics = ImmutableList.copyOf(builder.topics);
        this.maxMemoryUsageBytes = builder.maxMemoryUsageBytes;
        this.maxLag = builder.maxLag;
        this.readFrom = builder.readFrom;
        this.decompress = builder.decompress;
        this.decompressionExecutor = builder.decompressionExecutor;
    }

    public String getConsumerName() {
        return consumerName;
    }

    @Nullable
    public String getReaderName() {
        return readerName;
    }

    public List<TopicReadSettings> getTopics() {
        return topics;
    }

    public long getMaxMemoryUsageBytes() {
        return maxMemoryUsageBytes;
    }

    public Duration getMaxLag() {
        return maxLag;
    }

    public Instant getReadFrom() {
        return readFrom;
    }

    public boolean isDecompress() {
        return decompress;
    }

    public Executor getDecompressionExecutor() {
        return decompressionExecutor;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private String consumerName = null;
        private String readerName = null;
        private List<TopicReadSettings> topics = new ArrayList<>();
        private long maxMemoryUsageBytes = MAX_MEMORY_USAGE_BYTES_DEFAULT;
        private Duration maxLag = null;
        private Instant readFrom = null;
        private boolean decompress = true;
        private Executor decompressionExecutor = null;

        public Builder setConsumerName(String consumerName) {
            this.consumerName = consumerName;
            return this;
        }

        // Not supported in API yet
        public Builder setReaderName(String readerName) {
            this.readerName = readerName;
            return this;
        }

        public Builder addTopic(TopicReadSettings topic) {
            topics.add(topic);
            return this;
        }

        public Builder setTopics(List<TopicReadSettings> topics) {
            this.topics = topics;
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

        public Builder setReadFrom(Instant readFrom) {
            this.readFrom = readFrom;
            return this;
        }

        /**
         * Set whether messages should be decompressed.
         * @param decompress  whether messages should be decompressed. true by default
         * @return settings builder
         */
        public Builder setDecompress(boolean decompress) {
            this.decompress = decompress;
            return this;
        }

        /**
         * Set executor for decompression tasks.
         * If not set, default executor will be used.
         * @param decompressionExecutor  executor for decompression tasks
         * @return settings builder
         */
        public Builder setDecompressionExecutor(Executor decompressionExecutor) {
            this.decompressionExecutor = decompressionExecutor;
            return this;
        }

        public ReaderSettings build() {
            if (consumerName == null) {
                throw new IllegalArgumentException("Missing consumer name for read settings");
            }
            if (topics.isEmpty()) {
                throw new IllegalArgumentException("Missing topics for read settings. At least one should be set");
            }
            return new ReaderSettings(this);
        }
    }
}
