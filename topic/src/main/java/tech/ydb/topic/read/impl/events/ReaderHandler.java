package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.events.CommitOffsetAcknowledgementEvent;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.ReaderClosedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
@FunctionalInterface
public interface ReaderHandler extends ReadEventHandler {

    default void onMessagesImpl(DataReceivedEventImpl event) {
        PartitionControl control = event.getPartitionControl();
        try {
            if (control.isActive()) {
                onMessages(event);
            }
        } finally {
            control.confirmProcessedRange(event.getRangeToCommit());
        }
    }

    static ReaderHandler of(ReadEventHandler origin) {
        if (origin instanceof ReaderHandler) {
            return (ReaderHandler) origin;
        }

        return new ReaderHandler() {
            @Override
            public void onCommitResponse(CommitOffsetAcknowledgementEvent event) {
                origin.onCommitResponse(event);
            }

            @Override
            public void onMessages(DataReceivedEvent event) {
                origin.onMessages(event);
            }

            @Override
            public void onStartPartitionSession(StartPartitionSessionEvent event) {
                origin.onStartPartitionSession(event);
            }

            @Override
            public void onStopPartitionSession(StopPartitionSessionEvent event) {
                origin.onStopPartitionSession(event);
            }

            @Override
            public void onPartitionSessionClosed(PartitionSessionClosedEvent event) {
                origin.onPartitionSessionClosed(event);
            }

            @Override
            public void onReaderClosed(ReaderClosedEvent event) {
                origin.onReaderClosed(event);
            }

            @Override
            public void onSessionStarted(SessionStartedEvent event) {
                origin.onSessionStarted(event);
            }
        };
    }
}
