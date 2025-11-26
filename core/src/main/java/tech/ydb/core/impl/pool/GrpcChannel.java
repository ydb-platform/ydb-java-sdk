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
    private static final long WAIT_FOR_CLOSING_MS = 5000;
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
            boolean closed = channel.shutdown().awaitTermination(WAIT_FOR_CLOSING_MS, TimeUnit.MILLISECONDS);
            if (closed) {
                logger.debug("Grpc channel {} shutdown successfully", endpoint);
            } else {
                logger.warn("Grpc channel {} shutdown timeout exceeded", endpoint);
            }
            return closed;
        } catch (InterruptedException e) {
            logger.warn("transport shutdown interrupted for channel {}: ", endpoint, e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            channel.shutdownNow();
        }
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
