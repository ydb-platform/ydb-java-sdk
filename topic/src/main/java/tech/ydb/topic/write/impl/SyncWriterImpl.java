package tech.ydb.topic.write.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.SyncWriter;

/**
 * @author Nikolay Perfilov
 */
public class SyncWriterImpl implements SyncWriter {
    private final WriterImpl impl;

    public SyncWriterImpl(TopicRpc topicRpc,
                          WriterSettings settings,
                          Executor compressionExecutor,
                          @Nonnull CodecRegistry codecRegistry) {
        this.impl = new WriterImpl(topicRpc, settings, compressionExecutor, codecRegistry);
    }

    @Override
    public void init() {
        impl.init();
    }

    @Override
    public InitResult initAndWait() {
        return impl.init().join();
    }

    @Override
    public void send(Message message, SendSettings sendSettings) {
        try {
            impl.blockingSend(message, sendSettings);
        } catch (InterruptedException | QueueOverflowException ex) {
            throw new RuntimeException("Cannot send a message", ex);
        }
    }

    @Override
    public void send(Message message, SendSettings sendSettings, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            impl.blockingSend(message, sendSettings, timeout, unit);
        } catch (QueueOverflowException ex) {
            throw new RuntimeException("Cannot send a message", ex);
        }
    }

    @Override
    public void flush() {
        impl.flush().join();
    }

    @Override
    public void shutdown(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        impl.shutdown().get(timeout, unit);
    }
}
