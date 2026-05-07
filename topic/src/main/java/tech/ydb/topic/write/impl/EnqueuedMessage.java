package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;

import tech.ydb.topic.write.WriteAck;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class EnqueuedMessage {
    private final MessageMeta meta;
    private final CompletableFuture<WriteAck> ackFuture = new CompletableFuture<>();

    private volatile ByteString data = null;
    private volatile Throwable problem = null;
    private volatile long bufferSize;
    private volatile boolean isReady = false;

    public EnqueuedMessage(MessageMeta meta, long bufferSize) {
        this.meta = meta;
        this.data = null;
        this.bufferSize = bufferSize;
    }

    public MessageMeta getMeta() {
        return meta;
    }

    public ByteString getData() {
        return data;
    }

    public Throwable getProblem() {
        return problem;
    }

    public CompletableFuture<WriteAck> getAckFuture() {
        return ackFuture;
    }

    public boolean isReady() {
        return isReady;
    }

    public long getBufferSize() {
        return bufferSize;
    }

    public void completeWithData(ByteString data, long updatedSize) {
        this.bufferSize = updatedSize;
        this.data = data;
        this.problem = null;
        this.isReady = true;
    }

    public void completeWithProblem(Throwable problem) {
        this.problem = problem;
        this.isReady = true;
    }

    public void close(Throwable problem) {
        this.problem = problem;
        this.ackFuture.completeExceptionally(problem);
        this.isReady = true;
    }
}
