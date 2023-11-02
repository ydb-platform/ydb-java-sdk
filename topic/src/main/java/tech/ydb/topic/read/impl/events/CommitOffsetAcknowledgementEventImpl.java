package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.CommitOffsetAcknowledgementEvent;

public class CommitOffsetAcknowledgementEventImpl implements CommitOffsetAcknowledgementEvent {
    private final PartitionSession partitionSession;
    private final long committedOffset;

    public CommitOffsetAcknowledgementEventImpl(PartitionSession partitionSession, long committedOffset) {
        this.partitionSession = partitionSession;
        this.committedOffset = committedOffset;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partitionSession;
    }

    @Override
    public long getCommittedOffset() {
        return committedOffset;
    }
}
