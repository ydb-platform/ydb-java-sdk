package tech.ydb.topic.read.events;

import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public class StopPartitionSessionEvent {
    private final PartitionSession partitionSession;
    private final long committedOffset;

    public StopPartitionSessionEvent(PartitionSession partitionSession, long committedOffset) {
        this.partitionSession = partitionSession;
        this.committedOffset = committedOffset;
    }

    public PartitionSession getPartitionSession() {
        return partitionSession;
    }

    public long getCommittedOffset() {
        return committedOffset;
    }

    public void confirm() {

    }
}
