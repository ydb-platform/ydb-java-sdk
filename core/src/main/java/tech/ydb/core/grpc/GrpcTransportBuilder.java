package tech.ydb.core.grpc;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.auth.NopAuthProvider;
import tech.ydb.core.grpc.impl.YdbTransportImpl;
import tech.ydb.core.utils.Version;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.netty.NettyChannelBuilder;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcTransportBuilder {
    private final String endpoint;
    private final HostAndPort host;
    private final String database;

    private byte[] cert = null;
    private boolean useTLS = false;
    private Consumer<NettyChannelBuilder> channelInitializer = null;
    private String localDc;
    private Duration endpointsDiscoveryPeriod = Duration.ofSeconds(60);
    private BalancingSettings balancingSettings;
    private Executor callExecutor = MoreExecutors.directExecutor();
    private AuthProvider authProvider = NopAuthProvider.INSTANCE;
    private long readTimeoutMillis = 0;

    /**
     * can cause leaks https://github.com/grpc/grpc-java/issues/9340
     */
    private boolean enableRetry = false;

    GrpcTransportBuilder(@Nullable String endpoint, @Nullable HostAndPort host, @Nonnull String database) {
        this.endpoint = endpoint;
        this.host = host;
        this.database = Objects.requireNonNull(database);
    }

    @Nullable
    public HostAndPort getHost() {
        return host;
    }

    @Nullable
    public byte[] getCert() {
        return cert;
    }

    public boolean getUseTls() {
        return useTLS;
    }

    @Nullable
    public String getEndpoint() {
        return endpoint;
    }

    public Duration getEndpointsDiscoveryPeriod() {
        return endpointsDiscoveryPeriod;
    }

    @Nullable
    public String getDatabase() {
        return database;
    }

    public String getVersionString() {
        return Version.getVersion()
                .map(version -> "ydb-java-sdk/" + version)
                .orElse("unknown-version");
    }

    public Consumer<NettyChannelBuilder> getChannelInitializer() {
        return channelInitializer;
    }

    public String getLocalDc() {
        return localDc;
    }

    public BalancingSettings getBalancingSettings() {
        return balancingSettings;
    }

    public Executor getCallExecutor() {
        return callExecutor;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public long getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public boolean isEnableRetry() {
        return enableRetry;
    }

    public GrpcTransportBuilder withChannelInitializer(Consumer<NettyChannelBuilder> channelInitializer) {
        this.channelInitializer = Objects.requireNonNull(channelInitializer, "channelInitializer is null");
        return this;
    }

    public GrpcTransportBuilder withLocalDataCenter(String dc) {
        this.localDc = dc;
        return this;
    }

    public GrpcTransportBuilder withEndpointsDiscoveryPeriod(Duration period) {
        this.endpointsDiscoveryPeriod = period;
        return this;
    }

    public GrpcTransportBuilder withSecureConnection(byte[] cert) {
        this.cert = cert.clone();
        this.useTLS = true;
        return this;
    }

    public GrpcTransportBuilder withSecureConnection() {
        this.useTLS = true;
        return this;
    }

    public GrpcTransportBuilder withBalancingSettings(BalancingSettings balancingSettings) {
        this.balancingSettings = balancingSettings;
        return this;
    }

    public GrpcTransportBuilder withAuthProvider(AuthProvider authProvider) {
        this.authProvider = Objects.requireNonNull(authProvider);
        return this;
    }

    public GrpcTransportBuilder withReadTimeout(Duration timeout) {
        this.readTimeoutMillis = timeout.toMillis();
        Preconditions.checkArgument(readTimeoutMillis > 0, "readTimeoutMillis must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withReadTimeout(long timeout, TimeUnit unit) {
        this.readTimeoutMillis = unit.toMillis(timeout);
        Preconditions.checkArgument(readTimeoutMillis > 0, "readTimeoutMillis must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withCallExecutor(Executor executor) {
        this.callExecutor = Objects.requireNonNull(executor);
        return this;
    }

    public GrpcTransportBuilder enableRetry() {
        this.enableRetry = true;
        return this;
    }

    public GrpcTransportBuilder disableRetry() {
        this.enableRetry = false;
        return this;
    }

    public GrpcTransport build() {
        YdbTransportImpl impl = new YdbTransportImpl(this);
        try {
            impl.init();
            return impl;
        } catch (RuntimeException ex) {
            impl.close();
            throw ex;
        }
    }
}
