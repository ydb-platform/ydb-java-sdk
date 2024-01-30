package tech.ydb.core.grpc;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.auth.NopAuthProvider;
import tech.ydb.core.impl.YdbSchedulerFactory;
import tech.ydb.core.impl.YdbTransportImpl;
import tech.ydb.core.impl.auth.GrpcAuthRpc;
import tech.ydb.core.utils.Version;


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
    private Supplier<ScheduledExecutorService> schedulerFactory = YdbSchedulerFactory::createScheduler;
    private String localDc;
    private BalancingSettings balancingSettings;
    private Executor callExecutor = MoreExecutors.directExecutor();
    private AuthRpcProvider<? super GrpcAuthRpc> authProvider = NopAuthProvider.INSTANCE;
    private long readTimeoutMillis = 0;
    private long connectTimeoutMillis = 30_000;
    private long discoveryTimeoutMillis = 60_000;
    private boolean useDefaultGrpcResolver = false;
    private GrpcCompression compression = GrpcCompression.NO_COMPRESSION;

    /**
     * can cause leaks https://github.com/grpc/grpc-java/issues/9340
     */
    private boolean grpcRetry = false;
    private Long grpcKeepAliveTimeMillis = null;

    GrpcTransportBuilder(@Nullable String endpoint, @Nullable HostAndPort host, @Nonnull String database) {
        this.endpoint = endpoint;
        this.host = host;
        this.database = Objects.requireNonNull(database);
        this.useTLS = endpoint != null && endpoint.startsWith("grpcs://");
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

    public Supplier<ScheduledExecutorService> getSchedulerFactory() {
        return schedulerFactory;
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

    public AuthRpcProvider<? super GrpcAuthRpc> getAuthProvider() {
        return authProvider;
    }

    public long getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public long getDiscoveryTimeoutMillis() {
        return discoveryTimeoutMillis;
    }

    public GrpcCompression getGrpcCompression() {
        return compression;
    }

    public boolean isEnableRetry() {
        return grpcRetry;
    }

    public Long getGrpcKeepAliveTimeMillis() {
        return grpcKeepAliveTimeMillis;
    }

    public boolean useDefaultGrpcResolver() {
        return useDefaultGrpcResolver;
    }

    public GrpcTransportBuilder withChannelInitializer(Consumer<NettyChannelBuilder> channelInitializer) {
        this.channelInitializer = Objects.requireNonNull(channelInitializer, "channelInitializer is null");
        return this;
    }

    /**
     * use {@link GrpcTransportBuilder#withBalancingSettings(tech.ydb.core.grpc.BalancingSettings) } instead
     * @param dc preferable location
     * @return this
     * @deprecated
     */
    @Deprecated
    public GrpcTransportBuilder withLocalDataCenter(String dc) {
        this.localDc = dc;
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

    public GrpcTransportBuilder withAuthProvider(AuthRpcProvider<? super GrpcAuthRpc> authProvider) {
        this.authProvider = Objects.requireNonNull(authProvider);
        return this;
    }

    /**
     * Sets the compression to use for the calls. See {@link io.grpc.CallOptions#withCompression(java.lang.String) }
     * for details
     * @param compression the compression value
     * @return return GrpcTransportBuilder with the given compression
     */
    public GrpcTransportBuilder withGrpcCompression(@Nonnull GrpcCompression compression) {
        this.compression = Objects.requireNonNull(compression, "compression is null");
        return this;
    }

    /**
     * use tech.ydb.table.settings.RequestSettings#setTimeout(java.time.Duration) instead
     * @param timeout global timeout for grpc calls
     * @return this
     * @deprecated
     */
    @Deprecated
    public GrpcTransportBuilder withReadTimeout(Duration timeout) {
        this.readTimeoutMillis = timeout.toMillis();
        Preconditions.checkArgument(readTimeoutMillis > 0, "readTimeoutMillis must be greater than 0");
        return this;
    }

    /**
     * use tech.ydb.table.settings.RequestSettings#setTimeout(long, java.time.TimeUnit) instead
     * @param timeout size of global timeout for grpc calls
     * @param unit time unit of global timeout for grpc calls
     * @return this
     * @deprecated
     */
    @Deprecated
    public GrpcTransportBuilder withReadTimeout(long timeout, TimeUnit unit) {
        this.readTimeoutMillis = unit.toMillis(timeout);
        Preconditions.checkArgument(readTimeoutMillis > 0, "readTimeoutMillis must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withConnectTimeout(Duration timeout) {
        this.connectTimeoutMillis = timeout.toMillis();
        Preconditions.checkArgument(connectTimeoutMillis > 0, "connectTimeoutMillis must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withConnectTimeout(long timeout, TimeUnit unit) {
        this.connectTimeoutMillis = unit.toMillis(timeout);
        Preconditions.checkArgument(connectTimeoutMillis > 0, "connectTimeoutMillis must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withDiscoveryTimeout(Duration timeout) {
        this.discoveryTimeoutMillis = timeout.toMillis();
        Preconditions.checkArgument(discoveryTimeoutMillis > 0, "discoveryTimeoutMillis must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withDiscoveryTimeout(long timeout, TimeUnit unit) {
        this.discoveryTimeoutMillis = unit.toMillis(timeout);
        Preconditions.checkArgument(discoveryTimeoutMillis > 0, "discoveryTimeoutMillis must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withGrpcKeepAliveTime(Duration time) {
        this.grpcKeepAliveTimeMillis = time.toMillis();
        Preconditions.checkArgument(grpcKeepAliveTimeMillis > 0, "grpcKeepAliveTime must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withGrpcKeepAliveTime(long time, TimeUnit unit) {
        this.grpcKeepAliveTimeMillis = unit.toMillis(time);
        Preconditions.checkArgument(grpcKeepAliveTimeMillis > 0, "grpcKeepAliveTime must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withCallExecutor(Executor executor) {
        this.callExecutor = Objects.requireNonNull(executor);
        return this;
    }

    public GrpcTransportBuilder withGrpcRetry(boolean enabled) {
        this.grpcRetry = enabled;
        return this;
    }

    public GrpcTransportBuilder withUseDefaultGrpcResolver(boolean use) {
        this.useDefaultGrpcResolver = use;
        return this;
    }

    public GrpcTransportBuilder withSchedulerFactory(Supplier<ScheduledExecutorService> factory) {
        this.schedulerFactory = Objects.requireNonNull(factory, "schedulerFactory is null");
        return this;
    }

    /**
     * use {@link GrpcTransportBuilder#withGrpcRetry(boolean) } instead
     * @return this
     * @deprecated
     */
    @Deprecated
    public GrpcTransportBuilder enableRetry() {
        this.grpcRetry = true;
        return this;
    }

    /**
     * use {@link GrpcTransportBuilder#withGrpcRetry(boolean) } instead
     * @return this
     * @deprecated
     */
    @Deprecated
    public GrpcTransportBuilder disableRetry() {
        this.grpcRetry = false;
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

    public GrpcTransport buildAsync(Runnable ready) {
        YdbTransportImpl impl = new YdbTransportImpl(this);
        impl.initAsync(ready);
        return impl;
    }
}
