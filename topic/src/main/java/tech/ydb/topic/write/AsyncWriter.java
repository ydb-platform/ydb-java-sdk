package tech.ydb.topic.write;

import java.util.concurrent.CompletableFuture;

import io.grpc.ExperimentalApi;

import tech.ydb.common.transaction.BaseTransaction;

/**
 * @author Nikolay Perfilov
 */
@ExperimentalApi("Topic service interfaces are experimental and may change without notice")
public interface AsyncWriter {

    /**
     * Initialize internal threads in the background. Non-blocking
     * @return {@link CompletableFuture} with {@link InitResult} containing initialization data like lastSeqNo
     */
    CompletableFuture<InitResult> init();

    /**
     * Send message. Non-blocking
     * @param message message data to write
     * @return {@link CompletableFuture} with {@link WriteAck} for write acknowledgement
     */
    CompletableFuture<WriteAck> send(Message message) throws QueueOverflowException;

    /**
     * Get a virtual writer that writes all messages in provided transaction.
     * Such messages will be written only in case of transaction commit
     * @param transaction a transaction that all messages in new writer will be linked to
     * @return  a virtual writer.
     * All messages sent from this writer will be linked to transaction provided in this method.
     * Such messages will be considered by server as written only if transaction will be committed.
     */
    AsyncWriter getTransactionWriter(BaseTransaction transaction);

    /**
     * Stops internal threads and makes cleanup in background. Non-blocking
     */
    CompletableFuture<Void> shutdown();
}
