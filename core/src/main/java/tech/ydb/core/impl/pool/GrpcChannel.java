package tech.ydb.core.impl.pool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.grpc.Channel;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikolay Perfilov
 */
public class GrpcChannel {
    private static final long WAIT_FOR_CLOSING_MS = 1000;
    private static final Logger logger = LoggerFactory.getLogger(GrpcChannel.class);

    private final EndpointRecord endpoint;
    private final ManagedChannel channel;
    private final long connectTimeoutMs;
    private final ReadyWatcher readyWatcher;

    public GrpcChannel(EndpointRecord endpoint, ManagedChannelFactory factory) {
        logger.debug("Creating grpc channel with {}", endpoint);
        this.endpoint = endpoint;
        this.channel = factory.newManagedChannel(endpoint.getHost(), endpoint.getPort());
        this.connectTimeoutMs = factory.getConnectTimeoutMs();
        this.readyWatcher = new ReadyWatcher();
        this.readyWatcher.checkState();
    }

    public EndpointRecord getEndpoint() {
        return this.endpoint;
    }

    public Channel getReadyChannel() {
        return readyWatcher.getReadyChannel();
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

    private class ReadyWatcher implements Runnable {
        private CompletableFuture<ManagedChannel> future = new CompletableFuture<>();

        public Channel getReadyChannel() {
            try {
                return future.get(connectTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                logger.error("Grpc channel {} ready waiting is interrupted: ", endpoint, ex);
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                logger.error("Grpc channel {} connecting problem: ", endpoint, ex);
                throw new RuntimeException("Channel " + endpoint + " connecting problem", ex);
            } catch (TimeoutException ex) {
                logger.error("Grpc channel {} connect timeout exceeded", endpoint);
                throw new RuntimeException("Channel " + endpoint + " connecting timeout");
            }
            return null;
        }

        public void checkState() {
            ConnectivityState state = channel.getState(true);
            logger.debug("Grpc channel {} new state: {}", endpoint, state);
            switch (state) {
                case READY:
                    future.complete(channel);
                    // keep tracking channel state
                    channel.notifyWhenStateChanged(state, this);
                    break;
                case SHUTDOWN:
                    future.completeExceptionally(new IllegalStateException("Grpc channel already closed"));
                    break;
                case TRANSIENT_FAILURE:
                case CONNECTING:
                case IDLE:
                default:
                    if (future.isDone()) {
                        future = new CompletableFuture<>();
                    }
                    // keep tracking channel state
                    channel.notifyWhenStateChanged(state, this);
                    break;
            }
        }

        @Override
        public void run() {
            checkState();
        }
    }
}
