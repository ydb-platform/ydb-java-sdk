package tech.ydb.topic.read;

import java.time.Duration;

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
     * Throws {@link java.util.concurrent.TimeoutException} if timeout runs off
     *
     * @param timeout  timeout to wait a Message with
     * @return {@link Message}
     */
    Message receive(Duration timeout);

    /**
     * Receive a {@link Message}. Blocks until a Message is received.
     *
     * @return {@link Message}
     */
    Message receive();

    /**
     * Stops internal threads and makes cleanup in background. Blocking
     */
    void shutdown();
}
