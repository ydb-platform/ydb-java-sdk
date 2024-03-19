package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import tech.ydb.common.transaction.BaseTransaction;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public class EnqueuedMessage {
    private final Message message;
    private final CompletableFuture<WriteAck> future = new CompletableFuture<>();
    private final AtomicBoolean isCompressed = new AtomicBoolean();
    private final AtomicBoolean isProcessingFailed = new AtomicBoolean();
    private final long uncompressedSizeBytes;
    private final BaseTransaction transaction;
    private long compressedSizeBytes;
    private Long seqNo;

    public EnqueuedMessage(Message message, BaseTransaction transaction) {
        this.message = message;
        this.uncompressedSizeBytes = message.getData().length;
        this.transaction = transaction;
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

    public boolean isProcessingFailed() {
        return isProcessingFailed.get();
    }

     public void setProcessingFailed(boolean procesingFailed) {
        isProcessingFailed.set(procesingFailed);
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

    public Long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    public BaseTransaction getTransaction() {
        return transaction;
    }
}
