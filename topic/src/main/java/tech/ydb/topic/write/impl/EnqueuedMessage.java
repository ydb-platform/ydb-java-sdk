package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.WriteAck;

public class EnqueuedMessage {
    private final Message message;
    private final CompletableFuture<WriteAck> future = new CompletableFuture<>();
    private final AtomicBoolean isCompressed = new AtomicBoolean(false);
    private final long uncompressedSizeBytes;
    private long compressedSizeBytes;
    private long seqNo;

    public EnqueuedMessage(Message message) {
        this.message = message;
        this.uncompressedSizeBytes = message.getData().length;
    }

    public Message getMessage() {
        return message;
    }

    public CompletableFuture<WriteAck> getFuture() {
        return future;
    }

    public boolean isCompressed() {
        return isCompressed.get();
    }

    public void setCompressed(boolean compressed) {
        this.isCompressed.set(compressed);
    }

    public long getUncompressedSizeBytes() {
        return uncompressedSizeBytes;
    }

    public long getCompressedSizeBytes() {
        return compressedSizeBytes;
    }

    public void setCompressedSizeBytes(long compressedSizeBytes) {
        this.compressedSizeBytes = compressedSizeBytes;
    }

    public long getSizeBytes() {
        return isCompressed() ? getCompressedSizeBytes() : getUncompressedSizeBytes();
    }

    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }
}
