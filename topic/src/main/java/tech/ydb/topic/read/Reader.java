package tech.ydb.topic.read;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * @author Nikolay Perfilov
 */
public class Reader {

    /**
     * Initialize reading in the background. Non-blocking
     */
    public void start() { }

    /**
     * Receive a {@link Message}. Blocks until a Message is received.
     * Throws {@link java.util.concurrent.TimeoutException} if timeout runs off
     *
     * @param timeout  timeout to wait a Message with
     * @return {@link Message}
     */
    public Message receive(Duration timeout) {
        // Temp ----
        return new Message();
        // ---------
    }

    /**
     * Receive a {@link Message}. Blocks until a Message is received.
     *
     * @return {@link Message}
     */
    public Message receive() {
        return receive(Duration.ZERO);
    }

    /**
     * Stops internal threads and makes cleanup in background. Non-blocking
     */
    public void close() {

    }

    /**
     * Waits until all internal threads are sotpped and cleanup is performed.
     * Throws {@link java.util.concurrent.TimeoutException} if timeout runs off
     *
     * @param timeout  timeout to wait a Message with
     */
    public void waitForFinish(Duration timeout) throws TimeoutException {

    }

    /**
     * Waits until all internal threads are stopped and cleanup is performed.
     */
    public void waitForFinish() {
        try {
            waitForFinish(Duration.ZERO);
        } catch (TimeoutException ignored) {
            throw new RuntimeException("TimeoutException in simple waitForFinish method");
        }
    }
}
