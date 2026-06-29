package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;

/**
 * @author Nikolay Perfilov
 */
public class PartitionSessionClosedEventImpl implements PartitionSessionClosedEvent {
    private final PartitionSession partition;

    public PartitionSessionClosedEventImpl(PartitionSession partition) {
        this.partition = partition;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partition;
    }
}
