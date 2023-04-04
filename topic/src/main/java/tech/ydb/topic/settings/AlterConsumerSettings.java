package tech.ydb.topic.settings;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private final Map<String, String> alterAttributes;
    private final Set<String> dropAttributes;

    private AlterConsumerSettings(Builder builder) {
        this.name = builder.name;
        this.important = builder.important;
        this.readFrom = builder.readFrom;
        this.supportedCodecs = builder.supportedCodecs;
        this.alterAttributes = builder.alterAttributes;
        this.dropAttributes = builder.dropAttributes;
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

    public Map<String, String> getAlterAttributes() {
        return alterAttributes;
    }

    public Set<String> getDropAttributes() {
        return dropAttributes;
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
        private Set<String> dropAttributes = new HashSet<>();

        /**
         * @param name  Consumer name
         * @return settings builder
         */
        public Builder setName(@Nonnull String name) {
            this.name = name;
            return this;
        }

        /**
         * @param important  Flag that this consumer is important. Consumer may be marked as 'important'.
         *                   It means messages for this consumer will never expire due to retention. User should take
         *                   care that such consumer never stalls, to prevent running out of disk space.
         * @return settings builder
         */
        public Builder setImportant(boolean important) {
            this.important = important;
            return this;
        }

        /**
         * @param readFrom  Time to read from. All messages with smaller server written_at timestamp will be skipped.
         * @return settings builder
         */
        public Builder setReadFrom(Instant readFrom) {
            this.readFrom = readFrom;
            return this;
        }

        /**
         * @param supportedCodecs  Codecs supported by this consumer.
         *                         Should contain all codecs, supported by its Topic
         * @return settings builder
         */
        public Builder setSupportedCodecs(SupportedCodecs supportedCodecs) {
            this.supportedCodecs = supportedCodecs;
            return this;
        }

        /**
         * Add consumer attribute to alter.
         * @param key  Attribute name
         * @param value  Attribute value
         * @return settings builder
         */
        public Builder addAlterAttribute(@Nonnull String key, @Nonnull String value) {
            alterAttributes.put(key, value);
            return this;
        }

        /**
         * Set consumer attributes to alter.
         * @param attributes  Consumer attributes to alter.
         * @return settings builder
         */
        public Builder setAlterAttributes(Map<String, String> attributes) {
            alterAttributes = attributes;
            return this;
        }

        /**
         * Add consumer attribute to drop.
         * @param key  Attribute name
         * @return settings builder
         */
        public Builder addDropAttribute(@Nonnull String key) {
            dropAttributes.add(key);
            return this;
        }

        /**
         * Set consumer attributes to drop.
         * @param attributes  Consumer attributes
         * @return settings builder
         */
        public Builder setDropAttributes(Set<String> attributes) {
            dropAttributes = attributes;
            return this;
        }

        public AlterConsumerSettings build() {
            if (name == null) {
                throw new IllegalArgumentException("Consumer name is not set in AlterConsumerSettings");
            }
            return new AlterConsumerSettings(this);
        }
    }
}
