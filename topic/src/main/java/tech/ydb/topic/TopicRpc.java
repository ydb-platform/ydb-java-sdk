package tech.ydb.topic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.topic.YdbTopic;


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
    CompletableFuture<Status> createTopic(YdbTopic.CreateTopicRequest request, GrpcRequestSettings settings);

    /**
     * Alter topic.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> alterTopic(YdbTopic.AlterTopicRequest request, GrpcRequestSettings settings);

    /**
     * Drop topic.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> dropTopic(YdbTopic.DropTopicRequest request, GrpcRequestSettings settings);

    /**
     * Describe topic.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<YdbTopic.DescribeTopicResult>> describeTopic(YdbTopic.DescribeTopicRequest request,
                                                                          GrpcRequestSettings settings);

    /**
     * Describe consumer.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<YdbTopic.DescribeConsumerResult>> describeConsumer(
            YdbTopic.DescribeConsumerRequest request, GrpcRequestSettings settings
    );

    /**
     * Commit offset.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Status> commitOffset(YdbTopic.CommitOffsetRequest request, GrpcRequestSettings settings);

    /**
     * Updates offsets in transaction.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Status> updateOffsetsInTransaction(YdbTopic.UpdateOffsetsInTransactionRequest request,
                                                         GrpcRequestSettings settings);

    GrpcReadWriteStream<YdbTopic.StreamWriteMessage.FromServer, YdbTopic.StreamWriteMessage.FromClient> writeSession(
            String traceId
    );

    GrpcReadWriteStream<YdbTopic.StreamReadMessage.FromServer, YdbTopic.StreamReadMessage.FromClient> readSession(
            String traceId
    );

    ScheduledExecutorService getScheduler();
}
