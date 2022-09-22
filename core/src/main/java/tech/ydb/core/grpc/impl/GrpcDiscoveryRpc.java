package tech.ydb.core.grpc.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.discovery.DiscoveryProtos;
import tech.ydb.discovery.v1.DiscoveryServiceGrpc;


/**
 * @author Vladimir Gordiychuk
 * @author Alexandr Gorshenin
 */
public class GrpcDiscoveryRpc {
    private static final Logger logger = LoggerFactory.getLogger(GrpcDiscoveryRpc.class);
    private static final long DISCOVERY_TIMEOUT_SECONDS = 10;

    private final BaseGrpcTrasnsport parent;
    private final EndpointRecord endpoint;
    private final ChannelFactory channelFactory;

    public GrpcDiscoveryRpc(
            BaseGrpcTrasnsport parent,
            EndpointRecord endpoint,
            ChannelFactory channelFactory) {
        this.parent = parent;
        this.endpoint = endpoint;
        this.channelFactory = channelFactory;
    }

    public CompletableFuture<Result<DiscoveryProtos.ListEndpointsResult>> listEndpoints() {
        GrpcTransport transport = createTransport();

        logger.debug("list endpoints from {}", endpoint.getHostAndPort());
        DiscoveryProtos.ListEndpointsRequest request = DiscoveryProtos.ListEndpointsRequest.newBuilder()
                .setDatabase(parent.getDatabase())
                .build();

        GrpcRequestSettings grpcSettings = GrpcRequestSettings.newBuilder()
                .withDeadlineAfter(System.nanoTime() + Duration.ofSeconds(DISCOVERY_TIMEOUT_SECONDS).toNanos())
                .build();

        return transport.unaryCall(DiscoveryServiceGrpc.getListEndpointsMethod(), grpcSettings, request)
                .whenComplete((res, ex) -> transport.close())
                .thenApply(Operations.resultUnwrapper(
                        DiscoveryProtos.ListEndpointsResponse::getOperation,
                        DiscoveryProtos.ListEndpointsResult.class
                ));
    }

    private GrpcTransport createTransport() {
        return new SingleChannelTransport(
                parent.getCallOptions(),
                parent.getDefaultReadTimeoutMillis(),
                parent.getDatabase(),
                endpoint,
                channelFactory
        );
    }
}
