package tech.ydb.core.impl.discovery;


import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.BaseGrpcTransport;
import tech.ydb.core.impl.FixedCallOptionsTransport;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.ManagedChannelFactory;
import tech.ydb.core.operation.OperationManager;
import tech.ydb.proto.discovery.DiscoveryProtos;
import tech.ydb.proto.discovery.v1.DiscoveryServiceGrpc;


/**
 * @author Vladimir Gordiychuk
 * @author Alexandr Gorshenin
 */
public class GrpcDiscoveryRpc {
    private static final Logger logger = LoggerFactory.getLogger(GrpcDiscoveryRpc.class);

    private final BaseGrpcTransport parent;
    private final EndpointRecord endpoint;
    private final ManagedChannelFactory channelFactory;
    private final AuthCallOptions callOptions;
    private final Duration discoveryTimeout;

    public GrpcDiscoveryRpc(
            BaseGrpcTransport parent,
            EndpointRecord endpoint,
            ManagedChannelFactory channelFactory,
            AuthCallOptions callOptions,
            Duration discoveryTimeout) {
        this.parent = parent;
        this.endpoint = endpoint;
        this.channelFactory = channelFactory;
        this.callOptions = callOptions;
        this.discoveryTimeout = discoveryTimeout;
    }

    public CompletableFuture<Result<DiscoveryProtos.ListEndpointsResult>> listEndpoints() {
        GrpcTransport transport = createTransport();

        logger.debug("list endpoints from {} with timeout {}", endpoint.getHostAndPort(), discoveryTimeout);
        DiscoveryProtos.ListEndpointsRequest request = DiscoveryProtos.ListEndpointsRequest.newBuilder()
                .setDatabase(parent.getDatabase())
                .build();

        GrpcRequestSettings grpcSettings = GrpcRequestSettings.newBuilder()
                .withDeadline(discoveryTimeout)
                .build();

        return transport.unaryCall(DiscoveryServiceGrpc.getListEndpointsMethod(), grpcSettings, request)
                .whenComplete((res, ex) -> transport.close())
                .thenApply(OperationManager.syncResultUnwrapper(
                        DiscoveryProtos.ListEndpointsResponse::getOperation,
                        DiscoveryProtos.ListEndpointsResult.class
                ));
    }

    private GrpcTransport createTransport() {
        return new FixedCallOptionsTransport(
                parent.getScheduler(),
                callOptions,
                parent.getDatabase(),
                endpoint,
                channelFactory
        );
    }
}
