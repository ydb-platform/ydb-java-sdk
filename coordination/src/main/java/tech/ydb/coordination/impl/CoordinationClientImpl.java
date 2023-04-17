package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.AlterNodeRequest;
import tech.ydb.coordination.Config;
import tech.ydb.coordination.ConsistencyMode;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CreateNodeRequest;
import tech.ydb.coordination.DescribeNodeRequest;
import tech.ydb.coordination.DropNodeRequest;
import tech.ydb.coordination.RateLimiterCountersMode;
import tech.ydb.coordination.rpc.CoordinationGrpc;
import tech.ydb.coordination.session.CoordinationSession;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DescribeCoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.impl.operation.OperationUtils;
import tech.ydb.core.settings.BaseRequestSettings;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationClientImpl implements CoordinationClient {

    private final CoordinationGrpc coordinationGrpc;

    public CoordinationClientImpl(CoordinationGrpc coordinationGrpc) {
        this.coordinationGrpc = coordinationGrpc;
    }

    @Override
    public CoordinationSession createSession(String path) {
        return new CoordinationSession(coordinationGrpc.session());
    }

    @Override
    public CompletableFuture<Status> createNode(
            String path,
            CoordinationNodeSettings coordinationNodeSettings
    ) {
        return coordinationGrpc.createNode(
                CreateNodeRequest.newBuilder()
                        .setPath(path)
                        .setOperationParams(OperationUtils.createParams(coordinationNodeSettings))
                        .setConfig(createConfig(coordinationNodeSettings))
                        .build(),
                createGrpcRequestSettings(coordinationNodeSettings)
        );
    }

    @Override
    public CompletableFuture<Status> alterNode(
            String path,
            CoordinationNodeSettings coordinationNodeSettings
    ) {
        return coordinationGrpc.alterNode(
                AlterNodeRequest.newBuilder()
                        .setPath(path)
                        .setOperationParams(OperationUtils.createParams(coordinationNodeSettings))
                        .setConfig(createConfig(coordinationNodeSettings))
                        .build(),
                createGrpcRequestSettings(coordinationNodeSettings)
        );
    }

    @Override
    public CompletableFuture<Status> dropNode(
            String path,
            DropCoordinationNodeSettings dropCoordinationNodeSettings
    ) {
        return coordinationGrpc.dropNode(
                DropNodeRequest.newBuilder()
                        .setPath(path)
                        .setOperationParams(OperationUtils.createParams(dropCoordinationNodeSettings))
                        .build(),
                createGrpcRequestSettings(dropCoordinationNodeSettings)
        );
    }

    @Override
    public CompletableFuture<Status> describeNode(
            String path,
            DescribeCoordinationNodeSettings describeCoordinationNodeSettings
    ) {
        return coordinationGrpc.describeNode(
                DescribeNodeRequest.newBuilder()
                        .setPath(path)
                        .setOperationParams(OperationUtils.createParams(describeCoordinationNodeSettings))
                        .build(),
                createGrpcRequestSettings(describeCoordinationNodeSettings)
        );
    }

    private static ConsistencyMode toProto(CoordinationNodeSettings.ConsistencyMode consistencyMode) {
        switch (consistencyMode) {
            case CONSISTENCY_MODE_STRICT:
                return ConsistencyMode.CONSISTENCY_MODE_STRICT;
            case CONSISTENCY_MODE_RELAXED:
                return ConsistencyMode.CONSISTENCY_MODE_RELAXED;
            default:
                throw new RuntimeException("Unknown consistency mode: " + consistencyMode);
        }
    }

    private static RateLimiterCountersMode toProto(
            CoordinationNodeSettings.RateLimiterCountersMode rateLimiterCountersMode
    ) {
        switch (rateLimiterCountersMode) {
            case RATE_LIMITER_COUNTERS_MODE_DETAILED:
                return RateLimiterCountersMode.RATE_LIMITER_COUNTERS_MODE_DETAILED;
            case RATE_LIMITER_COUNTERS_MODE_AGGREGATED:
                return RateLimiterCountersMode.RATE_LIMITER_COUNTERS_MODE_AGGREGATED;
            default:
                throw new RuntimeException("Unknown rate limiter counters mode: " + rateLimiterCountersMode);
        }
    }

    private static Config createConfig(CoordinationNodeSettings coordinationNodeSettings) {
        Config.Builder configBuilder = Config.newBuilder()
                .setSelfCheckPeriodMillis(coordinationNodeSettings.getSelfCheckPeriodMillis())
                .setSessionGracePeriodMillis(coordinationNodeSettings.getSessionGracePeriodMillis())
                .setReadConsistencyMode(toProto(coordinationNodeSettings.getReadConsistencyMode()))
                .setAttachConsistencyMode(toProto(coordinationNodeSettings.getAttachConsistencyMode()));

        if (coordinationNodeSettings.getRateLimiterCountersMode() != null) {
            configBuilder.setRateLimiterCountersMode(toProto(coordinationNodeSettings.getRateLimiterCountersMode()));
        }

        return configBuilder.build();
    }

    private GrpcRequestSettings createGrpcRequestSettings(BaseRequestSettings settings) {
        return GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .build();
    }
}
