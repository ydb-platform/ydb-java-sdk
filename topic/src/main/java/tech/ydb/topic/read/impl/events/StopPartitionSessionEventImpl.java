package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.events.StopPartitionSessionEvent;

/**
 * @author Nikolay Perfilov
 */
public class StopPartitionSessionEventImpl implements StopPartitionSessionEvent {
    private final long partitionSessionId;
    private final long committedOffset;
    private final Runnable confirmCallback;

    public StopPartitionSessionEventImpl(long partitionSessionId, long committedOffset, Runnable confirmCallback) {
        this.partitionSessionId = partitionSessionId;
        this.committedOffset = committedOffset;
        this.confirmCallback = confirmCallback;
    }

    @Override
    public long getPartitionSessionId() {
        return partitionSessionId;
    }

    @Override
    public long getCommittedOffset() {
        return committedOffset;
    }

    @Override
    public void confirm() {
        confirmCallback.run();
    }
}
