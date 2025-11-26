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
     * Return identifier of read session.
     * @return current read session identifier or null if session has not started yet
     */
    @Nullable
    String getSessionId();

    /**
     * Receive a {@link Message}.Blocks until a Message is received.
     *
     * @param settings  settings for receiving a Message
     * @return returns a {@link Message}, or null if the specified timeout time elapses before a message is available
     * @throws java.lang.InterruptedException if current thread was interrupted
     */
    Message receive(ReceiveSettings settings) throws InterruptedException;

    /**
     * Receive a {@link Message}. Blocks until a Message is received.
     *
     * @param timeout  timeout to wait a Message with
     * @param unit  TimeUnit for timeout
     * @return returns a {@link Message}, or null if the specified waiting time elapses before a message is available
     * @throws java.lang.InterruptedException if current thread was interrupted
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
     * @throws java.lang.InterruptedException if current thread was interrupted
     */
    default Message receive() throws InterruptedException {
        return receive(ReceiveSettings.newBuilder().build());
    }

    /**
     * Stops internal threads and makes cleanup in background. Blocking
     */
    void shutdown();
}
