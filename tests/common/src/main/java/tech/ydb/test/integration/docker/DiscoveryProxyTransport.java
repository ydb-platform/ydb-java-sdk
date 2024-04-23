package tech.ydb.test.integration.docker;

import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.proto.discovery.DiscoveryProtos;
import tech.ydb.proto.discovery.v1.DiscoveryServiceGrpc;
import tech.ydb.test.integration.utils.ProxyGrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DiscoveryProxyTransport extends ProxyGrpcTransport {
    private final GrpcTransport dockered;
    private final EndpointRecord secured;
    private final EndpointRecord insecured;

    public DiscoveryProxyTransport(GrpcTransport dockered, EndpointRecord secured, EndpointRecord insecured) {
        this.dockered = dockered;
        this.secured = secured;
        this.insecured = insecured;
    }

    @Override
    protected GrpcTransport origin() {
        return dockered;
    }

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings, ReqT request) {
        if (method == DiscoveryServiceGrpc.getListEndpointsMethod()) {
            return super.unaryCall(method, settings, request).thenApply(this::mapDiscovery);
        }

        return super.unaryCall(method, settings, request);
    }

    @SuppressWarnings("unchecked")
    private <RespT> Result<RespT> mapDiscovery(Result<RespT> result) {
        return result.map(resp -> (RespT) mapDiscoveryResponse((DiscoveryProtos.ListEndpointsResponse) resp));
    }

    private DiscoveryProtos.ListEndpointsResponse mapDiscoveryResponse(DiscoveryProtos.ListEndpointsResponse origin) {
        if (!origin.getOperation().getReady()) {
            return origin;
        }

        try {
            DiscoveryProtos.ListEndpointsResult actual = origin.getOperation().getResult()
                    .unpack(DiscoveryProtos.ListEndpointsResult.class);

            DiscoveryProtos.ListEndpointsResult.Builder updated = DiscoveryProtos.ListEndpointsResult.newBuilder();
            if (actual.getSelfLocation() != null) {
                updated.setSelfLocation(actual.getSelfLocation());
            }
            for (DiscoveryProtos.EndpointInfo e: actual.getEndpointsList()) {
                DiscoveryProtos.EndpointInfo.Builder u = DiscoveryProtos.EndpointInfo.newBuilder();
                u.setAddress(e.getAddress());
                u.setNodeId(e.getNodeId());

                switch (e.getPort()) {
                    case YdbDockerContainer.DEFAULT_SECURE_PORT:
                        u.setPort(secured.getPort());
                        break;
                    case YdbDockerContainer.DEFAULT_INSECURE_PORT:
                        u.setPort(insecured.getPort());
                        break;
                    default:
                        u.setPort(e.getPort());
                        break;
                }

                if (e.getLocation() != null) {
                    u.setLocation(e.getLocation());
                }

                updated.addEndpoints(u.build());
            }

            return DiscoveryProtos.ListEndpointsResponse.newBuilder()
                    .setOperation(origin.getOperation().toBuilder().setResult(Any.pack(updated.build())).build())
                    .build();
        } catch (InvalidProtocolBufferException ex) {
            return origin;
        }
    }
}
