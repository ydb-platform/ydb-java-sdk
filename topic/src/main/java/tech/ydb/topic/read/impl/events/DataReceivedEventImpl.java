package tech.ydb.topic.read.impl.events;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.events.DataReceivedEvent;

/**
 * @author Nikolay Perfilov
 */
public class DataReceivedEventImpl implements DataReceivedEvent {
    private final List<Message> messages;
    private final Supplier<CompletableFuture<Void>> commitCallback;

    public DataReceivedEventImpl(List<Message> messages, Supplier<CompletableFuture<Void>> commitCallback) {
        this.messages = messages;
        this.commitCallback = commitCallback;
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public CompletableFuture<Void> commit() {
        return commitCallback.get();
    }
}
