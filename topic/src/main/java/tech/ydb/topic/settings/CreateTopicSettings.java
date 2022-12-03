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

import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.MeteringMode;


/**
 * @author Nikolay Perfilov
 */
public class CreateTopicSettings /* TODO: extends RequestSettings<CreateTopicSettings>*/ {
    @Nullable
    private final PartitioningSettings partitioningSettings;
    private final Duration retentionPeriod;
    private final long retentionStorageMb;
    private final List<Codec> supportedCodecs;
    private final long partitionWriteSpeedBytesPerSecond;
    private final long partitionWriteBurstBytes;
    private final Map<String, String> attributes;
    private final List<Consumer> consumers;
    private final MeteringMode meteringMode;

    private CreateTopicSettings(Builder builder) {
        this.partitioningSettings = builder.partitioningSettings;
        this.retentionPeriod = builder.retentionPeriod;
        this.retentionStorageMb = builder.retentionStorageMb;
        this.supportedCodecs = ImmutableList.copyOf(builder.supportedCodecs);
        this.partitionWriteSpeedBytesPerSecond = builder.partitionWriteSpeedBytesPerSecond;
        this.partitionWriteBurstBytes = builder.partitionWriteBurstBytes;
        this.attributes = ImmutableMap.copyOf(builder.attributes);
        this.consumers = builder.consumers;
        this.meteringMode = builder.meteringMode;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Nullable
    public PartitioningSettings getPartitioningSettings() {
        return partitioningSettings;
    }

    public Duration getRetentionPeriod() {
        return retentionPeriod;
    }

    public long getRetentionStorageMb() {
        return retentionStorageMb;
    }

    public List<Codec> getSupportedCodecs() {
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
    public static class Builder {
        @Nullable
        private PartitioningSettings partitioningSettings;
        private Duration retentionPeriod = Duration.ZERO;
        private long retentionStorageMb = 0;
        private List<Codec> supportedCodecs = new ArrayList<>();
        private long partitionWriteSpeedBytesPerSecond = 0;
        private long partitionWriteBurstBytes = 0;
        private Map<String, String> attributes = new HashMap<>();
        private List<Consumer> consumers = new ArrayList<>();
        private MeteringMode meteringMode = MeteringMode.UNSPECIFIED;

        public Builder setPartitioningSettings(@Nonnull PartitioningSettings partitioningSettings) {
            this.partitioningSettings = partitioningSettings;
            return this;
        }

        public Builder setPartitioningSettings(int minActivePartitions, int partitionCountLimit) {
            this.partitioningSettings = new PartitioningSettings(minActivePartitions, partitionCountLimit);
            return this;
        }

        public Builder setRetentionPeriod(@Nonnull Duration retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
            return this;
        }

        public Builder setRetentionStorageMb(long retentionStorageMb) {
            this.retentionStorageMb = retentionStorageMb;
            return this;
        }

        public Builder addSupportedCodec(Codec codec) {
            supportedCodecs.add(codec);
            return this;
        }

        public Builder setSupportedCodecs(List<Codec> supportedCodecs) {
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

        public Builder addAttribute(@Nonnull String key, String value) {
            attributes.put(key, value);
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

        public CreateTopicSettings build() {
            return new CreateTopicSettings(this);
        }
    }
}
