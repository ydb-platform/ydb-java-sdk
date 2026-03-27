package tech.ydb.topic.read.events;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.MessageCommitter;
import tech.ydb.topic.read.PartitionOffsets;
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
     * Returns offsets range for committing of this event
     *
     * @return Offsets range for committing of this event
     */
    OffsetsRange getRangeToCommit();

    /**
     * Returns partition offsets of this message
     *
     * @return Partition offsets of this message
     */
    @Deprecated
    PartitionOffsets getPartitionOffsets();

    /**
     * Commits all messages in this event at once.
     * If there was an error while committing, there is no point of retrying committing the same messages:
     * the whole PartitionSession should be shut down by that time. And if commit hadn't reached the server,
     * it will resend all these messages in next PartitionSession.
     *
     * @return a CompletableFuture that will be completed when commit confirmation from server will be received
     */
    default CompletableFuture<Void> commit() {
        return getCommitter().commit(getRangeToCommit());
    }

    /**
     * The committer for this event. The committer is linked to the current partition reading session and is active
     * when this session is alive. The commits on nonactive committer will return failed {@link CompletableFuture }
     *
     * @return committer instance for this message
     */
    MessageCommitter getCommitter();
}
