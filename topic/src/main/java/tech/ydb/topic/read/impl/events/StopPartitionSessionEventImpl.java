package tech.ydb.topic.read.impl.events;

import javax.annotation.Nullable;

import tech.ydb.topic.read.events.StopPartitionSessionEvent;

/**
 * @author Nikolay Perfilov
 */
public class StopPartitionSessionEventImpl implements StopPartitionSessionEvent {
    private final long partitionSessionId;
    @Nullable
    private final Long partitionId;
    private final long committedOffset;
    private final Runnable confirmCallback;

    public StopPartitionSessionEventImpl(long partitionSessionId, @Nullable Long partitionId, long committedOffset,
                                         Runnable confirmCallback) {
        this.partitionSessionId = partitionSessionId;
        this.partitionId = partitionId;
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
    @Nullable
    public Long getPartitionId() {
        return partitionId;
    }

    @Override
    public void confirm() {
        confirmCallback.run();
    }
}
