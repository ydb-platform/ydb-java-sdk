package tech.ydb.core.impl.pool;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.buffer.ByteBufAllocator;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.stub.MetadataUtils;

import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.core.ssl.YandexTrustManagerFactory;

/**
 * @author Nikolay Perfilov
 * @author Aleksandr Gorshenin
 */
public class ManagedChannelFactory {

    static final int INBOUND_MESSAGE_SIZE = 64 << 20; // 64 MiB
    static final String DEFAULT_BALANCER_POLICY = "round_robin";

    private final String database;
    private final String version;
    private final Consumer<NettyChannelBuilder> channelInitializer;
    private final boolean useTLS;
    private final byte[] cert;
    private final boolean retryEnabled;
    private final long connectTimeoutMs;
    private final boolean useDefaultGrpcResolver;

    private ManagedChannelFactory(GrpcTransportBuilder builder) {
        this.database = builder.getDatabase();
        this.version = builder.getVersionString();
        this.channelInitializer = builder.getChannelInitializer();
        this.useTLS = builder.getUseTls();
        this.cert = builder.getCert();
        this.retryEnabled = builder.isEnableRetry();
        this.connectTimeoutMs = builder.getConnectTimeoutMillis();
        this.useDefaultGrpcResolver = builder.useDefaultGrpcResolver();
    }

    public long getConnectTimeoutMs() {
        return this.connectTimeoutMs;
    }

    @SuppressWarnings("deprecation")
    public ManagedChannel newManagedChannel(String host, int port) {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(host, port);

        if (useTLS) {
            channelBuilder
                    .negotiationType(NegotiationType.TLS)
                    .sslContext(createSslContext());
        } else {
            channelBuilder.negotiationType(NegotiationType.PLAINTEXT);
        }

        channelBuilder
                .maxInboundMessageSize(INBOUND_MESSAGE_SIZE)
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                .intercept(metadataInterceptor());

        if (!useDefaultGrpcResolver) {
            // force usage of dns resolver and round_robin balancer
            channelBuilder
                    .nameResolverFactory(new DnsNameResolverProvider())
                    .defaultLoadBalancingPolicy(DEFAULT_BALANCER_POLICY);
        }

        if (channelInitializer != null) {
            channelInitializer.accept(channelBuilder);
        }

        if (retryEnabled) {
            channelBuilder.enableRetry();
        } else {
            channelBuilder.disableRetry();
        }

        return channelBuilder.build();
    }

    private ClientInterceptor metadataInterceptor() {
        Metadata extraHeaders = new Metadata();
        extraHeaders.put(YdbHeaders.DATABASE, database);
        extraHeaders.put(YdbHeaders.BUILD_INFO, version);
        return MetadataUtils.newAttachHeadersInterceptor(extraHeaders);
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

    public static ManagedChannelFactory fromBuilder(GrpcTransportBuilder builder) {
        return new ManagedChannelFactory(builder);
    }
}
