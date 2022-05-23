package tech.ydb.core.grpc.impl.ydb;

import java.util.concurrent.TimeUnit;

import tech.ydb.core.grpc.ChannelSettings;
import tech.ydb.core.grpc.YdbHeaders;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikolay Perfilov
 */
class GrpcChannel {
    private static final Logger logger = LoggerFactory.getLogger(GrpcChannel.class);

    public final ManagedChannel realChannel;
    public final Channel channel;
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
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);

        channelSettings.getChannelInitializer().accept(channelBuilder);
        realChannel = channelBuilder.build();
        channel = interceptChannel(realChannel, channelSettings);
    }

    private Channel interceptChannel(ManagedChannel realChannel, ChannelSettings channelSettings) {
        if (channelSettings.getDatabase() == null) {
            return realChannel;
        }

        Metadata extraHeaders = new Metadata();
        extraHeaders.put(YdbHeaders.DATABASE, channelSettings.getDatabase());
        extraHeaders.put(YdbHeaders.BUILD_INFO, channelSettings.getVersion());
        ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(extraHeaders);
        return ClientInterceptors.intercept(realChannel, interceptor);
    }

    public boolean shutdown(long timeoutMs) {
        try {
            boolean closed = realChannel.shutdown()
                    .awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
            if (!closed) {
                logger.warn("closing transport timeout exceeded for channel {}, terminate", endpoint);
                closed = realChannel.shutdownNow()
                        .awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
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
