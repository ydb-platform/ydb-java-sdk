package tech.ydb.topic.impl;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;

import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.YdbTopic;
import tech.ydb.topic.v1.TopicServiceGrpc;


/**
 * @author Nikolay Perfilov
 */
@ParametersAreNonnullByDefault
public final class GrpcTopicRpc implements TopicRpc {

    private final GrpcTransport transport;
    private final boolean transportOwned;

    private GrpcTopicRpc(GrpcTransport transport, boolean transportOwned) {
        this.transport = transport;
        this.transportOwned = transportOwned;
    }

    @Nullable
    public static GrpcTopicRpc useTransport(@WillNotClose GrpcTransport transport) {
        return new GrpcTopicRpc(transport, false);
    }

    @Nullable
    public static GrpcTopicRpc ownTransport(@WillClose GrpcTransport transport) {
        return new GrpcTopicRpc(transport, true);
    }

    @Override
    public CompletableFuture<Status> createTopic(YdbTopic.CreateTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getCreateTopicMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(YdbTopic.CreateTopicResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> alterTopic(YdbTopic.AlterTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getAlterTopicMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(YdbTopic.AlterTopicResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<YdbTopic.DescribeTopicResult>> describeTopic(YdbTopic.DescribeTopicRequest request,
                                                                                 GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getDescribeTopicMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(YdbTopic.DescribeTopicResponse::getOperation,
                        YdbTopic.DescribeTopicResult.class));
    }

    @Override
    public CompletableFuture<Status> dropTopic(YdbTopic.DropTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getDropTopicMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(YdbTopic.DropTopicResponse::getOperation));
    }

    @Override
    public String getDatabase() {
        return transport.getDatabase();
    }

    @Override
    public void close() {
        if (transportOwned) {
            transport.close();
        }
    }
}
