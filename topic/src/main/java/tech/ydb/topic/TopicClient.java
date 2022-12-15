package tech.ydb.topic;

import java.util.concurrent.CompletableFuture;

import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.topic.description.TopicDescription;
import tech.ydb.topic.impl.GrpcTopicRpc;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.ReadSession;
import tech.ydb.topic.settings.AlterTopicSettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.DescribeTopicSettings;
import tech.ydb.topic.settings.DropTopicSettings;
import tech.ydb.topic.settings.ReadSessionSettings;


/**
 * @author Nikolay Perfilov
 */
public interface TopicClient extends AutoCloseable {

    static Builder newClient(@WillNotClose GrpcTransport transport) {
        return TopicClientImpl.newClient(GrpcTopicRpc.useTransport(transport));
    }

    /**
     * Create topic.
     *
     * Parent directories must be already present.
     * @param path  path to topic
     * @param settings  topic creation settings
     * @return operation status
     */
    CompletableFuture<Status> createTopic(String path, CreateTopicSettings settings);

    /**
     * Alter topic.
     *
     * @param path  path to topic
     * @param settings  alter topic settings
     * @return operation status
     */
    CompletableFuture<Status> alterTopic(String path, AlterTopicSettings settings);

    /**
     * Drop topic.
     *
     * @param path  path to topic
     * @param settings  request settings (i.e. timeouts)
     * @return operation status
     */
    CompletableFuture<Status> dropTopic(String path, DropTopicSettings settings);

    /**
     * Drop topic.
     *
     * @param path  path to topic
     * @return operation status
     */
    default CompletableFuture<Status> dropTopic(String path) {
        return dropTopic(path, new DropTopicSettings());
    }

    /**
     * Describe topic.
     *
     * Receives all topic propertiens.
     * @param path  path to topic
     * @param settings  request settings
     * @return operation status
     */
    CompletableFuture<Result<TopicDescription>> describeTopic(String path, DescribeTopicSettings settings);

    /**
     * Describe topic.
     *
     * Receives all topic propertiens.
     * @param path  path to topic
     * @return operation status
     */
    default CompletableFuture<Result<TopicDescription>> describeTopic(String path) {
        return describeTopic(path, new DescribeTopicSettings());
    }

    ReadSession createReadSession(ReadSessionSettings settings);

    @Override
    void close();

    /**
     * BUILDER
     */
    interface Builder {

        TopicClient build();
    }
}
