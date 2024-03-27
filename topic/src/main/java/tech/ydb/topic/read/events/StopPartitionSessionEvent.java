package tech.ydb.topic.read.events;

import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public interface StopPartitionSessionEvent {
    PartitionSession getPartitionSession();
    long getPartitionSessionId();

    Long getPartitionId();

    long getCommittedOffset();

    void confirm();
}
