package tech.ydb.test.integration.docker;

import com.google.protobuf.Any;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerMethodDefinition;
import io.grpc.Status;

import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.discovery.DiscoveryProtos;
import tech.ydb.proto.discovery.v1.DiscoveryServiceGrpc;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class DiscoveryServiceProxy
        implements ServerCallHandler<DiscoveryProtos.ListEndpointsRequest, DiscoveryProtos.ListEndpointsResponse> {

    private final EndpointRecord endpoint;

    public DiscoveryServiceProxy(EndpointRecord endpoint) {
        this.endpoint = endpoint;
    }

    public ServerMethodDefinition<?, ?> toMethodDefinition() {
        return ServerMethodDefinition.create(DiscoveryServiceGrpc.getListEndpointsMethod(), this);
    }

    @Override
    public ServerCall.Listener<DiscoveryProtos.ListEndpointsRequest> startCall(
            ServerCall<DiscoveryProtos.ListEndpointsRequest, DiscoveryProtos.ListEndpointsResponse> serverCall,
            Metadata metadata
    ) {
        serverCall.request(1);
        serverCall.sendHeaders(new Metadata());
        return new ServerCall.Listener<DiscoveryProtos.ListEndpointsRequest>() {
            @Override
            public void onMessage(DiscoveryProtos.ListEndpointsRequest message) {
                serverCall.sendMessage(createDiscoveryResponse());
            }

            @Override
            public void onHalfClose() {
                serverCall.close(Status.OK, new Metadata());
            }
        };
    }

    private DiscoveryProtos.ListEndpointsResponse createDiscoveryResponse() {
        DiscoveryProtos.ListEndpointsResult result = DiscoveryProtos.ListEndpointsResult.newBuilder()
                .setSelfLocation("PROXY")
                .addEndpoints(DiscoveryProtos.EndpointInfo.newBuilder()
                        .setAddress(endpoint.getHost())
                        .setPort(endpoint.getPort())
                        .build()
                )
                .build();

        OperationProtos.Operation operation = OperationProtos.Operation.newBuilder()
                .setReady(true)
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setId("grpc-proxy-discovery")
                .setResult(Any.pack(result))
                .build();

        return DiscoveryProtos.ListEndpointsResponse.newBuilder()
                .setOperation(operation)
                .build();
    }
}
