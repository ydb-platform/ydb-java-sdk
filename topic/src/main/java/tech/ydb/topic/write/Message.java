package tech.ydb.topic.write;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import tech.ydb.common.transaction.BaseTransaction;
import tech.ydb.topic.description.MetadataItem;

/**
 * @author Nikolay Perfilov
 */
public class Message {
    private byte[] data;
    private final Long seqNo;
    private final Instant createTimestamp;
    private List<MetadataItem> metadataItems;
    private BaseTransaction transaction;

    private Message(Builder builder) {
        this.data = builder.data;
        this.seqNo = builder.seqNo;
        this.createTimestamp = builder.createTimestamp != null ? builder.createTimestamp : Instant.now();
        this.metadataItems = builder.metadataItems;
        this.transaction = builder.transaction;
    }

    private Message(byte[] data) {
        this.data = data;
        this.seqNo = null;
        this.createTimestamp = Instant.now();
    }

    public static Message of(byte[] data) {
        return new Message(data);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Long getSeqNo() {
        return seqNo;
    }

    public Instant getCreateTimestamp() {
        return createTimestamp;
    }

    public List<MetadataItem> getMetadataItems() {
        return metadataItems;
    }

    public BaseTransaction getTransaction() {
        return transaction;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private  byte[] data;
        private Long seqNo = null;
        private Instant createTimestamp = null;
        private List<MetadataItem> metadataItems = null;
        private BaseTransaction transaction = null;

        public Builder setData(byte[] data) {
            this.data = data;
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

        public Builder addMetadataItem(@Nonnull MetadataItem metadataItem) {
            if (metadataItems == null) {
                metadataItems = new ArrayList<>();
            }
            metadataItems.add(metadataItem);
            return this;
        }

        public Builder setMetadataItems(List<MetadataItem> metadataItems) {
            this.metadataItems = metadataItems;
            return this;
        }

        public Builder setTransaction(BaseTransaction transaction) {
            this.transaction = transaction;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }
}
