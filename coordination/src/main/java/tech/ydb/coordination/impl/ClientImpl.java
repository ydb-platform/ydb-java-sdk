package tech.ydb.coordination.impl;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.NodeConfig;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.CoordinationSessionSettings;
import tech.ydb.coordination.settings.DescribeCoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.operation.OperationUtils;
import tech.ydb.proto.coordination.AlterNodeRequest;
import tech.ydb.proto.coordination.CreateNodeRequest;
import tech.ydb.proto.coordination.DescribeNodeRequest;
import tech.ydb.proto.coordination.DropNodeRequest;

/**
 * @author Kirill Kurdyukov
 * @author Aleksandr Gorshenin
 */
class ClientImpl implements CoordinationClient {

    private final Rpc rpc;

    ClientImpl(Rpc rpc) {
        this.rpc = rpc;
    }

    private String validatePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Coordination node path cannot be empty");
        }

        return path.startsWith("/") ? path : rpc.getDatabase() + "/" + path;
    }

    @Override
    public CoordinationSession createSession(String path, CoordinationSessionSettings settings) {
        return new SessionImpl(rpc, Clock.systemUTC(), validatePath(path), settings);
    }

    @Override
    public CompletableFuture<Status> createNode(String path, CoordinationNodeSettings settings) {
        CreateNodeRequest request = CreateNodeRequest.newBuilder()
                .setPath(validatePath(path))
                .setOperationParams(OperationUtils.createParams(settings))
                .setConfig(settings.getConfig().toProto())
                .build();

        GrpcRequestSettings grpcSettings = OperationUtils.createGrpcRequestSettings(settings);
        return rpc.createNode(request, grpcSettings);
    }

    @Override
    public CompletableFuture<Status> alterNode(String path, CoordinationNodeSettings settings) {
        AlterNodeRequest request = AlterNodeRequest.newBuilder()
                .setPath(validatePath(path))
                .setOperationParams(OperationUtils.createParams(settings))
                .setConfig(settings.getConfig().toProto())
                .build();

        GrpcRequestSettings grpcSettings = OperationUtils.createGrpcRequestSettings(settings);
        return rpc.alterNode(request, grpcSettings);
    }

    @Override
    public CompletableFuture<Status> dropNode(String path, DropCoordinationNodeSettings settings) {
        DropNodeRequest request = DropNodeRequest.newBuilder()
                .setPath(validatePath(path))
                .setOperationParams(OperationUtils.createParams(settings))
                .build();

        GrpcRequestSettings grpcSettings = OperationUtils.createGrpcRequestSettings(settings);
        return rpc.dropNode(request, grpcSettings);
    }

    @Override
    public CompletableFuture<Result<NodeConfig>> describeNode(String path,
            DescribeCoordinationNodeSettings settings) {
        DescribeNodeRequest request = DescribeNodeRequest.newBuilder()
                .setPath(validatePath(path))
                .setOperationParams(OperationUtils.createParams(settings))
                .build();

        GrpcRequestSettings grpcSettings = OperationUtils.createGrpcRequestSettings(settings);
        return rpc.describeNode(request, grpcSettings);
    }

    @Override
    public String getDatabase() {
        return rpc.getDatabase();
    }
}
