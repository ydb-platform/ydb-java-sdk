package tech.ydb.topic.read.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.DecompressionException;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.MessageCommitter;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public class MessageImpl implements Message {
    private final PartitionSession session;
    private final MessageCommitter committer;
    private final long offset;
    private final OffsetsRange commitRange;

    private final long uncompressedSize;
    private final long seqNo;
    private final Instant createdAt;
    private final String messageGroupId;
    private final BatchMeta batchMeta;
    private final List<MetadataItem> metadataItems;

    private byte[] data;
    private IOException exception = null;

    public MessageImpl(PartitionSession session, MessageCommitter committer, BatchMeta meta, OffsetsRange commitRange,
            YdbTopic.StreamReadMessage.ReadResponse.MessageData msg) {
        this.session = session;
        this.committer = committer;
        this.uncompressedSize = msg.getUncompressedSize();
        this.offset = msg.getOffset();
        this.commitRange = commitRange;
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
    public OffsetsRange getRangeToCommit() {
        return commitRange;
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
        return session;
    }

    @Override
    public List<MetadataItem> getMetadataItems() {
        return metadataItems;
    }

    @Override
    @SuppressWarnings("deprecation")
    public PartitionOffsets getPartitionOffsets() {
        return new PartitionOffsets(session, Collections.singletonList(commitRange));
    }

    @Override
    public MessageCommitter getCommitter() {
        return committer;
    }
}
