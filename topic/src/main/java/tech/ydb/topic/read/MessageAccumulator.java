package tech.ydb.topic.read;

import tech.ydb.topic.read.events.DataReceivedEvent;

/**
 * A common interface that is used to accumulate several {@link Message}s or/and
 * {@link tech.ydb.topic.read.events.DataReceivedEvent}s to commit later all at once or to add to transaction.
 *
 * @author Nikolay Perfilov
 */
public interface MessageAccumulator {

    /**
     * Adds a {@link Message} to commit it later or to add to transaction
     *
     * @param message a {@link Message}
     */
    void add(Message message);

    /**
     * Adds a {@link DataReceivedEvent} to commit all its messages later or to add to transaction
     *
     * @param event a {@link DataReceivedEvent}
     */
    void add(DataReceivedEvent event);
}
