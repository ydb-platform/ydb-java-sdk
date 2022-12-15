package tech.ydb.topic.settings;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * @author Nikolay Perfilov
 */
public class TopicReadSettings {
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private final String path;
    private final List<Long> partitionIds;
    private final Duration maxLag;
    private final Instant readFrom;
    private final Duration connectTimeout;

    private TopicReadSettings(Builder builder) {
        this.path = builder.path;
        this.partitionIds =  ImmutableList.copyOf(builder.partitionIds);
        this.maxLag = builder.maxLag;
        this.readFrom = builder.readFrom;
        this.connectTimeout = builder.connectTimeout;
    }

    public String getPath() {
        return path;
    }

    public List<Long> getPartitionIds() {
        return partitionIds;
    }

    public Duration getMaxLag() {
        return maxLag;
    }

    public Instant getReadFrom() {
        return readFrom;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private String path;
        private List<Long> partitionIds;
        private Duration maxLag;
        private Instant readFrom;
        private Duration connectTimeout;

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setPartitionIds(List<Long> partitionIds) {
            this.partitionIds = partitionIds;
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

        public Builder setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public TopicReadSettings build() {
            return new TopicReadSettings(this);
        }

    }
}
