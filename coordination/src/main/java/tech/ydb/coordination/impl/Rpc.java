package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.coordination.description.NodeConfig;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.coordination.AlterNodeRequest;
import tech.ydb.proto.coordination.CreateNodeRequest;
import tech.ydb.proto.coordination.DescribeNodeRequest;
import tech.ydb.proto.coordination.DropNodeRequest;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionResponse;

/**
 * @author Kirill Kurdyukov
 */
public interface Rpc {

    GrpcReadWriteStream<SessionResponse, SessionRequest> createSession(GrpcRequestSettings settings);

    CompletableFuture<Status> createNode(CreateNodeRequest request, GrpcRequestSettings settings);

    CompletableFuture<Status> alterNode(AlterNodeRequest request, GrpcRequestSettings settings);

    CompletableFuture<Status> dropNode(DropNodeRequest request, GrpcRequestSettings settings);

    CompletableFuture<Result<NodeConfig>> describeNode(DescribeNodeRequest request, GrpcRequestSettings settings);

    String getDatabase();

    ScheduledExecutorService getScheduler();
}
