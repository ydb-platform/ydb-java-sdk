package tech.ydb.core.grpc.impl;

import java.util.concurrent.CompletableFuture;
import tech.ydb.core.Operations;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.discovery.DiscoveryProtos;
import tech.ydb.discovery.v1.DiscoveryServiceGrpc;

/**
 * @author Vladimir Gordiychuk
 */
public class GrpcDiscoveryRpc implements AutoCloseable {
    private final GrpcTransport transport;

    public GrpcDiscoveryRpc(GrpcTransport transport) {
        this.transport = transport;
    }

    public CompletableFuture<Result<DiscoveryProtos.ListEndpointsResult>> listEndpoints(String database, GrpcRequestSettings settings) {
        DiscoveryProtos.ListEndpointsRequest request = DiscoveryProtos.ListEndpointsRequest.newBuilder()
                .setDatabase(database)
                .build();

        return transport.unaryCall(DiscoveryServiceGrpc.getListEndpointsMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(
                        DiscoveryProtos.ListEndpointsResponse::getOperation,
                        DiscoveryProtos.ListEndpointsResult.class
                ));
    }

    @Override
    public void close() {
        transport.close();
    }
}
