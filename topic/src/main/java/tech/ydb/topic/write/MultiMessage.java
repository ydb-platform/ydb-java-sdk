package tech.ydb.topic.write;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import tech.ydb.topic.description.MetadataItem;

/**
 * Immutable input for a multiwriter. Payload and metadata are copied on construction and access.
 * Keys are UTF-8 strings. An empty key uses the producer prefix for key-based routing.
 * A key controls routing, not business-operation deduplication.
 */
public final class MultiMessage {
    private final byte[] data;
    private final String key;
    private final Long partitionId;
    private final Long seqNo;
    private final Instant createTimestamp;
    private final List<MetadataItem> metadataItems;

    private MultiMessage(Builder builder) {
        this.data = Objects.requireNonNull(builder.data, "data").clone();
        this.key = Objects.requireNonNull(builder.key, "key");
        this.partitionId = builder.partitionId;
        this.seqNo = builder.seqNo;
        if (partitionId != null && (partitionId < 0 || !key.isEmpty())) {
            throw new IllegalArgumentException("Explicit partition requires a nonnegative ID and an empty key");
        }
        if (seqNo != null && seqNo <= 0) {
            throw new IllegalArgumentException("Explicit sequence number must be positive");
        }
        this.createTimestamp = builder.createTimestamp != null ? builder.createTimestamp : Instant.now();
        this.metadataItems = copyMetadata(builder.metadataItems);
    }

    public static MultiMessage of(byte[] data, String key) {
        return newBuilder().setData(data).setKey(key).build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public byte[] getData() {
        return data.clone();
    }

    public String getKey() {
        return key;
    }

    /** @return explicit partition ID or null for key-based routing */
    public Long getPartitionId() {
        return partitionId;
    }

    /** @return explicit strictly increasing number or null for automatic numbering */
    public Long getSeqNo() {
        return seqNo;
    }

    public Instant getCreateTimestamp() {
        return createTimestamp;
    }

    public List<MetadataItem> getMetadataItems() {
        return copyMetadata(metadataItems);
    }

    private static List<MetadataItem> copyMetadata(List<MetadataItem> source) {
        List<MetadataItem> copy = new ArrayList<>(source.size());
        for (MetadataItem item : source) {
            Objects.requireNonNull(item, "metadata item");
            String name = Objects.requireNonNull(item.getKey(), "metadata key");
            if ("__partition_key".equals(name)) {
                throw new IllegalArgumentException("Metadata __partition_key is reserved for multiwriter routing");
            }
            copy.add(new MetadataItem(name, Objects.requireNonNull(item.getValue(), "metadata value").clone()));
        }
        return Collections.unmodifiableList(copy);
    }

    public static final class Builder {
        private byte[] data;
        private String key = "";
        private Long partitionId;
        private Long seqNo;
        private Instant createTimestamp;
        private List<MetadataItem> metadataItems = new ArrayList<>();

        public Builder setData(byte[] data) {
            this.data = data;
            return this;
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setPartitionId(long partitionId) {
            this.partitionId = partitionId;
            return this;
        }

        public Builder setSeqNo(long seqNo) {
            this.seqNo = seqNo;
            return this;
        }

        public Builder setCreateTimestamp(Instant createTimestamp) {
            this.createTimestamp = createTimestamp;
            return this;
        }

        public Builder addMetadataItem(MetadataItem metadataItem) {
            this.metadataItems.add(metadataItem);
            return this;
        }

        public Builder setMetadataItems(List<MetadataItem> metadataItems) {
            this.metadataItems = new ArrayList<>(Objects.requireNonNull(metadataItems, "metadataItems"));
            return this;
        }

        public MultiMessage build() {
            return new MultiMessage(this);
        }
    }
}
