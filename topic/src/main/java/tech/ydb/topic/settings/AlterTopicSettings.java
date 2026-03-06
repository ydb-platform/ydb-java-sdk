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
public class AlterTopicSettings extends TopicClientOperationSettings {
    @Nullable
    private final AlterPartitioningSettings alterPartitioningSettings;
    @Nullable
    private final Duration retentionPeriod;
    @Nullable
    private final Long retentionStorageMb;
    @Nullable
    private final SupportedCodecs supportedCodecs;
    @Nullable
    private final Long partitionWriteSpeedBytesPerSecond;
    @Nullable
    private final Long partitionWriteBurstBytes;
    private final Map<String, String> alterAttributes;
    private final List<Consumer> addConsumers;
    private final List<String> dropConsumers;
    private final List<AlterConsumerSettings> alterConsumers;
    @Nullable
    private final MeteringMode meteringMode;

    private AlterTopicSettings(Builder builder) {
        super(builder);
        this.alterPartitioningSettings = builder.alterPartitioningSettings;
        this.retentionPeriod = builder.retentionPeriod;
        this.retentionStorageMb = builder.retentionStorageMb;
        this.supportedCodecs = builder.supportedCodecs;
        this.partitionWriteSpeedBytesPerSecond = builder.partitionWriteSpeedBytesPerSecond;
        this.partitionWriteBurstBytes = builder.partitionWriteBurstBytes;
        this.alterAttributes = ImmutableMap.copyOf(builder.alterAttributes);
        this.addConsumers = ImmutableList.copyOf(builder.addConsumers);
        this.dropConsumers = ImmutableList.copyOf(builder.dropConsumers);
        this.alterConsumers = ImmutableList.copyOf(builder.alterConsumers);
        this.meteringMode = builder.meteringMode;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Nullable
    public AlterPartitioningSettings getAlterPartitioningSettings() {
        return alterPartitioningSettings;
    }

    @Nullable
    public Duration getRetentionPeriod() {
        return retentionPeriod;
    }

    @Nullable
    public Long getRetentionStorageMb() {
        return retentionStorageMb;
    }

    @Nullable
    public SupportedCodecs getSupportedCodecs() {
        return supportedCodecs;
    }

    @Nullable
    public Long getPartitionWriteSpeedBytesPerSecond() {
        return partitionWriteSpeedBytesPerSecond;
    }

    @Nullable
    public Long getPartitionWriteBurstBytes() {
        return partitionWriteBurstBytes;
    }

    public Map<String, String> getAlterAttributes() {
        return alterAttributes;
    }

    public List<Consumer> getAddConsumers() {
        return addConsumers;
    }

    public List<String> getDropConsumers() {
        return dropConsumers;
    }

    public List<AlterConsumerSettings> getAlterConsumers() {
        return alterConsumers;
    }

    @Nullable
    public MeteringMode getMeteringMode() {
        return meteringMode;
    }

    /**
     * BUILDER
     */
    public static class Builder extends TopicClientOperationBuilder<Builder> {
        private AlterPartitioningSettings alterPartitioningSettings = null;
        private Duration retentionPeriod = null;
        private Long retentionStorageMb = null;
        private SupportedCodecs supportedCodecs = null;
        private Long partitionWriteSpeedBytesPerSecond = null;
        private Long partitionWriteBurstBytes = null;
        private Map<String, String> alterAttributes = new HashMap<>();
        private List<Consumer> addConsumers = new ArrayList<>();
        private List<String> dropConsumers = new ArrayList<>();
        private List<AlterConsumerSettings> alterConsumers = new ArrayList<>();
        private MeteringMode meteringMode = null;


        public Builder setAlterPartitioningSettings(AlterPartitioningSettings alterPartitioningSettings) {
            this.alterPartitioningSettings = alterPartitioningSettings;
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

        public Builder addAlterAttribute(@Nonnull String name, @Nullable String value) {
            alterAttributes.put(name, value);
            return this;
        }

        public Builder setAlterAttributes(Map<String, String> attributes) {
            this.alterAttributes = attributes;
            return this;
        }

        public Builder addAddConsumer(Consumer consumer) {
            addConsumers.add(consumer);
            return this;
        }

        public Builder setAddConsumers(List<Consumer> consumers) {
            addConsumers = consumers;
            return this;
        }

        public Builder addDropConsumer(@Nonnull String consumerName) {
            dropConsumers.add(consumerName);
            return this;
        }

        public Builder setDropConsumers(List<String> consumerNames) {
            dropConsumers = consumerNames;
            return this;
        }

        public Builder addAlterConsumer(AlterConsumerSettings consumer) {
            alterConsumers.add(consumer);
            return this;
        }

        public Builder setAlterConsumers(List<AlterConsumerSettings> consumers) {
            alterConsumers = consumers;
            return this;
        }

        public Builder setMeteringMode(MeteringMode meteringMode) {
            this.meteringMode = meteringMode;
            return this;
        }

        @Override
        public AlterTopicSettings build() {
            return new AlterTopicSettings(this);
        }
    }
}
