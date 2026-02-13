package tech.ydb.topic.description;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.utils.ProtoUtils;

/**
 * @author Nikolay Perfilov
 */
public class Consumer {

    private final String name;
    private final boolean important;
    private final Instant readFrom;
    private final List<Codec> supportedCodecs;
    private final Map<String, String> attributes;
    private final ConsumerStats stats;
    private final Duration availabilityPeriod;

    private Consumer(Builder builder) {
        this.name = builder.name;
        this.important = builder.important;
        this.readFrom = builder.readFrom;
        this.supportedCodecs = builder.supportedCodecs;
        this.attributes = ImmutableMap.copyOf(builder.attributes);
        this.stats = builder.stats;
        this.availabilityPeriod = builder.availabilityPeriod;
    }

    public Consumer(YdbTopic.Consumer consumer) {
        this.name = consumer.getName();
        this.important = consumer.getImportant();
        this.readFrom = ProtobufUtils.protoToInstant(consumer.getReadFrom());
        this.supportedCodecs = consumer.getSupportedCodecs().getCodecsList()
                .stream().map(ProtoUtils::codecFromProto).collect(Collectors.toList());
        this.attributes = consumer.getAttributesMap();
        this.stats = new ConsumerStats(consumer.getConsumerStats());
        this.availabilityPeriod = ProtobufUtils.protoToDuration(consumer.getAvailabilityPeriod());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    /**
     * Consumer may be marked as 'important'. It means messages for this consumer will never expire due to retention.
     * User should take care that such consumer never stalls, to prevent running out of disk space.
     *
     * @return Flag that this consumer is important.
     */
    public boolean isImportant() {
        return important;
    }

    @Nullable
    public Instant getReadFrom() {
        return readFrom;
    }

    @Nullable
    public SupportedCodecs getSupportedCodecs() {
        return new SupportedCodecs(supportedCodecs);
    }

    public List<Codec> getSupportedCodecsList() {
        return supportedCodecs;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Nullable
    public ConsumerStats getStats() {
        return stats;

    }

    /**
     * Message for this consumer will not expire due to retention for at least <code>availabilityPeriod</code> if they
     * aren't committed.
     *
     * @return availability period for this consumer
     */
    public Duration getAvailabilityPeriod() {
        return availabilityPeriod;
    }

    public static class Builder {

        private String name;
        private boolean important = false;
        private Instant readFrom = null;
        private final List<Codec> supportedCodecs = new ArrayList<>();
        private Map<String, String> attributes = new HashMap<>();
        private ConsumerStats stats = null;
        private Duration availabilityPeriod = null;

        public Builder setName(@Nonnull String name) {
            this.name = name;
            return this;
        }

        /**
         * Configure the importance for this consumer.
         * <br>
         * An important consumer cannot have <code>availabilityPeriod</code> option
         *
         * @see Consumer#isImportant()
         * @param important - this consumer importance flag
         * @return this consumer builder
         */
        public Builder setImportant(boolean important) {
            this.important = important;
            return this;
        }

        public Builder setReadFrom(Instant readFrom) {
            this.readFrom = readFrom;
            return this;
        }

        /**
         * Configure <code>availabilityPeriod</code> for this consumer.
         * <br>
         * Option <code>availabilityPeriod</code> is not compatible with <code>important</code> option
         *
         * @see Consumer#getAvailabilityPeriod()
         * @param period - availability period value
         * @return this consumer builder
         */
        public Builder setAvailabilityPeriod(Duration period) {
            this.availabilityPeriod = period;
            return this;
        }

        public Builder addSupportedCodec(Codec codec) {
            this.supportedCodecs.add(codec);
            return this;
        }

        public Builder setSupportedCodecs(SupportedCodecs supportedCodecs) {
            this.supportedCodecs.clear();
            this.supportedCodecs.addAll(supportedCodecs.getCodecs());
            return this;
        }

        public Builder addAttribute(@Nonnull String name, String value) {
            attributes.put(name, value);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Consumer consumer = (Consumer) o;
        return important == consumer.important
                && Objects.equals(name, consumer.name)
                && Objects.equals(readFrom, consumer.readFrom)
                && Objects.equals(supportedCodecs, consumer.supportedCodecs)
                && Objects.equals(attributes, consumer.attributes)
                && Objects.equals(stats, consumer.stats)
                && Objects.equals(availabilityPeriod, consumer.availabilityPeriod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, important, readFrom, supportedCodecs, attributes, stats, availabilityPeriod);
    }
}
