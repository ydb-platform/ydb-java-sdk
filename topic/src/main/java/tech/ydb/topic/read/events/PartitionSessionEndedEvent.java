package tech.ydb.topic.read.events;

import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public interface PartitionSessionEndedEvent {
    PartitionSession getPartitionSession();
}
