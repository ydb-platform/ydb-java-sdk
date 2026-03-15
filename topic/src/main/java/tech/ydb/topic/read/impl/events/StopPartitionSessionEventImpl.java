package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;

/**
 * @author Nikolay Perfilov
 */
public abstract class StopPartitionSessionEventImpl implements StopPartitionSessionEvent {
    private final PartitionSession partition;
    private final long committedOffset;

    public StopPartitionSessionEventImpl(PartitionSession partition, long committedOffset) {
        this.partition = partition;
        this.committedOffset = committedOffset;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partition;
    }

    @Override
    public long getPartitionSessionId() {
        return partition.getId();
    }

    @Override
    public long getCommittedOffset() {
        return committedOffset;
    }

    @Override
    public Long getPartitionId() {
        return partition.getPartitionId();
    }
}
