package tech.ydb.topic.read.events;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.read.Committer;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;

/**
 * @author Nikolay Perfilov
 */
public interface DataReceivedEvent {

    /**
     * Returns a list of messages grouped in one batch.
     * Each message can be committed individually or all messages can be committed at once with commit() method
     *
     * @return a list of messages
     */
    List<Message> getMessages();

    /**
     * Returns a partition session this data was received on
     *
     * @return a partition session this data was received on
     */
    PartitionSession getPartitionSession();

    /**
     * Commits all messages in this event at once.
     * If there was an error while committing, there is no point of retrying committing the same messages:
     * the whole PartitionSession should be shut down by that time. And if commit hadn't reached the server,
     * it will resend all these messages in next PartitionSession.
     *
     * @return a CompletableFuture that will be completed when commit confirmation from server will be received
     */
    CompletableFuture<Void> commit();

    /**
     * Returns a Committer object to call commit() on later.
     * This object has no data references and therefore may be useful in cases where commit() is called after
     * processing data in an external system
     *
     * @return a Committer object
     */
    Committer getCommitter();

}
