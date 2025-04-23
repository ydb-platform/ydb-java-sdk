package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public class AsyncWriterImpl extends WriterImpl implements AsyncWriter {

    public AsyncWriterImpl(TopicRpc topicRpc, WriterSettings settings, Executor compressionExecutor, CodecRegistry codecRegistry) {
        super(topicRpc, settings, compressionExecutor, codecRegistry);
    }

    @Override
    public CompletableFuture<InitResult> init() {
        return initImpl();
    }

    @Override
    public CompletableFuture<WriteAck> send(Message message, SendSettings settings) throws QueueOverflowException {
        try {
            return sendImpl(message, settings, true).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof QueueOverflowException) {
                throw (QueueOverflowException) e.getCause();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public CompletableFuture<WriteAck> send(Message message) throws QueueOverflowException {
        return send(message, null);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return shutdownImpl();
    }
}
