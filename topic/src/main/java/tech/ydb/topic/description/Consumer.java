package tech.ydb.topic.description;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final List<TopicCodec> supportedCodecs;
    private final Map<String, String> attributes;
    private final ConsumerStats stats;

    private Consumer(Builder builder) {
        this.name = builder.name;
        this.important = builder.important;
        this.readFrom = builder.readFrom;
        this.supportedCodecs = builder.supportedCodecs;
        this.attributes = ImmutableMap.copyOf(builder.attributes);
        this.stats = builder.stats;
    }

    public Consumer(YdbTopic.Consumer consumer) {
        this.name = consumer.getName();
        this.important = consumer.getImportant();
        this.readFrom = ProtobufUtils.protoToInstant(consumer.getReadFrom());
        this.supportedCodecs = consumer.getSupportedCodecs().getCodecsList()
                .stream().map(ProtoUtils::codecFromProto).collect(Collectors.toList());
        this.attributes = consumer.getAttributesMap();
        this.stats = new ConsumerStats(consumer.getConsumerStats());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

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

    public List<TopicCodec> getSupportedCodecsList() {
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
     * BUILDER
     */
    public static class Builder {
        private String name;
        private boolean important = false;
        private Instant readFrom = null;
        private List<TopicCodec> supportedCodecs = new ArrayList<>();
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

        public Builder addSupportedCodec(TopicCodec codec) {
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
}
