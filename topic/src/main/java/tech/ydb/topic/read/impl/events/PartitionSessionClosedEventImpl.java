package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;

/**
 * @author Nikolay Perfilov
 */
public class PartitionSessionClosedEventImpl implements PartitionSessionClosedEvent {
    private final PartitionSession partitionSession;

    public PartitionSessionClosedEventImpl(PartitionSession partitionSession) {
        this.partitionSession = partitionSession;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partitionSession;
    }
}
