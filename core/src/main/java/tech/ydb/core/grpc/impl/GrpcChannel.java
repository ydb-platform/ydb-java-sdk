package tech.ydb.core.grpc.impl;

import java.util.concurrent.TimeUnit;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikolay Perfilov
 */
class GrpcChannel {
    private static final long WAIT_FOR_CLOSING_MS = 1000;
    private static final Logger logger = LoggerFactory.getLogger(GrpcChannel.class);

    private final ManagedChannel channel;
    private final String endpoint;
    
    public GrpcChannel(EndpointRecord endpoint, ChannelSettings channelSettings) {
        this.endpoint = endpoint.getHostAndPort();
        logger.debug("Creating grpc channel, endpoint: " + endpoint.getHost() + ", port: " + endpoint.getPort());
        final NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(
            endpoint.getHost(),
            endpoint.getPort());

        channelSettings.configureSecureConnection(channelBuilder);

        channelBuilder
                .maxInboundMessageSize(64 << 20) // 64 MiB
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                .intercept(channelSettings.metadataInterceptor());
        channelSettings.getChannelInitializer().accept(channelBuilder);
        channel = channelBuilder.build();
    }

    public String getEndpoint() {
        return endpoint;
    }
    
    public Channel getGrpcChannel() {
        return channel;
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
}
