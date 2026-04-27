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
import tech.ydb.proto.topic.YdbTopic.AlterTopicRequest;
import tech.ydb.proto.topic.YdbTopic.AlterTopicResponse;
import tech.ydb.proto.topic.YdbTopic.CommitOffsetRequest;
import tech.ydb.proto.topic.YdbTopic.CreateTopicRequest;
import tech.ydb.proto.topic.YdbTopic.DescribeConsumerRequest;
import tech.ydb.proto.topic.YdbTopic.DescribeConsumerResponse;
import tech.ydb.proto.topic.YdbTopic.DescribeConsumerResult;
import tech.ydb.proto.topic.YdbTopic.DescribeTopicRequest;
import tech.ydb.proto.topic.YdbTopic.DescribeTopicResponse;
import tech.ydb.proto.topic.YdbTopic.DescribeTopicResult;
import tech.ydb.proto.topic.YdbTopic.DropTopicRequest;
import tech.ydb.proto.topic.YdbTopic.DropTopicResponse;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage;
import tech.ydb.proto.topic.YdbTopic.UpdateOffsetsInTransactionRequest;
import tech.ydb.proto.topic.YdbTopic.UpdateOffsetsInTransactionResponse;
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
    public CompletableFuture<Status> createTopic(CreateTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getCreateTopicMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTopic.CreateTopicResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> alterTopic(AlterTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getAlterTopicMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(AlterTopicResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<DescribeTopicResult>> describeTopic(DescribeTopicRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getDescribeTopicMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(DescribeTopicResponse::getOperation, DescribeTopicResult.class));
    }

    @Override
    public CompletableFuture<Result<DescribeConsumerResult>> describeConsumer(DescribeConsumerRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getDescribeConsumerMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(DescribeConsumerResponse::getOperation,
                        DescribeConsumerResult.class));
    }

    @Override
    public CompletableFuture<Status> dropTopic(DropTopicRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getDropTopicMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(DropTopicResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> commitOffset(CommitOffsetRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getCommitOffsetMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTopic.CommitOffsetResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> updateOffsetsInTransaction(UpdateOffsetsInTransactionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TopicServiceGrpc.getUpdateOffsetsInTransactionMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(UpdateOffsetsInTransactionResponse::getOperation));
    }

    @Override
    public GrpcReadWriteStream<StreamWriteMessage.FromServer, StreamWriteMessage.FromClient> writeSession(String id) {
        GrpcRequestSettings settings = GrpcRequestSettings.newBuilder()
                .withTraceId(id)
                .disableDeadline()
                .build();
        return writeSession(settings);
    }

    @Override
    public GrpcReadWriteStream<StreamWriteMessage.FromServer, StreamWriteMessage.FromClient> writeSession(
            GrpcRequestSettings settings) {
        return transport.readWriteStreamCall(TopicServiceGrpc.getStreamWriteMethod(), settings);
    }


    @Override
    public GrpcReadWriteStream<StreamReadMessage.FromServer, StreamReadMessage.FromClient> readSession(String id) {
        GrpcRequestSettings settings = GrpcRequestSettings.newBuilder()
                .withTraceId(id)
                .disableDeadline()
                .build();
        return transport.readWriteStreamCall(TopicServiceGrpc.getStreamReadMethod(), settings);
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return transport.getScheduler();
    }
}
