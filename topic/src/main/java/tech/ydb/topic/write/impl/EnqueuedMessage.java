package tech.ydb.topic.write.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.protobuf.UnsafeByteOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.utils.Encoder;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.WriteAck;

public class EnqueuedMessage {

    // use logger from WriterImpl
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);

    private Long seqNo;
    private byte[] bytes;
    private final long originLength;
    private final Instant createdAt;
    private final List<MetadataItem> items;

    private final CompletableFuture<WriteAck> future = new CompletableFuture<>();
    private final YdbTransaction transaction;

    private volatile boolean isReady = false;
    private volatile IOException compressError = null;

    public EnqueuedMessage(Message message, SendSettings sendSettings, boolean noCompression) {
        this.bytes = message.getData();
        this.createdAt = message.getCreateTimestamp();
        this.items = message.getMetadataItems();
        this.seqNo = message.getSeqNo();

        this.originLength = bytes.length;
        this.transaction = sendSettings != null ? sendSettings.getTransaction() : null;
        this.isReady = noCompression;
    }

    public boolean isReady() {
        return isReady;
    }

    public long getOriginalSize() {
        return originLength;
    }

    public long getSize() {
        return bytes.length;
    }

    public IOException getCompressError() {
        return compressError;
    }

    public void encode(String writeId, Codec codec) {
        logger.trace("[{}] Started encoding message", writeId);

        try {
            bytes = Encoder.encode(codec, bytes);
            isReady = true;
            logger.trace("[{}] Successfully finished encoding message", writeId);
        } catch (IOException ex) {
            logger.error("[{}] Exception while encoding message: ", writeId, ex);
            isReady = true;
            future.completeExceptionally(ex);
        }
    }

    public CompletableFuture<WriteAck> getFuture() {
        return future;
    }

    public Long getSeqNo() {
        return seqNo;
    }

    public YdbTransaction getTransaction() {
        return transaction;
    }

    long updateSeqNo(long lastSeqNo) {
        if (seqNo == null) {
            seqNo = lastSeqNo + 1;
            return seqNo;
        }
        return Math.max(lastSeqNo, seqNo);
    }

    YdbTopic.StreamWriteMessage.WriteRequest.MessageData toMessageData() {
        return YdbTopic.StreamWriteMessage.WriteRequest.MessageData.newBuilder()
                        .setSeqNo(seqNo)
                        .setData(UnsafeByteOperations.unsafeWrap(bytes))
                        .setCreatedAt(ProtobufUtils.instantToProto(createdAt))
                        .setUncompressedSize(originLength)
                        .addAllMetadataItems(items.stream().map(it -> YdbTopic.MetadataItem.newBuilder()
                            .setKey(it.getKey())
                            .setValue(UnsafeByteOperations.unsafeWrap(it.getValue()))
                            .build()
                        ).collect(Collectors.toList()))
                .build();
    }
}
