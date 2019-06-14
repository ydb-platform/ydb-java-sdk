package tech.ydb.core.grpc;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.net.HostAndPort;
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

    private final List<HostAndPort> hosts;
    private final String endpoint;
    private final String database;
    private Consumer<NettyChannelBuilder> channelInitializer = (cb) -> {};

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

    @Nullable
    public List<HostAndPort> getHosts() {
        return hosts;
    }

    @Nullable
    public String getEndpoint() {
        return endpoint;
    }

    @Nullable
    public String getDatabase() {
        return database;
    }

    public Consumer<NettyChannelBuilder> getChannelInitializer() {
        return channelInitializer;
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

    public GrpcTransportBuilder withChannelInitializer(Consumer<NettyChannelBuilder> channelInitializer) {
        this.channelInitializer = requireNonNull(channelInitializer);
        return this;
    }

    @Override
    public GrpcTransport build() {
        return new GrpcTransport(this);
    }
}
