package tech.ydb.topic.write;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Blocking lifecycle/barrier adapter over the same asynchronous multiwriter.
 * Sending returns after admission and does not wait for an ACK. Buffer exhaustion is reported
 * immediately; use flush to wait for accepted messages and observe asynchronous delivery errors.
 */
public final class SyncMultiWriter {
    private final AsyncMultiWriter writer;

    public SyncMultiWriter(AsyncMultiWriter writer) {
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    /** Starts initialization without waiting for the server. */
    public void init() {
        writer.init();
    }

    /**
     * Waits for topology and producer history initialization.
     * @return bootstrap sequence number
     */
    public InitResult initAndWait() {
        return writer.init().join();
    }

    /**
     * Admits a message without waiting for acknowledgement or available capacity.
     * @param message immutable input
     * @throws QueueOverflowException if the message was not admitted
     */
    public void send(MultiMessage message) throws QueueOverflowException {
        writer.send(message);
    }

    /** Waits for all outcomes before the flush barrier and propagates delivery failures. */
    public void flush() {
        writer.flush().join();
    }

    /** Waits for the writer's configured shutdown deadline and propagates failure. */
    public void shutdown() {
        writer.shutdown().join();
    }

    /**
     * Waits at most the supplied duration for shutdown. A timeout stops only this wait.
     * @param timeout maximum wait
     * @param unit wait unit
     * @throws InterruptedException if interrupted
     * @throws ExecutionException if shutdown fails
     * @throws TimeoutException if the wait expires
     */
    public void shutdown(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        writer.shutdown().get(timeout, unit);
    }
}
