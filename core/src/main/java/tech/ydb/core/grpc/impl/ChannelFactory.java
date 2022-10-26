package tech.ydb.core.grpc.impl;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
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
public class ChannelFactory {
    private final String database;
    private final String version;
    private final Consumer<NettyChannelBuilder> channelInitializer;
    private final boolean useTLS;
    private final byte[] cert;
    private final boolean retryEnabled;

    private ChannelFactory(GrpcTransportBuilder builder) {
        this.database = builder.getDatabase();
        this.version = builder.getVersionString();
        this.channelInitializer = builder.getChannelInitializer();
        this.useTLS = builder.getUseTls();
        this.cert = builder.getCert();
        this.retryEnabled = builder.isEnableRetry();
    }

    public String getDatabase() {
        return database;
    }

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
                .maxInboundMessageSize(64 << 20) // 64 MiB
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                .intercept(metadataInterceptor());

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
        if (database != null) {
            extraHeaders.put(YdbHeaders.DATABASE, database);
        }
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
        } catch (SSLException e) {
            throw new RuntimeException("cannot create ssl context", e);
        }
    }

    public static ChannelFactory fromBuilder(GrpcTransportBuilder builder) {
        return new ChannelFactory(builder);
    }
}
