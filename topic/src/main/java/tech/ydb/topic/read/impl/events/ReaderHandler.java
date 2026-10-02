package tech.ydb.topic.read.impl.events;

import tech.ydb.topic.read.events.ReadEventHandler;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
@FunctionalInterface
public interface ReaderHandler extends ReadEventHandler {

    default void onMessagesWithControl(DataReceivedEventImpl event) {
        PartitionControl control = event.getPartitionControl();
        try {
            if (control.isActive()) {
                onMessages(event);
            }
        } finally {
            control.confirmProcessedRange(event.getRangeToCommit());
        }
    }
}
