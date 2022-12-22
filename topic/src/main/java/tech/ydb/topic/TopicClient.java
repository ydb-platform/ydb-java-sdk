package tech.ydb.topic;

import java.util.concurrent.CompletableFuture;

import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.topic.description.TopicDescription;
import tech.ydb.topic.impl.GrpcTopicRpc;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.Reader;
import tech.ydb.topic.settings.AlterTopicSettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.DescribeTopicSettings;
import tech.ydb.topic.settings.DropTopicSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.Writer;


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
     * @return {@link CompletableFuture} to operation status
     */
    CompletableFuture<Status> createTopic(String path, CreateTopicSettings settings);

    /**
     * Alter topic.
     *
     * @param path  path to topic
     * @param settings  alter topic settings
     * @return {@link CompletableFuture} to operation status
     */
    CompletableFuture<Status> alterTopic(String path, AlterTopicSettings settings);

    /**
     * Drop topic.
     *
     * @param path  path to topic
     * @param settings  request settings (i.e. timeouts)
     * @return {@link CompletableFuture} to operation status
     */
    CompletableFuture<Status> dropTopic(String path, DropTopicSettings settings);

    /**
     * Drop topic.
     *
     * @param path  path to topic
     * @return {@link CompletableFuture} to operation status
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
     * @return {@link CompletableFuture} to a result with {@link TopicDescription}
     */
    CompletableFuture<Result<TopicDescription>> describeTopic(String path, DescribeTopicSettings settings);

    /**
     * Describe topic.
     *
     * Receives all topic propertiens.
     * @param path  path to topic
     * @return {@link CompletableFuture} to a result with {@link TopicDescription}
     */
    default CompletableFuture<Result<TopicDescription>> describeTopic(String path) {
        return describeTopic(path, new DescribeTopicSettings());
    }

    /**
     * Create topic reader.
     *
     * @param settings  reader settings
     * @return topic {@link Reader}
     */
    Reader createReader(ReaderSettings settings);

    /**
     * Create topic writer.
     *
     * @param settings  {@link WriterSettings}
     * @return topic {@link Writer}
     */
    Writer createWriter(WriterSettings settings);

    @Override
    void close();

    /**
     * BUILDER
     */
    interface Builder {

        TopicClient build();
    }
}
