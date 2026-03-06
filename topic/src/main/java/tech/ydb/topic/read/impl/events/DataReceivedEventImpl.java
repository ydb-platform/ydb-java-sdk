package tech.ydb.topic.read.impl.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.MessageImpl;
import tech.ydb.topic.read.impl.OffsetsRangeImpl;
import tech.ydb.topic.read.impl.PartitionSessionImpl;

/**
 * @author Nikolay Perfilov
 */
public class DataReceivedEventImpl implements DataReceivedEvent {
    private final PartitionSessionImpl session;
    private final List<Message> messages;
    private final OffsetsRange offsetsToCommit;

    public DataReceivedEventImpl(PartitionSessionImpl session, List<MessageImpl> messages) {
        this.session = session;
        this.messages = new ArrayList<>(messages);
        this.offsetsToCommit = new OffsetsRangeImpl(
                messages.get(0).getCommitFromOffset(),
                messages.get(messages.size() - 1).getCommitToOffset()
        );
    }

    @Override
    public PartitionSession getPartitionSession() {
        return session.getSessionId();
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public PartitionOffsets getPartitionOffsets() {
        return new PartitionOffsets(session.getSessionId(), Collections.singletonList(offsetsToCommit));
    }

    public PartitionSessionImpl getPartitionSessionImpl() {
        return session;
    }

    @Override
    public CompletableFuture<Void> commit() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        session.commit(offsetsToCommit, future);
        return future;
    }
}
