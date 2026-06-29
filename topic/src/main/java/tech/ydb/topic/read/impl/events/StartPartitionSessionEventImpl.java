package tech.ydb.topic.read.impl.events;


import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;

/**
 * @author Nikolay Perfilov
 */
public abstract class StartPartitionSessionEventImpl implements StartPartitionSessionEvent {
    private final PartitionSession partition;
    private final long committedOffset;
    private final OffsetsRange partitionOffsets;

    public StartPartitionSessionEventImpl(PartitionSession partition, long committedOffset, OffsetsRange offsets) {
        this.partition = partition;
        this.committedOffset = committedOffset;
        this.partitionOffsets = offsets;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partition;
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
