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
    private byte[] data;
    private final long offset;
    private final long seqNo;
    private final long commitOffsetFrom;
    private final Instant createdAt;
    private final String messageGroupId;
    private final BatchMeta batchMeta;
    private final PartitionSessionImpl partitionSession;
    private final List<MetadataItem> metadataItems;
    private final OffsetsRange offsetsToCommit;
    private final CommitterImpl committer;
    private IOException exception = null;

    public MessageImpl(PartitionSessionImpl session, BatchMeta meta, long commitOffsetFrom,
            YdbTopic.StreamReadMessage.ReadResponse.MessageData msg) {
        this.data = msg.getData().toByteArray();
        this.offset = msg.getOffset();
        this.seqNo = msg.getSeqNo();
        this.commitOffsetFrom = commitOffsetFrom;
        this.createdAt = ProtobufUtils.protoToInstant(msg.getCreatedAt());
        this.messageGroupId = msg.getMessageGroupId();
        this.metadataItems = msg.getMetadataItemsList().stream()
                .map(metadataItem -> new MetadataItem(metadataItem.getKey(), metadataItem.getValue().toByteArray()))
                .collect(Collectors.toList());

        this.batchMeta = meta;
        this.partitionSession = session;
        this.offsetsToCommit = new OffsetsRangeImpl(commitOffsetFrom, offset + 1);
        this.committer = new CommitterImpl(partitionSession, 1, offsetsToCommit);
    }

    @Override
    public byte[] getData() {
        if (exception != null) {
            throw new DecompressionException("Error occurred while decoding a message",
                    exception, data, batchMeta.getCodec());
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
        return partitionSession.getSessionId();
    }

    public PartitionSessionImpl getPartitionSessionImpl() {
        return partitionSession;
    }

    @Override
    public List<MetadataItem> getMetadataItems() {
        return metadataItems;
    }

    @Override
    public PartitionOffsets getPartitionOffsets() {
        return new PartitionOffsets(partitionSession.getSessionId(), Collections.singletonList(offsetsToCommit));
    }

    @Override
    public CompletableFuture<Void> commit() {
        return committer.commitImpl(false);
    }

    public OffsetsRange getOffsetsToCommit() {
        return offsetsToCommit;
    }
}
