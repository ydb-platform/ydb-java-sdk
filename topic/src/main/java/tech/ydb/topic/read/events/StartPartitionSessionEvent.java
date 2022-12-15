package tech.ydb.topic.read.events;

import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public class StartPartitionSessionEvent {
    private final PartitionSession partitionSession;
    private final long committedOffset;
    private final long endOffset;

    public StartPartitionSessionEvent(PartitionSession partitionSession, long committedOffset, long endOffset) {
        this.partitionSession = partitionSession;
        this.committedOffset = committedOffset;
        this.endOffset = endOffset;
    }

    public PartitionSession getPartitionSession() {
        return partitionSession;
    }

    public long getCommittedOffset() {
        return committedOffset;
    }

    public long getEndOffset() {
        return endOffset;
    }

    public void confirm() {

    }
}
