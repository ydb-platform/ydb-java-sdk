package tech.ydb.topic.read.impl.events;


import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;

/**
 * @author Nikolay Perfilov
 */
public abstract class StartPartitionSessionEventImpl implements StartPartitionSessionEvent {
    private final PartitionSession sessionId;
    private final long committedOffset;
    private final OffsetsRange partitionOffsets;

    public StartPartitionSessionEventImpl(PartitionSession sessionId, long committedOffset, OffsetsRange offsets) {
        this.sessionId = sessionId;
        this.committedOffset = committedOffset;
        this.partitionOffsets = offsets;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return sessionId;
    }

    @Override
    public long getCommittedOffset() {
        return committedOffset;
    }

    @Override
    public OffsetsRange getPartitionOffsets() {
        return partitionOffsets;
    }

    @Override
    public void confirm() {
        confirm(null);
    }
}
