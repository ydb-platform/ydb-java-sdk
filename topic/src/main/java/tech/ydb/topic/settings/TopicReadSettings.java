package tech.ydb.topic.settings;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.annotation.Nullable;

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
        this.partitionIds = builder.partitionIds;
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

    @Nullable
    public Duration getMaxLag() {
        return maxLag;
    }

    @Nullable
    public Instant getReadFrom() {
        return readFrom;
    }

    @Nullable
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
        private String path = null;
        private List<Long> partitionIds = null;
        private Duration maxLag = null;
        private Instant readFrom = null;
        private Duration connectTimeout = null;

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
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("Missing path for topic read settings");
            }
            return new TopicReadSettings(this);
        }

    }
}
