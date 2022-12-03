package tech.ydb.topic;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.rpc.Rpc;
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
     * Drop topic.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> dropTopic(YdbTopic.DropTopicRequest request, GrpcRequestSettings settings);

}
