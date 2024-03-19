package tech.ydb.topic.write;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.grpc.ExperimentalApi;
import tech.ydb.topic.settings.SendSettings;


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
     * @param settings send settings
     */
    void send(Message message, SendSettings settings);

    /**
     * Send message. Blocks until the message is put into sending buffer.
     * If in-flight or memory usage limits is reached, waits until timeout expires and then
     * throws {@link TimeoutException} if message was not put into queue
     * @param message message data to write
     * @param settings send settings
     * @param timeout timeout to wait until message is punt into sending buffer
     * @param unit {@link TimeUnit} for timeout
     */
    void send(Message message, SendSettings settings, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Send message. Blocks infinitely until the message is put into sending buffer.
     * @param message message data to write
     */
    default void send(Message message) {
        send(message, SendSettings.newBuilder().build());
    }

    /**
     * Send message. Blocks until the message is put into sending buffer.
     * If in-flight or memory usage limits is reached, waits until timeout expires and then
     * throws {@link TimeoutException} if message was not put into queue
     * @param message message data to write
     * @param timeout timeout to wait until message is punt into sending buffer
     * @param unit {@link TimeUnit} for timeout
     */
    default void send(Message message, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        send(message, SendSettings.newBuilder().build(), timeout, unit);
    }

    /**
     * Waits until all current writes will be sent to server and response will be received. Blocking
     */
    void flush();

    /**
     * Stops internal threads and makes cleanup in background. Blocking
     */
    void shutdown(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
