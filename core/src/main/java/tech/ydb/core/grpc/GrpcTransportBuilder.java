package tech.ydb.core.grpc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.auth.NopAuthProvider;
import tech.ydb.core.impl.YdbSchedulerFactory;
import tech.ydb.core.impl.YdbTransportImpl;
import tech.ydb.core.impl.auth.GrpcAuthRpc;
import tech.ydb.core.impl.pool.ChannelFactoryLoader;
import tech.ydb.core.impl.pool.ManagedChannelFactory;
import tech.ydb.core.tracing.NoopTracer;
import tech.ydb.core.tracing.Tracer;
import tech.ydb.core.utils.Version;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcTransportBuilder {
    /**
     * The initialization mode defines the behavior of {@link tech.ydb.core.grpc.GrpcTransportBuilder#build() }
     * method.
     */
    public enum InitMode {
        /**
         * In synchronous mode, transport creation will wait for successful discovery of current database nodes. Any
         * errors on discovery execution like an authentication error or a network issue will be thrown as
         * RuntimeException. It allows to catch configuration problems and stops the transport creating.
         */
        SYNC,
        /**
         * In asynchronous mode, transport creation will not be blocked while waiting for discovery response and will
         * not throw any exceptions in case of configuration problems. But any request with the transport will wait for
         * the discovery and may throw an exception if it will not be completed. This mode allows the application not
         * to be blocked during the transport initialization. Any user request on this transport will wait for
         * initialization completion before being sent to the server
         */
        ASYNC,

        /**
         * In fallback asynchronous mode, neither transport creation nor user request execution will be blocked while
         * initial discovery is in progress. In this case if the discovery is not completed, all requests will be sent
         * to the discovery endpoint. Any discovery problems will be ignored. This mode allows to start working with the
         * database without waiting for discovery to complete, but after its completion, existing long-running
         * operations (like grpc streams) will be interrupted.
         * Thus, this mode is not recommended for long-running streams such as topic reading/writing.
         */
        ASYNC_FALLBACK
    }

    private final String endpoint;
    private final HostAndPort host;
    private final String database;

    private byte[] cert = null;
    private boolean useTLS = false;
    private String applicationName = null;
    private String clientProcessId = null;
    private ManagedChannelFactory.Builder channelFactoryBuilder = null;
    private final List<Consumer<? super ManagedChannelBuilder<?>>> channelInitializers = new ArrayList<>();
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
    private InitMode initMode = InitMode.SYNC;
    private Tracer tracer = NoopTracer.getInstance();

    /**
     * can cause leaks https://github.com/grpc/grpc-java/issues/9340
     */
    private boolean grpcRetry = false;
    private Long grpcKeepAliveTimeMillis = 10_000L;

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
                .orElse(Version.UNKNOWN_VERSION);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getClientProcessId() {
        return clientProcessId;
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

    public InitMode getInitMode() {
        return initMode;
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

    public Tracer getTracer() {
        return tracer;
    }

    public ManagedChannelFactory getManagedChannelFactory() {
        if (channelFactoryBuilder == null) {
            channelFactoryBuilder = ChannelFactoryLoader.load();
        }

        return channelFactoryBuilder.buildFactory(this);
    }

    public List<Consumer<? super ManagedChannelBuilder<?>>> getChannelInitializers() {
        return this.channelInitializers;
    }

    /**
     * Set a custom factory of {@link ManagedChannel}. This option must be used only if you want to configure
     * grpc channels in a special way.
     *
     * @param channelFactoryBuilder ManagerChannelFactory builder
     * @return this
     */
    public GrpcTransportBuilder withChannelFactoryBuilder(ManagedChannelFactory.Builder channelFactoryBuilder) {
        this.channelFactoryBuilder = Objects.requireNonNull(channelFactoryBuilder, "Channel factory must be not null");
        return this;
    }

    /**
     * Add a custom initialization of {@link ManagedChannelBuilder}
     *
     * @param ci custom ManagedChannelBuilder initializer
     * @return this
     */
    public GrpcTransportBuilder addChannelInitializer(Consumer<? super ManagedChannelBuilder<?>> ci) {
        channelInitializers.add(ci);
        return this;
    }

    /**
     * Set a custom initialization of {@link io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder} <br>
     * This method is deprecated. Use
     * {@link GrpcTransportBuilder#withChannelFactoryBuilder(tech.ydb.core.impl.pool.ManagedChannelFactory.Builder)}
     * instead
     *
     * @param ci custom NettyChannelBuilder initializer
     * @return this
     * @deprecated
     */
    @Deprecated
    public GrpcTransportBuilder withChannelInitializer(
            Consumer<io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder> ci
    ) {
        this.channelFactoryBuilder = tech.ydb.core.impl.pool.ShadedNettyChannelFactory.withInterceptor(ci);
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

    public GrpcTransportBuilder withApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    public GrpcTransportBuilder withClientProcessId(String clientProcessId) {
        this.clientProcessId = clientProcessId;
        return this;
    }

    public GrpcTransportBuilder withBalancingSettings(BalancingSettings balancingSettings) {
        this.balancingSettings = balancingSettings;
        return this;
    }

    /**
     * Set GrpcTransport's init mode.
     * See {@link tech.ydb.core.grpc.GrpcTransportBuilder.InitMode } for details
     *
     * @param initMode mode of transport initialization
     * @return GrpcTransportBuilder with the given initMode
     */
    public GrpcTransportBuilder withInitMode(InitMode initMode) {
        this.initMode = initMode;
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
     * @return GrpcTransportBuilder with the given compression
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
        if (time == null) {
            this.grpcKeepAliveTimeMillis = null;
        } else {
            this.grpcKeepAliveTimeMillis = time.toMillis();
            Preconditions.checkArgument(grpcKeepAliveTimeMillis > 0, "grpcKeepAliveTime must be greater than 0");
        }
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
     * Use the provided {@link ScheduledExecutorService} to schedule internal retries.
     * The SDK does not manage its lifecycle and will not shut it down.
     *
     * @param scheduler scheduler instance
     * @return this builder instance
     */
    public GrpcTransportBuilder withScheduler(ScheduledExecutorService scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        this.schedulerFactory = () -> YdbSchedulerFactory.wrapExternal(scheduler);
        return this;
    }

    /**
     * Configures tracing implementation used by higher-level SDK operations.
     *
     * @param tracer tracing facade implementation
     * @return this builder instance
     */
    public GrpcTransportBuilder withTracer(Tracer tracer) {
        this.tracer = Objects.requireNonNull(tracer, "tracer is null");
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
            impl.start(initMode);
            return impl;
        } catch (RuntimeException ex) {
            impl.close();
            throw ex;
        }
    }

    @Deprecated
    public GrpcTransport buildAsync(Runnable ready) {
        YdbTransportImpl impl = new YdbTransportImpl(this);
        try {
            impl.startAsync(ready);
            return impl;
        } catch (RuntimeException ex) {
            impl.close();
            throw ex;
        }
    }
}
