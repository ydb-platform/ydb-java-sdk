package tech.ydb.core.grpc.impl;

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
class GrpcChannel {
    private static final long WAIT_FOR_CLOSING_MS = 1000;
    private static final long WAIT_FOR_CONNECT_MS = 5000;
    private static final Logger logger = LoggerFactory.getLogger(GrpcChannel.class);

    private final String endpoint;
    private final ManagedChannel channel;
    private final ReadyWatcher readyWatcher;
    
    public GrpcChannel(EndpointRecord endpointRecord, ChannelFactory channelFactory, boolean tryToConnect) {
        logger.debug("Creating grpc channel with {}", endpointRecord);
        endpoint = endpointRecord.getHostAndPort();
        channel = channelFactory.newManagedChannel(endpointRecord.getHost(), endpointRecord.getPort());
        readyWatcher = new ReadyWatcher();
        readyWatcher.check(tryToConnect);
    }

    public String getEndpoint() {
        return endpoint;
    }
    
    public Channel getReadyChannel() {
        return readyWatcher.getReadyChannel();
    }

    public boolean shutdown() {
        try {
            boolean closed = channel.shutdown()
                    .awaitTermination(WAIT_FOR_CLOSING_MS, TimeUnit.MILLISECONDS);
            if (!closed) {
                logger.warn("closing transport timeout exceeded for channel {}, terminate", endpoint);
                closed = channel.shutdownNow()
                        .awaitTermination(WAIT_FOR_CLOSING_MS, TimeUnit.MILLISECONDS);
                if (closed) {
                    logger.debug("channel {} shut down successfully", endpoint);
                } else {
                    logger.warn("closing transport problem for channel {}", endpoint);
                }
            }

            return closed;
        } catch (InterruptedException e) {
            logger.warn("transport shutdown interrupted for channel {}: {}", endpoint, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private class ReadyWatcher implements Runnable {
        private final CompletableFuture<ManagedChannel> future = new CompletableFuture<>();

        public Channel getReadyChannel() {
            try {
                return future.get(WAIT_FOR_CONNECT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                logger.error("waiting of channel ready is interrupted", ex);
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                logger.error("Channel {} connecting problem", endpoint, ex);
                throw new RuntimeException("Channel " + endpoint + " connecting problem", ex);
            } catch (TimeoutException ex) {
                logger.error("Channel {} connect timeout excided", endpoint);
                throw new RuntimeException("Channel " + endpoint + " connecting timeout", ex);
            }
            return null;
        }
        
        public void check(boolean tryToConnect) {
            ConnectivityState state = channel.getState(tryToConnect);
            logger.debug("Grpc channel {} new state: {}", endpoint, state);
            switch (state) {
                case READY:
                    future.complete(channel);
                    break;
                case SHUTDOWN:
                    future.completeExceptionally(new IllegalStateException("Grpc channel already closed"));
                    break;
                case TRANSIENT_FAILURE:
                case CONNECTING:
                case IDLE:
                default:
                    // repeat watch
                    channel.notifyWhenStateChanged(state, this);
                    break;
            }
        }

        @Override
        public void run() {
            check(false);
        }
    }
}
