package tech.ydb.topic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import tech.ydb.core.metrics.Meter;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;


/**
 * @author Nikolay Perfilov
 */
public class TopicClientBuilderImpl implements TopicClient.Builder {

    protected final TopicRpc topicRpc;
    protected final List<Codec> codecs = new ArrayList<>(StandardCodecs.getAvailableCodecs());
    protected Integer compressionExecutorThreadCount;
    protected Executor compressionExecutor;
    protected Meter meter = Meter.NOOP;

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
    public TopicClientBuilderImpl withMeter(Meter meter) {
        if (meter == null) {
            throw new IllegalArgumentException("Meter must be not null");
        }
        this.meter = meter;
        return this;
    }

    @Override
    public TopicClientBuilderImpl registerCodec(Codec codec) {
        if (codec == null) {
            throw new IllegalArgumentException("Codec must be not null");
        }
        this.codecs.add(codec);
        return this;
    }

    @Override
    public TopicClient build() {
        return new TopicClientImpl(this);
    }
}
