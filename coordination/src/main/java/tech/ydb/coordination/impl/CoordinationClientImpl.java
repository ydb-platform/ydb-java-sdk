package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.rpc.CoordinationRpc;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.CoordinationSessionSettings;
import tech.ydb.coordination.settings.DescribeCoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.coordination.settings.NodeConsistenteMode;
import tech.ydb.coordination.settings.NodeRateLimiterCountersMode;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.operation.OperationUtils;
import tech.ydb.proto.coordination.AlterNodeRequest;
import tech.ydb.proto.coordination.Config;
import tech.ydb.proto.coordination.ConsistencyMode;
import tech.ydb.proto.coordination.CreateNodeRequest;
import tech.ydb.proto.coordination.DescribeNodeRequest;
import tech.ydb.proto.coordination.DropNodeRequest;
import tech.ydb.proto.coordination.RateLimiterCountersMode;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationClientImpl implements CoordinationClient {

    private final CoordinationRpc coordinationRpc;

    public CoordinationClientImpl(CoordinationRpc grpcCoordinationRpc) {
        this.coordinationRpc = grpcCoordinationRpc;
    }

    @Override
    public CompletableFuture<CoordinationSession> createSession(String path, CoordinationSessionSettings settings) {
        return CoordinationSessionImpl.newSession(coordinationRpc, path, settings);
    }

    @Override
    public CompletableFuture<Status> createNode(String path, CoordinationNodeSettings settings) {
        CreateNodeRequest request = CreateNodeRequest.newBuilder()
                .setPath(path)
                .setOperationParams(OperationUtils.createParams(settings))
                .setConfig(createConfig(settings))
                .build();

        GrpcRequestSettings grpcSettings = OperationUtils.createGrpcRequestSettings(settings);
        return coordinationRpc.createNode(request, grpcSettings);
    }

    @Override
    public CompletableFuture<Status> alterNode(String path, CoordinationNodeSettings settings) {
        AlterNodeRequest request = AlterNodeRequest.newBuilder()
                .setPath(path)
                .setOperationParams(OperationUtils.createParams(settings))
                .setConfig(createConfig(settings))
                .build();

        GrpcRequestSettings grpcSettings = OperationUtils.createGrpcRequestSettings(settings);
        return coordinationRpc.alterNode(request, grpcSettings);
    }

    @Override
    public CompletableFuture<Status> dropNode(String path, DropCoordinationNodeSettings settings) {
        DropNodeRequest request = DropNodeRequest.newBuilder()
                .setPath(path)
                .setOperationParams(OperationUtils.createParams(settings))
                .build();

        GrpcRequestSettings grpcSettings = OperationUtils.createGrpcRequestSettings(settings);
        return coordinationRpc.dropNode(request, grpcSettings);
    }

    @Override
    public CompletableFuture<Status> describeNode(String path,DescribeCoordinationNodeSettings settings) {
        DescribeNodeRequest request = DescribeNodeRequest.newBuilder()
                .setPath(path)
                .setOperationParams(OperationUtils.createParams(settings))
                .build();

        GrpcRequestSettings grpcSettings = OperationUtils.createGrpcRequestSettings(settings);
        return coordinationRpc.describeNode(request, grpcSettings);
    }

    @Override
    public String getDatabase() {
        return coordinationRpc.getDatabase();
    }

    private static ConsistencyMode toProto(NodeConsistenteMode mode) {
        switch (mode) {
            case UNSET:
                return ConsistencyMode.CONSISTENCY_MODE_UNSET;
            case STRICT:
                return ConsistencyMode.CONSISTENCY_MODE_STRICT;
            case RELAXED:
                return ConsistencyMode.CONSISTENCY_MODE_RELAXED;
            default:
                throw new RuntimeException("Unknown consistency mode: " + mode);
        }
    }

    private static RateLimiterCountersMode toProto(NodeRateLimiterCountersMode mode) {
        switch (mode) {
            case UNSET:
                return RateLimiterCountersMode.RATE_LIMITER_COUNTERS_MODE_UNSET;
            case DETAILED:
                return RateLimiterCountersMode.RATE_LIMITER_COUNTERS_MODE_DETAILED;
            case AGGREGATED:
                return RateLimiterCountersMode.RATE_LIMITER_COUNTERS_MODE_AGGREGATED;
            default:
                throw new RuntimeException("Unknown rate limiter counters mode: " + mode);
        }
    }

    private static Config createConfig(CoordinationNodeSettings coordinationNodeSettings) {
        return Config.newBuilder()
                .setSelfCheckPeriodMillis((int) coordinationNodeSettings.getSelfCheckPeriod().toMillis())
                .setSessionGracePeriodMillis((int) coordinationNodeSettings.getSessionGracePeriod().toMillis())
                .setReadConsistencyMode(toProto(coordinationNodeSettings.getReadConsistencyMode()))
                .setAttachConsistencyMode(toProto(coordinationNodeSettings.getAttachConsistencyMode()))
                .setRateLimiterCountersMode(toProto(coordinationNodeSettings.getRateLimiterCountersMode()))
                .build();
    }
}
