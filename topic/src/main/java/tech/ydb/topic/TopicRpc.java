package tech.ydb.topic;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.topic.YdbTopic.CreateTopicRequest;


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

    GrpcReadWriteStream<YdbTopic.StreamWriteMessage.FromServer, YdbTopic.StreamWriteMessage.FromClient> writeSession();

}
