package tech.ydb.topic.read;

import java.util.concurrent.CompletableFuture;

/**
 * A helper class that is used to call deferred commits
 * Contains no data references and therefore may be useful in cases where commit() is called after processing data in
 * an external system
 *
 * @author Nikolay Perfilov
 */
public interface Committer {
    /**
     * Commits offsets associated with this committer
     * If there was an error while committing, there is no point of retrying committing the same message(s):
     * the whole PartitionSession should be shut down by that time. And if commit hadn't reached the server,
     * it will resend all these messages in next PartitionSession.
     * @return CompletableFuture that will be completed when commit confirmation from server will be received
     */
    CompletableFuture<Void> commit();
}
