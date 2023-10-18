package tech.ydb.topic.read.events;

import javax.annotation.Nullable;

/**
 * @author Nikolay Perfilov
 */
public interface StopPartitionSessionEvent {
    long getPartitionSessionId();

    @Nullable
    Long getPartitionId();

    long getCommittedOffset();

    void confirm();
}
