package tech.ydb.topic.write;

import java.util.concurrent.CompletableFuture;

/**
 * A multiwriter bound to one already active transaction. Message futures confirm staging only.
 * A deduplicated retry in the same transaction may return ALREADY_WRITTEN instead of WRITTEN_IN_TX;
 * neither state confirms commit or reader visibility.
 * The caller must await {@link #prepareCommit()} before committing the external transaction.
 * Calling that transaction's commit method directly does not automatically wait for this writer.
 * A failed transaction must be retried as a whole by the application; this writer never moves
 * individual messages to a new transaction. An unknown commit result is not a rollback result.
 */
public interface TransactionalMultiWriter extends AsyncMultiWriter {
    /**
     * Atomically stops admission and waits for staging of all messages admitted before the barrier.
     * Later sends fail. Repeated calls return the same logical preparation result.
     * Success means ready to commit, not committed or visible to readers. After failure the caller
     * must roll back or resolve the outcome of the external transaction; shutdown itself is not rollback.
     *
     * @return preparation barrier
     */
    CompletableFuture<Void> prepareCommit();
}
