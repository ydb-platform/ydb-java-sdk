package tech.ydb.topic.read;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.grpc.ExperimentalApi;

import tech.ydb.common.transaction.BaseTransaction;

/**
 * @author Nikolay Perfilov
 */
@ExperimentalApi("Topic service interfaces are experimental and may change without notice")
public interface SyncReader {

    /**
     * Initialize reading in the background. Non-blocking
     */
    void init();

    /**
     * Initialize internal threads and wait for server init response. Blocking
     */
    void initAndWait();

    /**
     * Receive a {@link Message}. Blocks until a Message is received.
     * Throws {@link java.util.concurrent.TimeoutException} if timeout runs off
     *
     * @param timeout  timeout to wait a Message with
     * @param unit  TimeUnit for timeout
     * @return returns a {@link Message}, or null if the specified waiting time elapses before a message is available
     */
    @Nullable
    Message receive(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Receive a {@link Message}. Blocks until a Message is received.
     *
     * @return {@link Message}
     */
    Message receive() throws InterruptedException;

    /**
     * Get a virtual reader.
     * All messages received from this reader are bind to transaction and will be committed only on transaction commit.
     * @param transaction a transaction that all messages received from new reader will be linked with
     * @return  a virtual reader that links all received messages with provided transaction
     */
    SyncReader getTransactionReader(BaseTransaction transaction);

    /**
     * Stops internal threads and makes cleanup in background. Blocking
     */
    void shutdown();
}
