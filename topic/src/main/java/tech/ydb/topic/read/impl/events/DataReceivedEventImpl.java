package tech.ydb.topic.read.impl.events;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;

/**
 * @author Nikolay Perfilov
 */
public class DataReceivedEventImpl implements DataReceivedEvent {
    private final List<Message> messages;
    private final PartitionSession partitionSession;
    private final Supplier<CompletableFuture<Void>> commitCallback;

    public DataReceivedEventImpl(List<Message> messages, PartitionSession partitionSession,
                                 Supplier<CompletableFuture<Void>> commitCallback) {
        this.messages = messages;
        this.partitionSession = partitionSession;
        this.commitCallback = commitCallback;
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partitionSession;
    }

    @Override
    public CompletableFuture<Void> commit() {
        return commitCallback.get();
    }
}
