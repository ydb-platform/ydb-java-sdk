package tech.ydb.topic.read.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.read.DecompressionException;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.OffsetsRange;
import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public class MessageImpl implements Message {
    private byte[] data;
    private final long offset;
    private final long seqNo;
    private final long commitOffsetFrom;
    private final Instant createdAt;
    private final String messageGroupId;
    private final BatchMeta batchMeta;
    private final PartitionSessionImpl partitionSession;
    private List<MetadataItem> metadataItems;
    private final OffsetsRange offsetsToCommit;
    private final CommitterImpl committer;
    private boolean isDecompressed = false;
    private IOException exception = null;

    private MessageImpl(Builder builder) {
        this.data = builder.data;
        this.offset = builder.offset;
        this.seqNo = builder.seqNo;
        this.commitOffsetFrom = builder.commitOffsetFrom;
        this.createdAt = builder.createdAt;
        this.messageGroupId = builder.messageGroupId;
        this.batchMeta = builder.batchMeta;
        this.partitionSession = builder.partitionSession;
        this.metadataItems = builder.metadataItems;
        this.offsetsToCommit = new OffsetsRangeImpl(commitOffsetFrom, offset + 1);
        this.committer = new CommitterImpl(partitionSession, 1, offsetsToCommit);
    }

    @Override
    public byte[] getData() {
        if (exception != null) {
            throw new DecompressionException("Error occurred while decoding a message",
                    exception, data);
        }
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setException(IOException exception) {
        this.exception = exception;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public long getSeqNo() {
        return seqNo;
    }

    public long getCommitOffsetFrom() {
        return commitOffsetFrom;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String getMessageGroupId() {
        return messageGroupId;
    }

    @Override
    public String getProducerId() {
        return batchMeta.getProducerId();
    }

    @Override
    public Map<String, String> getWriteSessionMeta() {
        return batchMeta.getWriteSessionMeta();
    }

    @Override
    public Instant getWrittenAt() {
        return batchMeta.getWrittenAt();
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partitionSession.getSessionInfo();
    }

    public PartitionSessionImpl getPartitionSessionImpl() {
        return partitionSession;
    }

    @Override
    public List<MetadataItem> getMetadataItems() {
        return metadataItems;
    }

    public void setDecompressed(boolean decompressed) {
        isDecompressed = decompressed;
    }

    @Override
    public CompletableFuture<Void> commit() {
        return committer.commitImpl(false);
    }

    public OffsetsRange getOffsetsToCommit() {
        return offsetsToCommit;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private byte[] data;
        private long offset;
        private long seqNo;
        private long commitOffsetFrom;
        private Instant createdAt;
        private String messageGroupId;
        private BatchMeta batchMeta;
        private PartitionSessionImpl partitionSession;
        private List<MetadataItem> metadataItems;

        public Builder setData(byte[] data) {
            this.data = data;
            return this;
        }

        public Builder setOffset(long offset) {
            this.offset = offset;
            return this;
        }

        public Builder setSeqNo(long seqNo) {
            this.seqNo = seqNo;
            return this;
        }

        public Builder setCommitOffsetFrom(long commitOffsetFrom) {
            this.commitOffsetFrom = commitOffsetFrom;
            return this;
        }

        public Builder setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setMessageGroupId(String messageGroupId) {
            this.messageGroupId = messageGroupId;
            return this;
        }

        public Builder setBatchMeta(BatchMeta batchMeta) {
            this.batchMeta = batchMeta;
            return this;
        }

        public Builder setPartitionSession(PartitionSessionImpl partitionSession) {
            this.partitionSession = partitionSession;
            return this;
        }

        public Builder setMetadataItems(List<MetadataItem> metadataItems) {
            this.metadataItems = metadataItems;
            return this;
        }

        public MessageImpl build() {
            return new MessageImpl(this);
        }
    }
}
