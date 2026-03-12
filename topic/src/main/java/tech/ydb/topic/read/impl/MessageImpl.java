package tech.ydb.topic.read.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.DecompressionException;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public class MessageImpl implements Message {
    private final ReadPartitionSession session;
    private final long uncompressedSize;
    private final long commitOffset;
    private final long offset;
    private final long seqNo;
    private final Instant createdAt;
    private final String messageGroupId;
    private final BatchMeta batchMeta;
    private final List<MetadataItem> metadataItems;

    private byte[] data;
    private IOException exception = null;

    public MessageImpl(ReadPartitionSession session, BatchMeta meta, long commitFromOffset,
            YdbTopic.StreamReadMessage.ReadResponse.MessageData msg) {
        this.session = session;
        this.uncompressedSize = msg.getUncompressedSize();
        this.commitOffset = commitFromOffset;
        this.offset = msg.getOffset();
        this.seqNo = msg.getSeqNo();
        this.createdAt = ProtobufUtils.protoToInstant(msg.getCreatedAt());
        this.messageGroupId = msg.getMessageGroupId();
        this.metadataItems = msg.getMetadataItemsList().stream()
                .map(metadataItem -> new MetadataItem(metadataItem.getKey(), metadataItem.getValue().toByteArray()))
                .collect(Collectors.toList());
        this.batchMeta = meta;

        this.data = msg.getData().toByteArray();
    }

    @Override
    public byte[] getData() {
        if (exception != null) {
            throw new DecompressionException("Error occurred while decoding a message",
                    exception, data, batchMeta.getCodec());
        }
        return data;
    }

    public long getUncompressedSize() {
        return uncompressedSize;
    }

    public long getCommitFromOffset() {
        return commitOffset;
    }

    public long getCommitToOffset() {
        return offset + 1;
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
        return session.getPartition();
    }

    public ReadPartitionSession getPartitionSessionImpl() {
        return session;
    }

    @Override
    public List<MetadataItem> getMetadataItems() {
        return metadataItems;
    }

    @Override
    public PartitionOffsets getPartitionOffsets() {
        OffsetsRange range = new OffsetsRangeImpl(getCommitFromOffset(), getCommitToOffset());
        return new PartitionOffsets(session.getPartition(), Collections.singletonList(range));
    }

    @Override
    public CompletableFuture<Void> commit() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        OffsetsRange range = new OffsetsRangeImpl(getCommitFromOffset(), getCommitToOffset());
        session.commit(range, future);
        return future;
    }
}
