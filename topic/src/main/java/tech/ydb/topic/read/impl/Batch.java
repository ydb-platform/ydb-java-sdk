package tech.ydb.topic.read.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.description.Codec;

/**
 * @author Nikolay Perfilov
 */
public class Batch {
    private final BatchMeta meta;
    private final List<MessageImpl> messages;

    // Completes when batch is read
    private final CompletableFuture<Void> readFuture = new CompletableFuture<>();
    private volatile boolean isReady = false;

    public Batch(BatchMeta meta, List<MessageImpl> messages) {
        this.meta = meta;
        this.messages = messages;
        this.isReady = meta.getCodec() == Codec.RAW;
    }

    public List<MessageImpl> getMessages() {
        return messages;
    }

    public void complete() {
        readFuture.complete(null);
    }

    public CompletableFuture<Void> getReadFuture() {
        return readFuture;
    }

    public int getCodec() {
        return meta.getCodec();
    }

    public boolean isReady() {
        return isReady;
    }

    public void markAsReady() {
        this.isReady = true;
    }
}
