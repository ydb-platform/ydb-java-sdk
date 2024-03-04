package tech.ydb.topic.read;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.grpc.ExperimentalApi;

import tech.ydb.topic.settings.ReceiveSettings;

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
     * @param settings  settings for receiving a Message
     * @param timeout  timeout to wait a Message with
     * @param unit  TimeUnit for timeout
     * @return returns a {@link Message}, or null if the specified waiting time elapses before a message is available
     */
    @Nullable
    Message receive(ReceiveSettings settings, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Receive a {@link Message}. Blocks until a Message is received.
     *
     * @param settings  settings for receiving a Message
     * @return {@link Message}
     */
    Message receive(ReceiveSettings settings) throws InterruptedException;

    /**
     * Receive a {@link Message}. Blocks until a Message is received.
     * Throws {@link java.util.concurrent.TimeoutException} if timeout runs off
     *
     * @param timeout  timeout to wait a Message with
     * @param unit  TimeUnit for timeout
     * @return returns a {@link Message}, or null if the specified waiting time elapses before a message is available
     */
    @Nullable
    default Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        return receive(ReceiveSettings.newBuilder().build(), timeout, unit);
    }

    /**
     * Receive a {@link Message}. Blocks until a Message is received.
     *
     * @return {@link Message}
     */
    default Message receive() throws InterruptedException {
        return receive(ReceiveSettings.newBuilder().build());
    }

    /**
     * Stops internal threads and makes cleanup in background. Blocking
     */
    void shutdown();
}
