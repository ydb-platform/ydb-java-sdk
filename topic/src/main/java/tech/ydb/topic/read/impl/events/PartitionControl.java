package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.PartitionSession;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public interface PartitionControl {
    PartitionSession getPartition();

    boolean isActive();

    void confirmProcessedRange(OffsetsRange range);
}
