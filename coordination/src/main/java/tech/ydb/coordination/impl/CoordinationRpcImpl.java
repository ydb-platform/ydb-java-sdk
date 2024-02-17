package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import javax.annotation.WillNotClose;

import tech.ydb.coordination.description.NodeConfig;
import tech.ydb.core.Result;
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
import tech.ydb.proto.coordination.DescribeNodeResult;
import tech.ydb.proto.coordination.DropNodeRequest;
import tech.ydb.proto.coordination.DropNodeResponse;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionResponse;
import tech.ydb.proto.coordination.v1.CoordinationServiceGrpc;

/**
 * @author Kirill Kurdyukov
 */
class CoordinationRpcImpl implements CoordinationRpc {
    private static final Function<Result<CreateNodeResponse>, Status> CREATE_NODE_STATUS = OperationManager
            .syncStatusUnwrapper(CreateNodeResponse::getOperation);

    private static final Function<Result<AlterNodeResponse>, Status> ALTER_NODE_STATUS = OperationManager
            .syncStatusUnwrapper(AlterNodeResponse::getOperation);

    private static final Function<Result<DropNodeResponse>, Status> DROP_NODE_STATUS = OperationManager
            .syncStatusUnwrapper(DropNodeResponse::getOperation);

    private static final Function<Result<DescribeNodeResponse>, Result<DescribeNodeResult>> DESCRIBE_NODE_RESULT =
            OperationManager.syncResultUnwrapper(DescribeNodeResponse::getOperation, DescribeNodeResult.class);

    private final GrpcTransport transport;

    private CoordinationRpcImpl(GrpcTransport grpcTransport) {
        this.transport = grpcTransport;
    }

    public static CoordinationRpc useTransport(@WillNotClose GrpcTransport transport) {
        return new CoordinationRpcImpl(transport);
    }

    @Override
    public GrpcReadWriteStream<SessionResponse, SessionRequest> createSession(GrpcRequestSettings settings) {
        return transport.readWriteStreamCall(CoordinationServiceGrpc.getSessionMethod(), settings);
    }

    @Override
    public CompletableFuture<Status> createNode(CreateNodeRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(CoordinationServiceGrpc.getCreateNodeMethod(), settings, request)
                .thenApply(CREATE_NODE_STATUS);
    }

    @Override
    public CompletableFuture<Status> alterNode(AlterNodeRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(CoordinationServiceGrpc.getAlterNodeMethod(), settings, request)
                .thenApply(ALTER_NODE_STATUS);
    }

    @Override
    public CompletableFuture<Status> dropNode(DropNodeRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(CoordinationServiceGrpc.getDropNodeMethod(), settings, request)
                .thenApply(DROP_NODE_STATUS);
    }

    @Override
    public CompletableFuture<Result<NodeConfig>> describeNode(DescribeNodeRequest req, GrpcRequestSettings settings) {
        return transport
                .unaryCall(CoordinationServiceGrpc.getDescribeNodeMethod(), settings, req)
                .thenApply(DESCRIBE_NODE_RESULT)
                .thenApply(r -> r.map(NodeConfig::fromProto));
    }

    @Override
    public String getDatabase() {
        return transport.getDatabase();
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return transport.getScheduler();
    }
}
