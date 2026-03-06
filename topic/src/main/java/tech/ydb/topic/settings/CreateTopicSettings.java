package tech.ydb.topic.settings;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.MeteringMode;
import tech.ydb.topic.description.SupportedCodecs;


/**
 * @author Nikolay Perfilov
 */
public class CreateTopicSettings extends TopicClientOperationSettings {
    @Nullable
    private final PartitioningSettings partitioningSettings;
    @Nullable
    private final Duration retentionPeriod;
    private final long retentionStorageMb;
    @Nullable
    private final SupportedCodecs supportedCodecs;
    private final long partitionWriteSpeedBytesPerSecond;
    private final long partitionWriteBurstBytes;
    private final Map<String, String> attributes;
    private final List<Consumer> consumers;
    private final MeteringMode meteringMode;

    private CreateTopicSettings(Builder builder) {
        super(builder);
        this.partitioningSettings = builder.partitioningSettings;
        this.retentionPeriod = builder.retentionPeriod;
        this.retentionStorageMb = builder.retentionStorageMb;
        this.supportedCodecs = builder.supportedCodecs;
        this.partitionWriteSpeedBytesPerSecond = builder.partitionWriteSpeedBytesPerSecond;
        this.partitionWriteBurstBytes = builder.partitionWriteBurstBytes;
        this.attributes = ImmutableMap.copyOf(builder.attributes);
        this.consumers = ImmutableList.copyOf(builder.consumers);
        this.meteringMode = builder.meteringMode;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Nullable
    public PartitioningSettings getPartitioningSettings() {
        return partitioningSettings;
    }

    @Nullable
    public Duration getRetentionPeriod() {
        return retentionPeriod;
    }

    public long getRetentionStorageMb() {
        return retentionStorageMb;
    }

    @Nullable
    public SupportedCodecs getSupportedCodecs() {
        return supportedCodecs;
    }

    public long getPartitionWriteSpeedBytesPerSecond() {
        return partitionWriteSpeedBytesPerSecond;
    }

    public long getPartitionWriteBurstBytes() {
        return partitionWriteBurstBytes;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<Consumer> getConsumers() {
        return consumers;
    }

    public MeteringMode getMeteringMode() {
        return meteringMode;
    }

    /**
     * BUILDER
     */
    public static class Builder extends TopicClientOperationBuilder<Builder> {
        private PartitioningSettings partitioningSettings = null;
        private Duration retentionPeriod = null;
        private long retentionStorageMb = 0;
        private SupportedCodecs supportedCodecs = null;
        private long partitionWriteSpeedBytesPerSecond = 0;
        private long partitionWriteBurstBytes = 0;
        private Map<String, String> attributes = new HashMap<>();
        private List<Consumer> consumers = new ArrayList<>();
        private MeteringMode meteringMode = MeteringMode.UNSPECIFIED;

        public Builder setPartitioningSettings(@Nonnull PartitioningSettings partitioningSettings) {
            this.partitioningSettings = partitioningSettings;
            return this;
        }

        public Builder setRetentionPeriod(Duration retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
            return this;
        }

        public Builder setRetentionStorageMb(long retentionStorageMb) {
            this.retentionStorageMb = retentionStorageMb;
            return this;
        }

        public Builder setSupportedCodecs(SupportedCodecs supportedCodecs) {
            this.supportedCodecs = supportedCodecs;
            return this;
        }

        public Builder setPartitionWriteSpeedBytesPerSecond(long partitionWriteSpeedBytesPerSecond) {
            this.partitionWriteSpeedBytesPerSecond = partitionWriteSpeedBytesPerSecond;
            return this;
        }

        public Builder setPartitionWriteBurstBytes(long partitionWriteBurstBytes) {
            this.partitionWriteBurstBytes = partitionWriteBurstBytes;
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

        public Builder addConsumer(Consumer consumer) {
            consumers.add(consumer);
            return this;
        }

        public Builder setConsumers(List<Consumer> consumers) {
            this.consumers = consumers;
            return this;
        }

        public Builder setMeteringMode(MeteringMode meteringMode) {
            this.meteringMode = meteringMode;
            return this;
        }

        @Override
        public CreateTopicSettings build() {
            return new CreateTopicSettings(this);
        }
    }
}
