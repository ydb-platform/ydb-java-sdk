package tech.ydb.topic.settings;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import tech.ydb.topic.description.SupportedCodecs;

/**
 * @author Nikolay Perfilov
 */
public class AlterConsumerSettings {
    private final String name;
    @Nullable
    private final Boolean important;
    @Nullable
    private final Instant readFrom;
    @Nullable
    private final SupportedCodecs supportedCodecs;
    @Nullable
    private final Map<String, String> alterAttributes;

    private AlterConsumerSettings(Builder builder) {
        this.name = builder.name;
        this.important = builder.important;
        this.readFrom = builder.readFrom;
        this.supportedCodecs = builder.supportedCodecs;
        this.alterAttributes = builder.alterAttributes;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    @Nullable
    public Boolean getImportant() {
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

    @Nullable
    public Map<String, String> getAlterAttributes() {
        return alterAttributes;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private String name;
        private Boolean important = null;
        private Instant readFrom = null;
        private SupportedCodecs supportedCodecs = null;
        private Map<String, String> alterAttributes = new HashMap<>();

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

        public Builder addAlterAttribute(@Nonnull String key, @Nullable String value) {
            alterAttributes.put(key, value);
            return this;
        }

        public Builder setAlterAttributes(Map<String, String> attributes) {
            alterAttributes = attributes;
            return this;
        }

        public AlterConsumerSettings build() {
            if (name == null) {
                throw new IllegalArgumentException("Consumer name is not set in AlterConsumerSetings");
            }
            return new AlterConsumerSettings(this);
        }
    }
}
