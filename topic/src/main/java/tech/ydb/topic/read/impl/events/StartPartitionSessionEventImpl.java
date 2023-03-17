package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.impl.OffsetsRange;

/**
 * @author Nikolay Perfilov
 */
public class StartPartitionSessionEventImpl implements StartPartitionSessionEvent {
    private final PartitionSession partitionSession;
    private final long committedOffset;
    private final OffsetsRange partitionOffsets;
    private final Runnable confirmCallback;

    public StartPartitionSessionEventImpl(PartitionSession partitionSession, long committedOffset,
                                      OffsetsRange partitionOffsets, Runnable confirmCallback) {
        this.partitionSession = partitionSession;
        this.committedOffset = committedOffset;
        this.partitionOffsets = partitionOffsets;
        this.confirmCallback = confirmCallback;
    }

    public PartitionSession getPartitionSession() {
        return partitionSession;
    }

    public long getCommittedOffset() {
        return committedOffset;
    }

    public OffsetsRange getPartitionOffsets() {
        return partitionOffsets;
    }

    public void confirm() {
        confirmCallback.run();
    }
}
