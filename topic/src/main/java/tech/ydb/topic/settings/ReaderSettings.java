package tech.ydb.topic.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import tech.ydb.common.retry.RetryConfig;

/**
 * @author Nikolay Perfilov
 */
public class ReaderSettings {
    private static final long MAX_MEMORY_USAGE_BYTES_DEFAULT = 100 * 1024 * 1024; // 100 MB

    private final String consumerName;
    private final String readerName;
    private final List<TopicReadSettings> topics;
    private final RetryConfig retryConfig;
    private final long maxMemoryUsageBytes;
    private final Executor decompressionExecutor;

    private ReaderSettings(Builder builder) {
        this.consumerName = builder.consumerName;
        this.readerName = builder.readerName;
        this.topics = ImmutableList.copyOf(builder.topics);
        this.retryConfig = builder.retryConfig;
        this.maxMemoryUsageBytes = builder.maxMemoryUsageBytes;
        this.decompressionExecutor = builder.decompressionExecutor;
    }

    public String getConsumerName() {
        return consumerName;
    }

    @Nullable
    public String getReaderName() {
        return readerName;
    }

    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    public List<TopicReadSettings> getTopics() {
        return topics;
    }

    public long getMaxMemoryUsageBytes() {
        return maxMemoryUsageBytes;
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
        private boolean readWithoutConsumer = false;
        private String readerName = null;
        private List<TopicReadSettings> topics = new ArrayList<>();
        private RetryConfig retryConfig = RetryConfig.idempotentRetryForever();
        private long maxMemoryUsageBytes = MAX_MEMORY_USAGE_BYTES_DEFAULT;
        private Executor decompressionExecutor = null;

        public Builder setConsumerName(String consumerName) {
            this.consumerName = consumerName;
            return this;
        }

        /**
         * Experimental feature. Interface may change in future
         * Explicitly require reading without a consumer. Reading progress will not be saved on server this way.
         * @return settings builder
         */
        public Builder withoutConsumer() {
            this.readWithoutConsumer = true;
            return this;
        }

        /**
         * Set reader name for debug purposes
         * @param readerName name of reader
         * @return settings builder
         */
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

        /**
         * Set {@link RetryConfig} to define behavior of the stream internal retries
         * @param config retry mode
         * @return settings builder
         */
        public Builder setRetryConfig(RetryConfig config) {
            this.retryConfig = config;
            return this;
        }

        public Builder setMaxMemoryUsageBytes(long maxMemoryUsageBytes) {
            this.maxMemoryUsageBytes = maxMemoryUsageBytes;
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
                if (!readWithoutConsumer) {
                    throw new IllegalArgumentException("Missing consumer name for read settings. " +
                            "Use withoutConsumer option explicitly if you want to read without a consumer");
                }
            } else {
                if (readWithoutConsumer) {
                    throw new IllegalArgumentException("Both mutually exclusive options consumerName and " +
                            "withoutConsumer are set for read settings");
                }
            }
            if (topics.isEmpty()) {
                throw new IllegalArgumentException("Missing topics for read settings. At least one should be set");
            }
            return new ReaderSettings(this);
        }
    }
}
