package tech.ydb.topic.read;

import java.time.Duration;

/**
 * @author Nikolay Perfilov
 */
public class ReadSession {

    // Non-blocking
    public void start() {

    }

    public Message receive(Duration timeout) {
        // Temp ----
        return new Message();
        // ---------
    }

    public Message receive() {
        return receive(Duration.ZERO);
    }

    // Non-blocking. Stops threads and makes cleanup
    public void close() {

    }

    public void waitForFinish(Duration timeout) {

    }

    public void waitForFinish() {
        waitForFinish(Duration.ZERO);
    }
}
