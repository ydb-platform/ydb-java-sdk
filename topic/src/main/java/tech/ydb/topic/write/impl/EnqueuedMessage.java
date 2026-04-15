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
        return data != null;
    }

    public boolean hasProblem() {
        return problem != null;
    }

    public long getBufferSize() {
        return bufferSize;
    }

    public void setData(ByteString data, long updatedSize) {
        this.bufferSize = updatedSize;
        this.data = data;
        this.problem = null;
    }

    public void setError(Throwable ex) {
        this.problem = ex;
    }
}
