package tech.ydb.coordination.rpc;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.AlterNodeRequest;
import tech.ydb.coordination.CreateNodeRequest;
import tech.ydb.coordination.DescribeNodeRequest;
import tech.ydb.coordination.DropNodeRequest;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.SessionResponse;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;

public interface CoordinationRpc {

    GrpcReadWriteStream<SessionResponse, SessionRequest> session();

    CompletableFuture<Status> createNode(CreateNodeRequest request, GrpcRequestSettings settings);

    CompletableFuture<Status> alterNode(AlterNodeRequest request, GrpcRequestSettings settings);

    CompletableFuture<Status> dropNode(DropNodeRequest request, GrpcRequestSettings settings);

    CompletableFuture<Status> describeNode(DescribeNodeRequest request, GrpcRequestSettings settings);

    String getDatabase();
}
