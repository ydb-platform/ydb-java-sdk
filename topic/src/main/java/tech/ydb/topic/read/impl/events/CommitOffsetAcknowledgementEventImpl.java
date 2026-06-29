package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.CommitOffsetAcknowledgementEvent;

public class CommitOffsetAcknowledgementEventImpl implements CommitOffsetAcknowledgementEvent {
    private final PartitionSession partition;
    private final long committedOffset;

    public CommitOffsetAcknowledgementEventImpl(PartitionSession partition, long committedOffset) {
        this.partition = partition;
        this.committedOffset = committedOffset;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partition;
    }

    @Override
    public long getCommittedOffset() {
        return committedOffset;
    }
}
