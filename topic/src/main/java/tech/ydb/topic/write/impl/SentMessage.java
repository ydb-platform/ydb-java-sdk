package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.protobuf.UnsafeByteOperations;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.write.WriteAck;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SentMessage {
    private final long seqNo;
    private final long bufferSize;
    private final YdbTransaction tx;
    private final YdbTopic.StreamWriteMessage.WriteRequest.MessageData pb;
    private final CompletableFuture<WriteAck> ackFuture;

    public SentMessage(EnqueuedMessage msg, long seqNo) {
        this.seqNo = seqNo;
        this.bufferSize = msg.getBufferSize();
        this.tx = msg.getMeta().getTransaction();
        this.pb = buildPb(seqNo, msg);
        this.ackFuture = msg.getAckFuture();
    }

    public long getSeqNo() {
        return seqNo;
    }

    public long getBufferSize() {
        return bufferSize;
    }

    public CompletableFuture<WriteAck> getAckFuture() {
        return ackFuture;
    }

    public YdbTransaction getTx() {
        return tx;
    }

    public YdbTopic.StreamWriteMessage.WriteRequest.MessageData getPb() {
        return pb;
    }

    private static YdbTopic.StreamWriteMessage.WriteRequest.MessageData buildPb(long seqNo, EnqueuedMessage msg) {
        MessageMeta meta = msg.getMeta();
        return YdbTopic.StreamWriteMessage.WriteRequest.MessageData.newBuilder()
                .setSeqNo(seqNo)
                .setData(msg.getData())
                .setCreatedAt(ProtobufUtils.instantToProto(meta.getCreatedAt()))
                .setUncompressedSize(meta.getUncompressedSize())
                .addAllMetadataItems(meta.getItems().stream()
                        .map(it -> YdbTopic.MetadataItem.newBuilder()
                        .setKey(it.getKey())
                        .setValue(UnsafeByteOperations.unsafeWrap(it.getValue()))
                        .build()).collect(Collectors.toList())
                ).build();
    }
}
