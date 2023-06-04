package tech.ydb.topic.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.OperationManager;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.YdbTopic;
import tech.ydb.topic.v1.TopicServiceGrpc;


/**
 * @author Nikolay Perfilov
 */
@ParametersAreNonnullByDefault
public final class GrpcTopicRpc implements TopicRpc {

    private final GrpcTransport transport;

    private GrpcTopicRpc(GrpcTransport transport) {
        this.transport = transport;
    }

    public static GrpcTopicRpc useTransport(@WillNotClose GrpcTransport transport) {
        return new GrpcTopicRpc(transport);
    }

    @Override
    public CompletableFuture<Status> createTopic(YdbTopic.CreateTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getCreateTopicMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(YdbTopic.CreateTopicResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> alterTopic(YdbTopic.AlterTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getAlterTopicMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(YdbTopic.AlterTopicResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<YdbTopic.DescribeTopicResult>> describeTopic(YdbTopic.DescribeTopicRequest request,
                                                                                 GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getDescribeTopicMethod(), settings, request)
                .thenApply(OperationManager.syncResultUnwrapper(YdbTopic.DescribeTopicResponse::getOperation,
                        YdbTopic.DescribeTopicResult.class));
    }

    @Override
    public CompletableFuture<Status> dropTopic(YdbTopic.DropTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getDropTopicMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(YdbTopic.DropTopicResponse::getOperation));
    }

    @Override
    public GrpcReadWriteStream<
        YdbTopic.StreamWriteMessage.FromServer,
        YdbTopic.StreamWriteMessage.FromClient
        > writeSession() {
        return transport.readWriteStreamCall(TopicServiceGrpc.getStreamWriteMethod(),
                GrpcRequestSettings.newBuilder().build());
    }

    @Override
    public GrpcReadWriteStream<
            YdbTopic.StreamReadMessage.FromServer,
            YdbTopic.StreamReadMessage.FromClient
            > readSession() {
        return transport.readWriteStreamCall(TopicServiceGrpc.getStreamReadMethod(),
                GrpcRequestSettings.newBuilder().build());
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return transport.getScheduler();
    }
}
