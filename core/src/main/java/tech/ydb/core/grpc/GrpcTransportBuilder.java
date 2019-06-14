package tech.ydb.core.grpc;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.MoreExecutors;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.auth.NopAuthProvider;
import tech.ydb.core.rpc.RpcTransportBuilder;
import io.grpc.netty.NettyChannelBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public final class GrpcTransportBuilder extends RpcTransportBuilder<GrpcTransport, GrpcTransportBuilder> {

    final List<HostAndPort> hosts;
    final String endpoint;
    final String database;
    Executor callExecutor = MoreExecutors.directExecutor();
    Consumer<NettyChannelBuilder> channelInitializer = (cb) -> {};
    AuthProvider authProvider = NopAuthProvider.INSTANCE;
    long readTimeoutMillis = 0;

    private GrpcTransportBuilder(List<HostAndPort> hosts) {
        this.hosts = hosts;
        this.endpoint = null;
        this.database = null;
    }

    private GrpcTransportBuilder(String endpoint, String database) {
        this.hosts = null;
        this.endpoint = endpoint;
        this.database = database;
    }

    public static GrpcTransportBuilder singleHost(String host, int port) {
        return new GrpcTransportBuilder(singletonList(HostAndPort.fromParts(host, port)));
    }

    public static GrpcTransportBuilder multipleHosts(HostAndPort... hosts) {
        checkArgument(hosts.length > 0, "empty hosts array");
        return new GrpcTransportBuilder(Arrays.asList(requireNonNull(hosts, "hosts")));
    }

    public static GrpcTransportBuilder multipleHosts(List<HostAndPort> hosts) {
        checkArgument(!hosts.isEmpty(), "empty hosts list");
        return new GrpcTransportBuilder(requireNonNull(hosts, "hosts"));
    }

    public static GrpcTransportBuilder forEndpoint(String endpoint, String database) {
        return new GrpcTransportBuilder(requireNonNull(endpoint, "endpoint"), requireNonNull(database, "database"));
    }

    public GrpcTransportBuilder withAuthProvider(AuthProvider authProvider) {
        this.authProvider = requireNonNull(authProvider);
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
        this.callExecutor = requireNonNull(executor);
        return this;
    }

    public GrpcTransportBuilder withChannelInitializer(Consumer<NettyChannelBuilder> channelInitializer) {
        this.channelInitializer = requireNonNull(channelInitializer);
        return this;
    }

    @Override
    public GrpcTransport build() {
        return new GrpcTransport(this);
    }
}
