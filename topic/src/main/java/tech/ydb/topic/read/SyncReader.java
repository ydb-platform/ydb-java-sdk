package tech.ydb.topic.read;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import tech.ydb.topic.settings.ReceiveSettings;

/**
 * @author Nikolay Perfilov
 */
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
     *
     * @param settings  settings for receiving a Message
     * @return returns a {@link Message}, or null if the specified timeout time elapses before a message is available
     */
    Message receive(ReceiveSettings settings) throws InterruptedException;

    /**
     * Receive a {@link Message}. Blocks until a Message is received.
     *
     * @param timeout  timeout to wait a Message with
     * @param unit  TimeUnit for timeout
     * @return returns a {@link Message}, or null if the specified waiting time elapses before a message is available
     */
    @Nullable
    default Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        return receive(ReceiveSettings.newBuilder()
                .setTimeout(timeout, unit)
                .build());
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
