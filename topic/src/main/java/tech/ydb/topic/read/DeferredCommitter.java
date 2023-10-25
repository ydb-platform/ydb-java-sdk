package tech.ydb.topic.read;

/**
 * A helper class that is used to call deferred commits.
 * Several {@link Message}s or/and {@link tech.ydb.topic.read.events.DataReceivedEvent}s can be accepted to commit later
 * all at once.
 * Contains no data references and therefore may also be useful in cases where commit() is called after processing data
 * in an external system.
 *
 * @author Nikolay Perfilov
 */
public interface DeferredCommitter {
    /**
     * Adds a {@link Message} to commit it later with a commit method
     *
     * @param message a {@link Message} to commit later
     */
    void add(Message message);

    /**
     * Commits offset ranges from all {@link Message}s and {@link tech.ydb.topic.read.events.DataReceivedEvent}s
     * that were added to this DeferredCommitter since last commit
     */
    void commit();
}
