package tech.ydb.topic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.topic.YdbTopic.AlterTopicRequest;
import tech.ydb.proto.topic.YdbTopic.CommitOffsetRequest;
import tech.ydb.proto.topic.YdbTopic.CreateTopicRequest;
import tech.ydb.proto.topic.YdbTopic.DescribeConsumerRequest;
import tech.ydb.proto.topic.YdbTopic.DescribeConsumerResult;
import tech.ydb.proto.topic.YdbTopic.DescribeTopicRequest;
import tech.ydb.proto.topic.YdbTopic.DescribeTopicResult;
import tech.ydb.proto.topic.YdbTopic.DropTopicRequest;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage;
import tech.ydb.proto.topic.YdbTopic.UpdateOffsetsInTransactionRequest;


/**
 * @author Nikolay Perfilov
 */
public interface TopicRpc {

    /**
     * Create topic.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> createTopic(CreateTopicRequest request, GrpcRequestSettings settings);

    /**
     * Alter topic.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> alterTopic(AlterTopicRequest request, GrpcRequestSettings settings);

    /**
     * Drop topic.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> dropTopic(DropTopicRequest request, GrpcRequestSettings settings);

    /**
     * Describe topic.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<DescribeTopicResult>> describeTopic(DescribeTopicRequest request,
            GrpcRequestSettings settings);

    /**
     * Describe consumer.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<DescribeConsumerResult>> describeConsumer(
            DescribeConsumerRequest request, GrpcRequestSettings settings
    );

    /**
     * Commit offset.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Status> commitOffset(CommitOffsetRequest request, GrpcRequestSettings settings);

    /**
     * Updates offsets in transaction.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Status> updateOffsetsInTransaction(UpdateOffsetsInTransactionRequest request,
                                                         GrpcRequestSettings settings);

    GrpcReadWriteStream<StreamWriteMessage.FromServer, StreamWriteMessage.FromClient> writeSession(String traceId);

    default GrpcReadWriteStream<StreamWriteMessage.FromServer, StreamWriteMessage.FromClient> writeSession(
            String traceId, Integer directWriteNodeId) {
        return writeSession(traceId);
    }

    GrpcReadWriteStream<StreamReadMessage.FromServer, StreamReadMessage.FromClient> readSession(String traceId);

    ScheduledExecutorService getScheduler();
}
