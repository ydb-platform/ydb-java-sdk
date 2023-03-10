package tech.ydb.topic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.topic.description.TopicDescription;
import tech.ydb.topic.impl.GrpcTopicRpc;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.AlterTopicSettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.DescribeTopicSettings;
import tech.ydb.topic.settings.DropTopicSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.SyncWriter;


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
     * Receives all topic properties.
     *
     * @param path  path to topic
     * @param settings  request settings
     * @return {@link CompletableFuture} to a result with {@link TopicDescription}
     */
    CompletableFuture<Result<TopicDescription>> describeTopic(String path, DescribeTopicSettings settings);

    /**
     * Describe topic.
     *
     * Receives all topic properties.
     * @param path  path to topic
     * @return {@link CompletableFuture} to a result with {@link TopicDescription}
     */
    default CompletableFuture<Result<TopicDescription>> describeTopic(String path) {
        return describeTopic(path, new DescribeTopicSettings());
    }

    /**
     * Create sync topic reader.
     *
     * @param settings  reader settings
     * @return topic {@link SyncReader}
     */
    SyncReader createSyncReader(ReaderSettings settings);

    /**
     * Create async topic reader.
     *
     * @param settings  reader settings
     * @param handlersSettings  settings for read event handling
     * @return topic {@link AsyncReader}
     */
    AsyncReader createAsyncReader(ReaderSettings settings, ReadEventHandlersSettings handlersSettings);

    /**
     * Create sync topic writer.
     *
     * @param settings  {@link WriterSettings}
     * @return topic {@link SyncWriter}
     */
    SyncWriter createSyncWriter(WriterSettings settings);

    /**
     * Create async topic writer.
     *
     * @param settings  {@link WriterSettings}
     * @return topic {@link AsyncWriter}
     */
    AsyncWriter createAsyncWriter(WriterSettings settings);

    @Override
    void close();

    /**
     * BUILDER
     */
    interface Builder {
        TopicClient build();

        /**
         * Set executor for compression tasks.
         * If not set, default executor will be used.
         * You can change default executor thread count with setCompressionPoolThreadCount.
         * If compressionExecutor is set, it will not be affected by compressionPoolThreadCount parameter.
         * @param compressionExecutor  executor for compression tasks
         * @return settings builder
         */
        Builder setCompressionExecutor(Executor compressionExecutor);

        /**
         * Set default executor thread count for compression tasks.
         * If not set, default number of threads will be used.
         * If compressionExecutor is set, current parameter will be ignored.
         * @param compressionPoolThreadCount  executor thread count for compression tasks
         * @return settings builder
         */
        Builder setCompressionPoolThreadCount(Integer compressionPoolThreadCount);

    }
}
