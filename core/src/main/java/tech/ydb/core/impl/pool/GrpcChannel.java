package tech.ydb.core.impl.pool;

import java.util.concurrent.TimeUnit;

import io.grpc.Channel;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikolay Perfilov
 */
public final class GrpcChannel implements Runnable {

    /* Channel shutdown waits for finish of active grpc calls, so there must be enough time to complete them all */
    static final long WAIT_FOR_CLOSING_MS = 5000;
    private static final Logger logger = LoggerFactory.getLogger(GrpcChannel.class);

    private final EndpointRecord endpoint;
    private final ManagedChannel channel;

    public GrpcChannel(EndpointRecord endpoint, ManagedChannelFactory factory) {
        try {
            logger.debug("Creating grpc channel with {}", endpoint);
            this.endpoint = endpoint;
            this.channel = factory.newManagedChannel(endpoint.getHost(), endpoint.getPort(), endpoint.getAuthority());
            checkState();
        } catch (Throwable th) {
            throw new RuntimeException("cannot create channel", th);
        }
    }

    public EndpointRecord getEndpoint() {
        return this.endpoint;
    }

    public Channel getReadyChannel() {
        return channel;
    }

    public boolean isShutdown() {
        return channel.isShutdown();
    }

    public boolean shutdown() {
        if (isShutdown()) {
            return true;
        }

        try {
            initShutdown();
            return awaitTermination(closingDeadline());
        } catch (InterruptedException e) {
            logger.warn("transport shutdown interrupted for channel {}: ", endpoint, e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            shutdownNow();
        }
    }

    /**
     * @return the {@link System#nanoTime()} based deadline for the termination of channels shut down now
     */
    static long closingDeadline() {
        return System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_FOR_CLOSING_MS);
    }

    /**
     * Initiates an orderly shutdown: new calls are rejected, but the active ones are allowed to complete.
     */
    void initShutdown() {
        channel.shutdown();
    }

    /**
     * Waits for the termination of the channel after {@link #initShutdown()}.
     *
     * @param deadlineNanos the {@link System#nanoTime()} based deadline of waiting
     * @return true if the channel is terminated
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination(long deadlineNanos) throws InterruptedException {
        long timeoutNanos = Math.max(deadlineNanos - System.nanoTime(), 0);
        boolean closed = channel.awaitTermination(timeoutNanos, TimeUnit.NANOSECONDS);

        if (closed) {
            logger.debug("gRPC channel {} was shut down successfully", endpoint);
        } else {
            logger.warn("gRPC channel {} did not terminate before the shutdown deadline", endpoint);
        }

        return closed;
    }

    /**
     * Forcefully shuts down the channel, the active calls are cancelled.
     */
    void shutdownNow() {
        channel.shutdownNow();
    }

    private void checkState() {
        ConnectivityState state = channel.getState(true);
        logger.debug("Grpc channel {} new state: {}", endpoint, state);
        channel.notifyWhenStateChanged(state, this);
    }

    @Override
    public void run() {
        checkState();
    }
}
