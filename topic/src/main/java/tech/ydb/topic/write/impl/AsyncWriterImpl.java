package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public class AsyncWriterImpl extends WriterImpl implements AsyncWriter {

    public AsyncWriterImpl(TopicRpc topicRpc, WriterSettings settings, Executor compressionExecutor) {
        super(topicRpc, settings, compressionExecutor);
    }

    @Override
    public CompletableFuture<InitResult> init() {
        return initImpl();
    }

    @Override
    public CompletableFuture<WriteAck> send(Message message) {
        return sendImpl(message).join();
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return shutdownImpl();
    }
}
