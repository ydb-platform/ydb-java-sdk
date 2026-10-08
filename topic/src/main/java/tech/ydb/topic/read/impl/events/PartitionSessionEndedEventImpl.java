package tech.ydb.topic.read.impl.events;

import java.util.List;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.*;

/**
 * @author Nikolay Perfilov
 */
public class PartitionSessionEndedEventImpl implements PartitionSessionEndedEvent {
    private final PartitionSession partition;
    private final List<Long> childPartitionIds;

    public PartitionSessionEndedEventImpl(PartitionSession partition, List<Long> childPartitionIds) {
        this.partition = partition;
        this.childPartitionIds = childPartitionIds;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partition;
    }

    public List<Long> getChildPartitionIds() {
        return childPartitionIds;
    }
}
