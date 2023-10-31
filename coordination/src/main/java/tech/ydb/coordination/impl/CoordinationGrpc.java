package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.WillNotClose;

import tech.ydb.coordination.rpc.CoordinationRpc;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.OperationManager;
import tech.ydb.proto.coordination.AlterNodeRequest;
import tech.ydb.proto.coordination.AlterNodeResponse;
import tech.ydb.proto.coordination.CreateNodeRequest;
import tech.ydb.proto.coordination.CreateNodeResponse;
import tech.ydb.proto.coordination.DescribeNodeRequest;
import tech.ydb.proto.coordination.DescribeNodeResponse;
import tech.ydb.proto.coordination.DropNodeRequest;
import tech.ydb.proto.coordination.DropNodeResponse;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionResponse;
import tech.ydb.proto.coordination.v1.CoordinationServiceGrpc;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationGrpc implements CoordinationRpc {

    private final GrpcTransport grpcTransport;

    private CoordinationGrpc(GrpcTransport grpcTransport) {
        this.grpcTransport = grpcTransport;
    }

    public static CoordinationGrpc useTransport(@WillNotClose GrpcTransport transport) {
        return new CoordinationGrpc(transport);
    }

    @Override
    public GrpcReadWriteStream<SessionResponse, SessionRequest> session() {
        return grpcTransport.readWriteStreamCall(
                CoordinationServiceGrpc.getSessionMethod(),
                GrpcRequestSettings.newBuilder().build()
        );
    }

    @Override
    public CompletableFuture<Status> createNode(CreateNodeRequest request, GrpcRequestSettings settings) {
        return grpcTransport.unaryCall(
                CoordinationServiceGrpc.getCreateNodeMethod(),
                settings,
                request
        ).thenApply(OperationManager.syncStatusUnwrapper(CreateNodeResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> alterNode(AlterNodeRequest request, GrpcRequestSettings settings) {
        return grpcTransport.unaryCall(
                CoordinationServiceGrpc.getAlterNodeMethod(),
                settings,
                request
        ).thenApply(OperationManager.syncStatusUnwrapper(AlterNodeResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> dropNode(DropNodeRequest request, GrpcRequestSettings settings) {
        return grpcTransport.unaryCall(
                CoordinationServiceGrpc.getDropNodeMethod(),
                settings,
                request
        ).thenApply(OperationManager.syncStatusUnwrapper(DropNodeResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> describeNode(DescribeNodeRequest request, GrpcRequestSettings settings) {
        return grpcTransport.unaryCall(
                CoordinationServiceGrpc.getDescribeNodeMethod(),
                settings,
                request
        ).thenApply(OperationManager.syncStatusUnwrapper(DescribeNodeResponse::getOperation));
    }

    @Override
    public String getDatabase() {
        return grpcTransport.getDatabase();
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return grpcTransport.getScheduler();
    }
}
