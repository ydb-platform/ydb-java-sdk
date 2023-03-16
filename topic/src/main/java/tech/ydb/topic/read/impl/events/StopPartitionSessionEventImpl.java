package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.events.StopPartitionSessionEvent;

/**
 * @author Nikolay Perfilov
 */
public class StopPartitionSessionEventImpl implements StopPartitionSessionEvent {
    private final long partitionSessionId;
    private final long committedOffset;
    private final boolean isGraceful;
    private final Runnable confirmCallback;

    public StopPartitionSessionEventImpl(long partitionSessionId, long committedOffset, boolean isGraceful,
                                         Runnable confirmCallback) {
        this.partitionSessionId = partitionSessionId;
        this.committedOffset = committedOffset;
        this.isGraceful = isGraceful;
        this.confirmCallback = confirmCallback;
    }

    public long getPartitionSessionId() {
        return partitionSessionId;
    }

    public long getCommittedOffset() {
        return committedOffset;
    }

    public boolean isGraceful() {
        return isGraceful;
    }

    public void confirm() {
        confirmCallback.run();
    }
}
