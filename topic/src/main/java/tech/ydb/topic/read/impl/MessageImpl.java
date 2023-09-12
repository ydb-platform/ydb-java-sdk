package tech.ydb.topic.read.impl;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public class MessageImpl implements Message {
    private static final Logger logger = LoggerFactory.getLogger(MessageImpl.class);
    private byte[] data;
    private final long offset;
    private final long seqNo;
    private final long commitOffsetFrom;
    private final Instant createdAt;
    private final String messageGroupId;
    private final BatchMeta batchMeta;
    private final PartitionSession partitionSession;
    private final Function<OffsetsRange, CompletableFuture<Void>> commitFunction;
    private boolean isDecompressed = false;

    private MessageImpl(Builder builder) {
        this.data = builder.data;
        this.offset = builder.offset;
        this.seqNo = builder.seqNo;
        this.commitOffsetFrom = builder.commitOffsetFrom;
        this.createdAt = builder.createdAt;
        this.messageGroupId = builder.messageGroupId;
        this.batchMeta = builder.batchMeta;
        this.partitionSession = builder.partitionSession;
        this.commitFunction = builder.commitFunction;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public long getSeqNo() {
        return seqNo;
    }

    @Override
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
        return partitionSession;
    }

    public void setDecompressed(boolean decompressed) {
        isDecompressed = decompressed;
    }

    @Override
    public CompletableFuture<Void> commit() {
        final long commitOffsetTo = offset + 1;
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] partition session {} (partition {}): committing message with offset {} [{}-{})",
                    partitionSession.getPath(), partitionSession.getId(), partitionSession.getPartitionId(),
                    offset, commitOffsetFrom, commitOffsetTo);
        }
        return commitFunction.apply(new OffsetsRange(commitOffsetFrom, commitOffsetTo));
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
        private PartitionSession partitionSession;
        private Function<OffsetsRange, CompletableFuture<Void>> commitFunction;

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

        public Builder setPartitionSession(PartitionSession partitionSession) {
            this.partitionSession = partitionSession;
            return this;
        }

        public Builder setCommitFunction(Function<OffsetsRange, CompletableFuture<Void>> commitFunction) {
            this.commitFunction = commitFunction;
            return this;
        }

        public MessageImpl build() {
            return new MessageImpl(this);
        }
    }
}
