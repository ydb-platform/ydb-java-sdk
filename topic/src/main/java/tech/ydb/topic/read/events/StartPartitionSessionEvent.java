package tech.ydb.topic.read.events;

import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.impl.OffsetsRange;

/**
 * @author Nikolay Perfilov
 */
public interface StartPartitionSessionEvent {

    PartitionSession getPartitionSession();

    long getCommittedOffset();

    OffsetsRange getPartitionOffsets();

    void confirm();
}
