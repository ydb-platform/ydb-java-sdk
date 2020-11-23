package tech.ydb.core.grpc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import tech.ydb.core.Result;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest;
import tech.ydb.discovery.DiscoveryProtos.ListEndpointsResult;
import tech.ydb.discovery.v1.DiscoveryServiceGrpc;

/**
 * @author Vladimir Gordiychuk
 */
public class GrpcDiscoveryRpc implements AutoCloseable {
    private final GrpcTransport transport;
    private final OperationTray operationTray;

    public GrpcDiscoveryRpc(GrpcTransport transport) {
        this.transport = transport;
        this.operationTray = transport.getOperationTray();
    }

    public CompletableFuture<Result<ListEndpointsResult>> listEndpoints(String database, long deadlineAfter) {
        ListEndpointsRequest request = ListEndpointsRequest.newBuilder()
                .setDatabase(database)
                .build();

        return transport.unaryCall(DiscoveryServiceGrpc.getListEndpointsMethod(), request, deadlineAfter)
                .thenCompose(result -> {
                    if (!result.isSuccess()) {
                        return CompletableFuture.completedFuture(result.cast());
                    }

                    return operationTray.waitResult(
                            result.expect("listEndpoints()").getOperation(),
                            ListEndpointsResult.class,
                            Function.identity(),
                            deadlineAfter);
                });
    }

    @Override
    public void close() {
        transport.close();
    }
}
