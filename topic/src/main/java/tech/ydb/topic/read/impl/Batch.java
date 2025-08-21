package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.description.Codec;

/**
 * @author Nikolay Perfilov
 */
public class Batch {
    private final BatchMeta meta;
    private final List<MessageImpl> messages = new ArrayList<>();
    // Completes when batch is read
    private final CompletableFuture<Void> readFuture = new CompletableFuture<>();
    private boolean decompressed = false;

    public Batch(BatchMeta meta) {
        this.meta = meta;
    }

    public List<MessageImpl> getMessages() {
        return messages;
    }

    public void addMessage(MessageImpl message) {
        messages.add(message);
    }

    public void complete() {
        readFuture.complete(null);
    }

    public CompletableFuture<Void> getReadFuture() {
        return readFuture;
    }

    public Codec getCodec() {
        return meta.getCodec();
    }

    public boolean isDecompressed() {
        return decompressed;
    }

    public void setDecompressed(boolean decompressed) {
        this.decompressed = decompressed;
    }

    long getFirstCommitOffsetFrom() {
        return messages.get(0).getCommitOffsetFrom();
    }

    long getLastOffset() {
        return messages.get(messages.size() - 1).getOffset();
    }
}
