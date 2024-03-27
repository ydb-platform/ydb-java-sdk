package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;

/**
 * @author Nikolay Perfilov
 */
public class StopPartitionSessionEventImpl implements StopPartitionSessionEvent {
    private final PartitionSession partitionSession;
    private final long committedOffset;
    private final Runnable confirmCallback;

    public StopPartitionSessionEventImpl(PartitionSession partitionSession, long committedOffset,
                                         Runnable confirmCallback) {
        this.partitionSession = partitionSession;
        this.committedOffset = committedOffset;
        this.confirmCallback = confirmCallback;
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

    @Override
    public void confirm() {
        confirmCallback.run();
    }
}
