package tech.ydb.topic.read.impl.events;

import java.util.Collections;
import java.util.List;

import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.MessageCommitter;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;

/**
 * @author Nikolay Perfilov
 */
public class DataReceivedEventImpl implements DataReceivedEvent {
    private final PartitionSession session;
    private final MessageCommitter committer;
    private final List<Message> messages;
    private final OffsetsRange offsetRange;

    public DataReceivedEventImpl(PartitionSession session, MessageCommitter committer, List<Message> messages) {
        this.session = session;
        this.committer = committer;
        this.messages = messages;
        this.offsetRange = OffsetsRange.of(
                messages.get(0).getRangeToCommit().getStart(),
                messages.get(messages.size() - 1).getRangeToCommit().getEnd()
        );
    }

    @Override
    public OffsetsRange getRangeToCommit() {
        return offsetRange;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return session;
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public MessageCommitter getCommitter() {
        return committer;
    }

    @Override
    @Deprecated
    public PartitionOffsets getPartitionOffsets() {
        return new PartitionOffsets(session, Collections.singletonList(offsetRange));
    }
}
