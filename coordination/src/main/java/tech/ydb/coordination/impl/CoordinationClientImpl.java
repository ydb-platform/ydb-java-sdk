package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSessionNew;
import tech.ydb.coordination.rpc.CoordinationRpc;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DescribeCoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;
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
    public CompletableFuture<CoordinationSessionNew> createSession(String nodePath, Duration timeout) {
        return CoordinationSessionNewImpl.newSession(new CoordinationRetryableStreamImpl(coordinationRpc,
                        Executors.newScheduledThreadPool(Thread.activeCount()),
                        nodePath),
                timeout);
    }

    @Override
    public CompletableFuture<Status> createNode(
            String path,
            CoordinationNodeSettings coordinationNodeSettings
    ) {
        return coordinationRpc.createNode(
                CreateNodeRequest.newBuilder()
                        .setPath(path)
                        .setOperationParams(OperationUtils.createParams(coordinationNodeSettings))
                        .setConfig(createConfig(coordinationNodeSettings))
                        .build(),
                OperationUtils.createGrpcRequestSettings(coordinationNodeSettings)
        );
    }

    @Override
    public CompletableFuture<Status> alterNode(
            String path,
            CoordinationNodeSettings coordinationNodeSettings
    ) {
        return coordinationRpc.alterNode(
                AlterNodeRequest.newBuilder()
                        .setPath(path)
                        .setOperationParams(OperationUtils.createParams(coordinationNodeSettings))
                        .setConfig(createConfig(coordinationNodeSettings))
                        .build(),
                OperationUtils.createGrpcRequestSettings(coordinationNodeSettings)
        );
    }

    @Override
    public CompletableFuture<Status> dropNode(
            String path,
            DropCoordinationNodeSettings dropCoordinationNodeSettings
    ) {
        return coordinationRpc.dropNode(
                DropNodeRequest.newBuilder()
                        .setPath(path)
                        .setOperationParams(OperationUtils.createParams(dropCoordinationNodeSettings))
                        .build(),
                OperationUtils.createGrpcRequestSettings(dropCoordinationNodeSettings)
        );
    }

    @Override
    public CompletableFuture<Status> describeNode(
            String path,
            DescribeCoordinationNodeSettings describeCoordinationNodeSettings
    ) {
        return coordinationRpc.describeNode(
                DescribeNodeRequest.newBuilder()
                        .setPath(path)
                        .setOperationParams(OperationUtils.createParams(describeCoordinationNodeSettings))
                        .build(),
                OperationUtils.createGrpcRequestSettings(describeCoordinationNodeSettings)
        );
    }

    @Override
    public String getDatabase() {
        return coordinationRpc.getDatabase();
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
}
