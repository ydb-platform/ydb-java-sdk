package tech.ydb.topic.write;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.grpc.ExperimentalApi;

import tech.ydb.common.transaction.BaseTransaction;


/**
 * @author Nikolay Perfilov
 */
@ExperimentalApi("Topic service interfaces are experimental and may change without notice")
public interface SyncWriter {

    /**
     * Initialize internal threads in the background. Non-blocking
     */
    void init();

    /**
     * Initialize internal threads and wait for server init response. Blocking
     * @return  {@link InitResult} containing initialization data like lastSeqNo
     */
    InitResult initAndWait();

    /**
     * Send message. Blocks infinitely until the message is put into sending buffer.
     * @param message message data to write
     */
    void send(Message message);

    /**
     * Send message. Blocks until the message is put into sending buffer.
     * If in-flight or memory usage limits is reached, waits until timeout expires and then
     * throws {@link TimeoutException} if message was not put into queue
     * @param message message data to write
     * @param timeout timeout to wait until message is punt into sending buffer
     * @param unit {@link TimeUnit} for timeout
     */
    void send(Message message, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Get a virtual writer that writes all messages in provided transaction.
     * Such messages will be written only in case of transaction commit
     * @param transaction a transaction that all messages in new writer will be linked to
     * @return  a virtual writer.
     * All messages sent from this writer will be linked to transaction provided in this method.
     * Such messages will be considered by server as written only if transaction will be committed.
     */
    SyncWriter getTransactionWriter(BaseTransaction transaction);

    /**
     * Waits until all current writes will be sent to server and response will be received. Blocking
     */
    void flush();

    /**
     * Stops internal threads and makes cleanup in background. Blocking
     */
    void shutdown(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
