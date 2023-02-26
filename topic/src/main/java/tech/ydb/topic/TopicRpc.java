package tech.ydb.topic;

import java.util.concurrent.CompletableFuture;

import io.grpc.CallOptions;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.rpc.OutStreamObserver;
import tech.ydb.core.rpc.Rpc;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.topic.YdbTopic.CreateTopicRequest;


/**
 * @author Nikolay Perfilov
 */
public interface TopicRpc extends Rpc {

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

    OutStreamObserver<YdbTopic.StreamWriteMessage.FromClient> writeSession(
            StreamObserver<YdbTopic.StreamWriteMessage.FromServer> observer);

    CallOptions getCallOptions();

}
