package tech.ydb.topic.settings;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * @author Nikolay Perfilov
 */
public class ReaderSettings {
    private static final long MAX_MEMORY_USAGE_BYTES_DEFAULT = 100 * 1024 * 1024; // 100 MB

    private final String consumerName;
    private final List<TopicReadSettings> topics;
    private final long maxMemoryUsageBytes;
    private final Duration maxLag;
    private final Instant readFrom;
    private final ReadEventHandlersSettings handlersSettings;

    private ReaderSettings(Builder builder) {
        this.consumerName = builder.consumerName;
        this.topics = ImmutableList.copyOf(builder.topics);
        this.maxMemoryUsageBytes = builder.maxMemoryUsageBytes;
        this.maxLag = builder.maxLag;
        this.readFrom = builder.readFrom;
        this.handlersSettings = builder.handlersSettings;
    }

    public String getConsumerName() {
        return consumerName;
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

    public ReadEventHandlersSettings getHandlersSettings() {
        return handlersSettings;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private String consumerName;
        private List<TopicReadSettings> topics;
        private long maxMemoryUsageBytes = MAX_MEMORY_USAGE_BYTES_DEFAULT;
        private Duration maxLag;
        private Instant readFrom;
        private ReadEventHandlersSettings handlersSettings;

        public Builder setConsumerName(String consumerName) {
            this.consumerName = consumerName;
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

        public Builder setHandlersSettings(ReadEventHandlersSettings handlersSettings) {
            this.handlersSettings = handlersSettings;
            return this;
        }

        public ReaderSettings build() {
            return new ReaderSettings(this);
        }
    }
}
