package tech.ydb.topic.read.impl;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.MessageCommitter;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public abstract class MessageImpl implements Message {
    private final PartitionSession session;
    private final MessageCommitter committer;
    private final BatchMeta batchMeta;
    private final OffsetsRange commitRange;

    private final long seqNo;
    private final long offset;
    private final Instant createdAt;
    private final String messageGroupId;
    private final List<MetadataItem> metadataItems;

    public MessageImpl(PartitionSession session, MessageCommitter committer, BatchMeta meta, OffsetsRange commitRange,
            YdbTopic.StreamReadMessage.ReadResponse.MessageData msg) {
        this.session = session;
        this.committer = committer;
        this.batchMeta = meta;
        this.commitRange = commitRange;

        this.offset = msg.getOffset();
        this.seqNo = msg.getSeqNo();
        this.createdAt = ProtobufUtils.protoToInstant(msg.getCreatedAt());
        this.messageGroupId = msg.getMessageGroupId();
        this.metadataItems = msg.getMetadataItemsList().stream()
                .map(metadataItem -> new MetadataItem(metadataItem.getKey(), metadataItem.getValue().toByteArray()))
                .collect(Collectors.toList());
    }

    public abstract boolean isReady();

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
