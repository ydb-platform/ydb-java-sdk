package tech.ydb.core.grpc;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.netty.NettyChannelBuilder;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.auth.NopAuthProvider;
import tech.ydb.core.grpc.impl.grpc.GrpcTransportImpl;
import tech.ydb.core.grpc.impl.ydb.YdbTransportImpl;
import tech.ydb.core.utils.Version;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcTransportBuilder {
    private final String endpoint;
    private String database;
    private final List<HostAndPort> hosts;
    private byte[] cert = null;
    private boolean useTLS = false;
    private Consumer<NettyChannelBuilder> channelInitializer = (cb) -> {
    };
    private String localDc;
    private Duration endpointsDiscoveryPeriod = Duration.ofSeconds(60);
    private DiscoveryMode discoveryMode = DiscoveryMode.SYNC;
    private TransportImplType transportImplType = TransportImplType.GRPC_TRANSPORT_IMPL;
    private BalancingSettings balancingSettings;
    private Executor callExecutor = MoreExecutors.directExecutor();
    private AuthProvider authProvider = NopAuthProvider.INSTANCE;
    private long readTimeoutMillis = 0;

    GrpcTransportBuilder(@Nullable String endpoint, @Nullable String database, @Nullable List<HostAndPort> hosts) {
        this.endpoint = endpoint;
        this.database = database;
        this.hosts = hosts;
    }

    @Nullable
    public List<HostAndPort> getHosts() {
        return hosts;
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

    public DiscoveryMode getDiscoveryMode() {
        return discoveryMode;
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

    public GrpcTransportBuilder withDataBase(String dataBase) {
        this.database = dataBase;
        return this;
    }

    public GrpcTransportBuilder withChannelInitializer(Consumer<NettyChannelBuilder> channelInitializer) {
        this.channelInitializer = checkNotNull(channelInitializer, "channelInitializer is null");
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

    public GrpcTransportBuilder withDiscoveryMode(DiscoveryMode discoveryMode) {
        this.discoveryMode = discoveryMode;
        return this;
    }

    public GrpcTransportBuilder withTransportImplType(TransportImplType transportImplType) {
        this.transportImplType = transportImplType;
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
        checkArgument(readTimeoutMillis > 0, "readTimeoutMillis must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withReadTimeout(long timeout, TimeUnit unit) {
        this.readTimeoutMillis = unit.toMillis(timeout);
        checkArgument(readTimeoutMillis > 0, "readTimeoutMillis must be greater than 0");
        return this;
    }

    public GrpcTransportBuilder withCallExecutor(Executor executor) {
        this.callExecutor = Objects.requireNonNull(executor);
        return this;
    }

    public GrpcTransport build() {
        switch (transportImplType) {
            case YDB_TRANSPORT_IMPL:
                return new YdbTransportImpl(this);
            case GRPC_TRANSPORT_IMPL:
            default:
                return new GrpcTransportImpl(this);
        }
    }
}
