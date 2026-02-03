package tech.ydb.core.impl.pool;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.core.ssl.YandexTrustManagerFactory;

/**
 * @author Nikolay Perfilov
 * @author Aleksandr Gorshenin
 */
public class NettyChannelFactory implements ManagedChannelFactory {
    static final int INBOUND_MESSAGE_SIZE = 64 << 20; // 64 MiB
    static final String DEFAULT_BALANCER_POLICY = "round_robin";

    private final ClientInterceptor metadata;

    private final boolean useTLS;
    private final byte[] cert;
    private final boolean retryEnabled;
    private final boolean useDefaultGrpcResolver;
    private final Long grpcKeepAliveTimeMillis;
    private final List<Consumer<? super ManagedChannelBuilder<?>>> initializers;

    private NettyChannelFactory(GrpcTransportBuilder builder) {
        this.metadata = YdbHeaders.createMetadataInterceptor(builder);
        this.useTLS = builder.getUseTls();
        this.cert = builder.getCert();
        this.retryEnabled = builder.isEnableRetry();
        this.useDefaultGrpcResolver = builder.useDefaultGrpcResolver();
        this.grpcKeepAliveTimeMillis = builder.getGrpcKeepAliveTimeMillis();
        this.initializers = builder.getChannelInitializers();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ManagedChannel newManagedChannel(String host, int port, String sslHostOverride) {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(host, port);

        if (useTLS) {
            channelBuilder
                    .negotiationType(NegotiationType.TLS)
                    .sslContext(createSslContext());
            if (sslHostOverride != null) {
                channelBuilder.overrideAuthority(sslHostOverride);
            }
        } else {
            channelBuilder.negotiationType(NegotiationType.PLAINTEXT);
        }

        channelBuilder
                .maxInboundMessageSize(INBOUND_MESSAGE_SIZE)
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                .withOption(ChannelOption.TCP_NODELAY, true)
                .intercept(metadata);

        if (!useDefaultGrpcResolver) {
            // force usage of dns resolver and round_robin balancer
            channelBuilder
                    .nameResolverFactory(new DnsNameResolverProvider())
                    .defaultLoadBalancingPolicy(DEFAULT_BALANCER_POLICY);
        }

        if (grpcKeepAliveTimeMillis != null) {
            channelBuilder.keepAliveTime(grpcKeepAliveTimeMillis, TimeUnit.MILLISECONDS)
                .keepAliveWithoutCalls(true);
        }

        if (retryEnabled) {
            channelBuilder.enableRetry();
        } else {
            channelBuilder.disableRetry();
        }

        for (Consumer<? super ManagedChannelBuilder<?>> initializer: initializers) {
            initializer.accept(channelBuilder);
        }

        configure(channelBuilder);

        return channelBuilder.build();
    }

    protected void configure(NettyChannelBuilder channelBuilder) {

    }

    private SslContext createSslContext() {
        try {
            SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();
            if (cert != null) {
                sslContextBuilder.trustManager(new ByteArrayInputStream(cert));
            } else {
                sslContextBuilder.trustManager(new YandexTrustManagerFactory(""));
            }
            return sslContextBuilder.build();
        } catch (SSLException | RuntimeException e) {
            throw new RuntimeException("cannot create ssl context", e);
        }
    }

    public static ManagedChannelFactory.Builder build() {
        return new Builder() {
            @Override
            public ManagedChannelFactory buildFactory(GrpcTransportBuilder builder) {
                return new NettyChannelFactory(builder);
            }

            @Override
            public String toString() {
                return "NettyChannelFactory";
            }
        };
    }

    public static ManagedChannelFactory.Builder withInterceptor(Consumer<NettyChannelBuilder> ci) {
        return builder -> new NettyChannelFactory(builder) {
            @Override
            protected void configure(NettyChannelBuilder channelBuilder) {
                if (ci != null) {
                    ci.accept(channelBuilder);
                }
            }
        };
    }
}
