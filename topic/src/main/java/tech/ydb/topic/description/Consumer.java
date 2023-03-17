package tech.ydb.topic.description;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

/**
 * @author Nikolay Perfilov
 */
public class Consumer {
    private final String name;
    @Nullable
    private final Boolean important;
    @Nullable
    private final Instant readFrom;
    @Nullable
    private final SupportedCodecs supportedCodecs;
    private final Map<String, String> attributes;
    @Nullable
    private final ConsumerStats stats;

    private Consumer(Builder builder) {
        this.name = builder.name;
        this.important = builder.important;
        this.readFrom = builder.readFrom;
        this.supportedCodecs = builder.supportedCodecs;
        this.attributes = ImmutableMap.copyOf(builder.attributes);
        this.stats = builder.stats;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    @Nullable
    public Boolean isImportant() {
        return important;
    }

    @Nullable
    public Instant getReadFrom() {
        return readFrom;
    }

    @Nullable
    public SupportedCodecs getSupportedCodecs() {
        return supportedCodecs;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public ConsumerStats getStats() {
        return stats;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private String name;
        private Boolean important = null;
        private Instant readFrom = null;
        private SupportedCodecs supportedCodecs = null;
        private Map<String, String> attributes = new HashMap<>();
        private ConsumerStats stats = null;

        public Builder setName(@Nonnull String name) {
            this.name = name;
            return this;
        }

        public Builder setImportant(boolean important) {
            this.important = important;
            return this;
        }

        public Builder setReadFrom(Instant readFrom) {
            this.readFrom = readFrom;
            return this;
        }

        public Builder setSupportedCodecs(SupportedCodecs supportedCodecs) {
            this.supportedCodecs = supportedCodecs;
            return this;
        }

        public Builder addAttribute(@Nonnull String key, String value) {
            attributes.put(key, value);
            return this;
        }

        public Builder setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder setStats(ConsumerStats stats) {
            this.stats = stats;
            return this;
        }

        public Consumer build() {
            if (name == null) {
                throw new IllegalArgumentException("Consumer name is not set");
            }
            return new Consumer(this);
        }
    }
}
