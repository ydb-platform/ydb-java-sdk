package tech.ydb.topic.read;

import java.util.concurrent.CompletableFuture;

import io.grpc.ExperimentalApi;

import tech.ydb.core.Status;
import tech.ydb.table.transaction.BaseTransaction;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;

/**
 * @author Nikolay Perfilov
 */
@ExperimentalApi("Topic service interfaces are experimental and may change without notice")
public interface AsyncReader {

    /**
     * Initialize reading in the background. Non-blocking
     */
    CompletableFuture<Void> init();

    /**
     * Stops internal threads and makes cleanup in background. Non-blocking
     */
    CompletableFuture<Void> shutdown();

    /**
     * Add offsets to transaction request sent from client to server.
     *
     * @param settings  {@link WriterSettings}
     * @return topic {@link AsyncWriter}
     */
    CompletableFuture<Status> updateOffsetsInTransaction(BaseTransaction transaction, PartitionOffsets offsets,
                                                         UpdateOffsetsInTransactionSettings settings);
}
