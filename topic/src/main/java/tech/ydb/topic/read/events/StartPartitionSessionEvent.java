package tech.ydb.topic.read.events;

import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.settings.StartPartitionSessionSettings;

/**
 * @author Nikolay Perfilov
 */
public interface StartPartitionSessionEvent {

    PartitionSession getPartitionSession();

    long getCommittedOffset();

    OffsetsRange getPartitionOffsets();

    void confirm();

    void confirm(StartPartitionSessionSettings settings);
}
