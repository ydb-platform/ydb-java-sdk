package ru.yandex.ydb.core.grpc;

import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import io.grpc.CallOptions;

import ru.yandex.ydb.core.auth.AuthProvider;
import ru.yandex.ydb.core.auth.NopAuthProvider;
import ru.yandex.ydb.core.auth.TokenAuthProvider;
import ru.yandex.ydb.core.rpc.RpcTransportBuilder;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public final class GrpcTransportBuilder extends RpcTransportBuilder<GrpcTransport, GrpcTransportBuilder> {

    final String host;
    final int port;
    final String endpoint;
    final String database;
    AuthProvider authProvider = NopAuthProvider.INSTANCE;
    CallOptions callOptions = CallOptions.DEFAULT;

    private GrpcTransportBuilder(String host, int port) {
        this.host = host;
        this.port = port;
        this.endpoint = null;
        this.database = null;
    }

    private GrpcTransportBuilder(String endpoint, String database) {
        this.host = null;
        this.port = -1;
        this.endpoint = endpoint;
        this.database = database;
    }

    public static GrpcTransportBuilder singleHost(String host, int port) {
        return new GrpcTransportBuilder(host, port);
    }

    public static GrpcTransportBuilder forEndpoint(String endpoint, String database) {
        return new GrpcTransportBuilder(endpoint, database);
    }

    public GrpcTransportBuilder withAuthToken(String token) {
        return withAuthProvider(new TokenAuthProvider(token));
    }

    public GrpcTransportBuilder withAuthProvider(AuthProvider authProvider) {
        this.authProvider = Objects.requireNonNull(authProvider);
        return this;
    }

    @Override
    public GrpcTransport build() {
        return new GrpcTransport(this);
    }
}
