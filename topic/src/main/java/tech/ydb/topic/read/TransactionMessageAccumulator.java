package tech.ydb.topic.read;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;
import tech.ydb.table.transaction.BaseTransaction;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * A helper class that is used to accumulate messages and add them to {@link tech.ydb.table.transaction.Transaction}.
 * Several {@link Message}s or/and {@link tech.ydb.topic.read.events.DataReceivedEvent}s can be accepted to add to
 * {@link tech.ydb.table.transaction.Transaction} later all at once.
 * All messages should be read from the same Reader this accumulator was created on.
 * Contains no data references and therefore may also be useful in cases where messages are committed after processing
 * data in an external system.
 *
 * @author Nikolay Perfilov
 */
public interface TransactionMessageAccumulator extends MessageAccumulator {
    CompletableFuture<Status> updateOffsetsInTransaction(BaseTransaction transaction,
                                                         UpdateOffsetsInTransactionSettings settings);
}
