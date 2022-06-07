package tech.ydb.core.grpc;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import tech.ydb.core.ssl.YandexTrustManagerFactory;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * @author Nikolay Perfilov
 */
public class ChannelSettings {
    private final String database;
    private final String version;
    private final Consumer<NettyChannelBuilder> channelInitializer;
    private final boolean useTLS;
    private final byte[] cert;

    private ChannelSettings(GrpcTransportBuilder builder) {
        this.database = builder.getDatabase();
        this.version = builder.getVersionString();
        this.channelInitializer = builder.getChannelInitializer();
        this.useTLS = builder.getUseTls();
        this.cert = builder.getCert();
    }

    public String getDatabase() {
        return database;
    }

    public String getVersion() {
        return version;
    }

    public Consumer<NettyChannelBuilder> getChannelInitializer() {
        return channelInitializer;
    }

    public byte[] getCert() {
        return cert;
    }

    public boolean isUseTLS() {
        return useTLS;
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

    public void configureSecureConnection(NettyChannelBuilder channelBuilder) {
        if (useTLS) {
            channelBuilder
                    .negotiationType(NegotiationType.TLS)
                    .sslContext(createSslContext());
        } else {
            channelBuilder.negotiationType(NegotiationType.PLAINTEXT);
        }
    }

    public static ChannelSettings fromBuilder(GrpcTransportBuilder builder) {
        return new ChannelSettings(builder);
    }
}
