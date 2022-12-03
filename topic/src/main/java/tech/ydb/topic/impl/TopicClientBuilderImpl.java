package tech.ydb.topic.impl;

import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;


/**
 * @author Nikolay Perfilov
 */
public class TopicClientBuilderImpl implements TopicClient.Builder {

    protected final TopicRpc topicRpc;

    public TopicClientBuilderImpl(TopicRpc topicRpc) {
        this.topicRpc = topicRpc;
    }

    @Override
    public TopicClient build() {
        return new TopicClientImpl(this);
    }
}
