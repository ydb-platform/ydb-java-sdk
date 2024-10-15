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
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.v1.TopicServiceGrpc;
import tech.ydb.topic.TopicRpc;


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
                .thenApply(OperationBinder.bindSync(YdbTopic.CreateTopicResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> alterTopic(YdbTopic.AlterTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getAlterTopicMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTopic.AlterTopicResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<YdbTopic.DescribeTopicResult>> describeTopic(YdbTopic.DescribeTopicRequest request,
                                                                                 GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getDescribeTopicMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(
                        YdbTopic.DescribeTopicResponse::getOperation, YdbTopic.DescribeTopicResult.class)
                );
    }

    @Override
    public CompletableFuture<Result<YdbTopic.DescribeConsumerResult>> describeConsumer(
            YdbTopic.DescribeConsumerRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TopicServiceGrpc.getDescribeConsumerMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(
                        YdbTopic.DescribeConsumerResponse::getOperation, YdbTopic.DescribeConsumerResult.class)
                );
    }

    @Override
    public CompletableFuture<Status> dropTopic(YdbTopic.DropTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getDropTopicMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTopic.DropTopicResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> commitOffset(YdbTopic.CommitOffsetRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getCommitOffsetMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTopic.CommitOffsetResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> updateOffsetsInTransaction(YdbTopic.UpdateOffsetsInTransactionRequest request,
                                                         GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getUpdateOffsetsInTransactionMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTopic.UpdateOffsetsInTransactionResponse::getOperation));
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
