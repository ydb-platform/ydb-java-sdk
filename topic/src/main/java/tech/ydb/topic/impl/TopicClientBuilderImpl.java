package tech.ydb.topic.impl;

import java.util.concurrent.Executor;

import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;


/**
 * @author Nikolay Perfilov
 */
public class TopicClientBuilderImpl implements TopicClient.Builder {

    protected final TopicRpc topicRpc;
    protected Integer compressionExecutorThreadCount;
    protected Executor compressionExecutor;

    public TopicClientBuilderImpl(TopicRpc topicRpc) {
        this.topicRpc = topicRpc;
    }

    @Override
    public TopicClientBuilderImpl setCompressionPoolThreadCount(Integer compressionPoolThreadCount) {
        this.compressionExecutorThreadCount = compressionPoolThreadCount;
        return this;
    }

    @Override
    public TopicClientBuilderImpl setCompressionExecutor(Executor compressionExecutor) {
        this.compressionExecutor = compressionExecutor;
        return this;
    }

    @Override
    public TopicClient build() {
        return new TopicClientImpl(this);
    }
}
