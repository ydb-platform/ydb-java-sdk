package tech.ydb.topic.read.impl.events;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.CommitterImpl;
import tech.ydb.topic.read.impl.PartitionSessionImpl;

/**
 * @author Nikolay Perfilov
 */
public class DataReceivedEventImpl implements DataReceivedEvent {
    private final List<Message> messages;
    private final PartitionSessionImpl partitionSession;
    private final OffsetsRange offsetsToCommit;
    private final CommitterImpl committer;

    public DataReceivedEventImpl(PartitionSessionImpl partitionSession, List<Message> messages,
                                 OffsetsRange offsetsToCommit) {
        this.messages = messages;
        this.partitionSession = partitionSession;
        this.offsetsToCommit = offsetsToCommit;
        this.committer = new CommitterImpl(partitionSession, messages.size(), offsetsToCommit);
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public PartitionOffsets getPartitionOffsets() {
        return new PartitionOffsets(partitionSession.getSessionId(), Collections.singletonList(offsetsToCommit));
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partitionSession.getSessionId();
    }

    public PartitionSessionImpl getPartitionSessionImpl() {
        return partitionSession;
    }

    @Override
    public CompletableFuture<Void> commit() {
        return committer.commitImpl(false);
    }

    public OffsetsRange getOffsetsToCommit() {
        return offsetsToCommit;
    }
}
