package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;

/**
 * @author Nikolay Perfilov
 */
public abstract class StopPartitionSessionEventImpl implements StopPartitionSessionEvent {
    private final PartitionSession partitionSession;
    private final long committedOffset;

    public StopPartitionSessionEventImpl(PartitionSession session, long committedOffset) {
        this.partitionSession = session;
        this.committedOffset = committedOffset;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partitionSession;
    }

    @Override
    public long getPartitionSessionId() {
        return partitionSession.getId();
    }

    @Override
    public long getCommittedOffset() {
        return committedOffset;
    }

    @Override
    public Long getPartitionId() {
        return partitionSession.getPartitionId();
    }
}
