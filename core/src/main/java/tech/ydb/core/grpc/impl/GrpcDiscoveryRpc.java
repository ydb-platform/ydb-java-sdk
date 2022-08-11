package tech.ydb.core.grpc.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.discovery.DiscoveryProtos;
import tech.ydb.discovery.v1.DiscoveryServiceGrpc;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Vladimir Gordiychuk
 */
public class GrpcDiscoveryRpc {
    private static final long DISCOVERY_TIMEOUT_SECONDS = 10;

    private final AuthProvider authProvider;
    private final EndpointRecord endpoint;
    private final long readTimeoutMillis;
    private final String database;
    private final ChannelFactory channelFactory;

    public GrpcDiscoveryRpc(
            AuthProvider authProvider,
            long readTimeoutMillis,
            EndpointRecord endpoint,
            ChannelFactory channelFactory,
            String database) {
        this.authProvider = authProvider;
        this.endpoint = endpoint;
        this.readTimeoutMillis = readTimeoutMillis;
        this.channelFactory = channelFactory;
        this.database = database;
    }

    public CompletableFuture<Result<DiscoveryProtos.ListEndpointsResult>> listEndpoints() {
        try (GrpcTransport transport = createTransport()) {
            DiscoveryProtos.ListEndpointsRequest request = DiscoveryProtos.ListEndpointsRequest.newBuilder()
                    .setDatabase(database)
                    .build();

            GrpcRequestSettings grpcSettings = GrpcRequestSettings.newBuilder()
                    .withDeadlineAfter(System.nanoTime() + Duration.ofSeconds(DISCOVERY_TIMEOUT_SECONDS).toNanos())
                    .build();

            return transport.unaryCall(DiscoveryServiceGrpc.getListEndpointsMethod(), grpcSettings, request)
                    .thenApply(Operations.resultUnwrapper(
                            DiscoveryProtos.ListEndpointsResponse::getOperation,
                            DiscoveryProtos.ListEndpointsResult.class
                    ));
            
        }
    }
    
    private GrpcTransport createTransport() {
        return new SingleChannelTransport(
                authProvider,
                MoreExecutors.directExecutor(),
                readTimeoutMillis,
                endpoint,
                channelFactory
        );
    }
}
