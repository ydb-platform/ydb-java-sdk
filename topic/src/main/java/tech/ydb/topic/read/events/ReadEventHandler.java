package tech.ydb.topic.read.events;

import tech.ydb.topic.read.impl.events.SessionStartedEvent;

/**
 * @author Nikolay Perfilov
 */
@FunctionalInterface
public interface ReadEventHandler {

    void onMessages(DataReceivedEvent event);

    default void onCommitResponse(CommitOffsetAcknowledgementEvent event) { }


    default void onStartPartitionSession(StartPartitionSessionEvent event) {
        event.confirm();
    }

    default void onStopPartitionSession(StopPartitionSessionEvent event) {
        event.confirm();
    }

    default void onPartitionSessionClosed(PartitionSessionClosedEvent event) { }

    default void onReaderClosed(ReaderClosedEvent event) { }

    default void onSessionStarted(SessionStartedEvent event) { }
}
