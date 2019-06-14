package tech.ydb.core.grpc;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.net.HostAndPort;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.auth.NopAuthProvider;
import tech.ydb.core.rpc.RpcTransportBuilder;
import io.grpc.CallOptions;

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
    AuthProvider authProvider = NopAuthProvider.INSTANCE;
    CallOptions callOptions = CallOptions.DEFAULT;

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
        return new GrpcTransportBuilder(Arrays.asList(requireNonNull(hosts, "hosts")));
    }

    public static GrpcTransportBuilder multipleHosts(List<HostAndPort> hosts) {
        return new GrpcTransportBuilder(requireNonNull(hosts, "hosts"));
    }

    public static GrpcTransportBuilder forEndpoint(String endpoint, String database) {
        return new GrpcTransportBuilder(requireNonNull(endpoint, "endpoint"), requireNonNull(database, "database"));
    }

    public GrpcTransportBuilder withAuthProvider(AuthProvider authProvider) {
        this.authProvider = requireNonNull(authProvider);
        return this;
    }

    @Override
    public GrpcTransport build() {
        return new GrpcTransport(this);
    }
}
