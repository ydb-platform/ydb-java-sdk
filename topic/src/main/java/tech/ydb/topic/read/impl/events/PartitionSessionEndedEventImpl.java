package tech.ydb.topic.read.impl.events;

import java.util.List;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.*;
import tech.ydb.topic.read.impl.TopicPartition;

/**
 * @author Nikolay Perfilov
 */
public class PartitionSessionEndedEventImpl implements PartitionSessionEndedEvent {
    private final PartitionSession partition;
    private final List<TopicPartition> childs;

    public PartitionSessionEndedEventImpl(PartitionSession partition, List<TopicPartition> childs) {
        this.partition = partition;
        this.childs = childs;
    }

    @Override
    public PartitionSession getPartitionSession() {
        return partition;
    }

    public List<TopicPartition> getChilds() {
        return childs;
    }

    public String getChildsString() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (TopicPartition child: childs) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(child.getPartitionId());
            first = false;
        }
        return sb.append("]").toString();
    }
}
