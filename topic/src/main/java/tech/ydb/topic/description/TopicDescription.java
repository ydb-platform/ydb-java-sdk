package tech.ydb.topic.description;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import tech.ydb.topic.settings.PartitioningSettings;

/**
 * @author Nikolay Perfilov
 */
public class TopicDescription {

    private final PartitioningSettings partitioningSettings;
    private final List<PartitionInfo> partitions;
    @Nullable
    private final Duration retentionPeriod;
    private final long retentionStorageMb;
    private final SupportedCodecs supportedCodecs;
    private final long partitionWriteSpeedBytesPerSecond;
    private final long partitionWriteBurstBytes;
    private final Map<String, String> attributes;
    private final List<Consumer> consumers;
    private final MeteringMode meteringMode;
    private final TopicStats topicStats;

    private TopicDescription(Builder builder) {
        this.partitioningSettings = builder.partitioningSettings;
        this.partitions = ImmutableList.copyOf(builder.partitions);
        this.retentionPeriod = builder.retentionPeriod;
        this.retentionStorageMb = builder.retentionStorageMb;
        this.supportedCodecs = builder.supportedCodecs;
        this.partitionWriteSpeedBytesPerSecond = builder.partitionWriteSpeedBytesPerSecond;
        this.partitionWriteBurstBytes = builder.partitionWriteBurstBytes;
        this.attributes = ImmutableMap.copyOf(builder.attributes);
        this.consumers = ImmutableList.copyOf(builder.consumers);
        this.meteringMode = builder.meteringMode;
        this.topicStats = builder.topicStats;
    }

    public PartitioningSettings getPartitioningSettings() {
        return partitioningSettings;
    }

    public List<PartitionInfo> getPartitions() {
        return partitions;
    }

    @Nullable
    public Duration getRetentionPeriod() {
        return retentionPeriod;
    }

    public long getRetentionStorageMb() {
        return retentionStorageMb;
    }

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

    public TopicStats getTopicStats() {
        return topicStats;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private PartitioningSettings partitioningSettings;
        private List<PartitionInfo> partitions = new ArrayList<>();
        private Duration retentionPeriod = null;
        private long retentionStorageMb;
        private SupportedCodecs supportedCodecs;
        private long partitionWriteSpeedBytesPerSecond;
        private long partitionWriteBurstBytes;
        private Map<String, String> attributes;
        private List<Consumer> consumers;
        private MeteringMode meteringMode;
        private TopicStats topicStats;

        public Builder setPartitioningSettings(PartitioningSettings partitioningSettings) {
            this.partitioningSettings = partitioningSettings;
            return this;
        }

        public Builder setPartitions(List<PartitionInfo> partitions) {
            this.partitions = partitions;
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

        public Builder setTopicStats(TopicStats topicStats) {
            this.topicStats = topicStats;
            return this;
        }

        public TopicDescription build() {
            return new TopicDescription(this);
        }
    }

}
