package tech.ydb.coordination.rpc.grpc;

import java.util.concurrent.CompletableFuture;

import javax.annotation.PreDestroy;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;

import tech.ydb.coordination.AlterNodeRequest;
import tech.ydb.coordination.AlterNodeResponse;
import tech.ydb.coordination.CreateNodeRequest;
import tech.ydb.coordination.CreateNodeResponse;
import tech.ydb.coordination.DescribeNodeRequest;
import tech.ydb.coordination.DescribeNodeResponse;
import tech.ydb.coordination.DropNodeRequest;
import tech.ydb.coordination.DropNodeResponse;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.SessionResponse;
import tech.ydb.coordination.rpc.CoordinationRpc;
import tech.ydb.coordination.v1.CoordinationServiceGrpc;
import tech.ydb.core.Operations;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;

/**
 * @author Kirill Kurdyukov
 */
public class GrpcCoordinationRpc implements CoordinationRpc {

    private final GrpcTransport grpcTransport;
    private final boolean transportOwned;

    private GrpcCoordinationRpc(
            GrpcTransport grpcTransport,
            boolean transportOwned
    ) {
        this.grpcTransport = grpcTransport;
        this.transportOwned = transportOwned;
    }

    public static GrpcCoordinationRpc useTransport(@WillNotClose GrpcTransport transport) {
        return new GrpcCoordinationRpc(transport, false);
    }

    public static GrpcCoordinationRpc ownTransport(@WillClose GrpcTransport transport) {
        return new GrpcCoordinationRpc(transport, true);
    }

    public GrpcReadWriteStream<SessionResponse, SessionRequest> session() {
        return grpcTransport.readWriteStreamCall(
                CoordinationServiceGrpc.getSessionMethod(),
                GrpcRequestSettings.newBuilder().build()
        );
    }

    public CompletableFuture<Status> createNode(CreateNodeRequest request, GrpcRequestSettings settings) {
        return grpcTransport.unaryCall(
                CoordinationServiceGrpc.getCreateNodeMethod(),
                settings,
                request
        ).thenApply(Operations.statusUnwrapper(CreateNodeResponse::getOperation));
    }

    public CompletableFuture<Status> alterNode(AlterNodeRequest request, GrpcRequestSettings settings) {
        return grpcTransport.unaryCall(
                CoordinationServiceGrpc.getAlterNodeMethod(),
                settings,
                request
        ).thenApply(Operations.statusUnwrapper(AlterNodeResponse::getOperation));
    }

    public CompletableFuture<Status> dropNode(DropNodeRequest request, GrpcRequestSettings settings) {
        return grpcTransport.unaryCall(
                CoordinationServiceGrpc.getDropNodeMethod(),
                settings,
                request
        ).thenApply(Operations.statusUnwrapper(DropNodeResponse::getOperation));
    }

    public CompletableFuture<Status> describeNode(DescribeNodeRequest request, GrpcRequestSettings settings) {
        return grpcTransport.unaryCall(
                CoordinationServiceGrpc.getDescribeNodeMethod(),
                settings,
                request
        ).thenApply(Operations.statusUnwrapper(DescribeNodeResponse::getOperation));
    }

    @Override
    public String getDatabase() {
        return grpcTransport.getDatabase();
    }

    @PreDestroy
    public void close() {
        if (transportOwned) {
            grpcTransport.close();
        }
    }
}
