package tech.ydb.topic.write.impl;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.impl.DebugTools;
import tech.ydb.topic.impl.SerialRunnable;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public class WriterImpl {
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);

    private final String debugId;
    private final WriterQueue writeQueue;
    private final WriteSession stream;
    private final Runnable sendTask = new SerialRunnable(new SendTask());

    private final CompletableFuture<InitResult> initFuture = new CompletableFuture<>();
    private final CompletableFuture<Status> shutdownFuture = new CompletableFuture<>();

    private volatile boolean isReady = false;
    private Boolean isSeqNoProvided = null;

    public WriterImpl(TopicRpc topicRpc,
                      WriterSettings settings,
                      Executor compressionExecutor,
                      @Nonnull CodecRegistry codecRegistry) {
        this.debugId = DebugTools.createDebugId(settings.getLogPrefix());
        this.stream = new WriteSession(debugId, topicRpc, settings, new ListenerImpl());
        this.writeQueue = new WriterQueue(debugId, settings, codecRegistry, compressionExecutor, sendTask);

        logger.info("Writer with id {} created for topic \"{}\" with producerId \"{}\" and messageGroupId \"{}\"",
                debugId, settings.getTopicPath(), settings.getProducerId(), settings.getMessageGroupId());
    }

    public CompletableFuture<InitResult> init() {
        logger.info("[{}] start called", debugId);
        stream.start();
        return initFuture;
    }

    public CompletableFuture<Void> shutdown() {
        stream.close();
        return shutdownFuture.thenApply(s -> null);
    }

    public CompletableFuture<Void> flush() {
        return writeQueue.flush();
    }

    private Message validate(Message message) {
        if (shutdownFuture.isDone()) {
            throw new RuntimeException("Writer is already stopped");
        }
        if (isSeqNoProvided != null) {
            if (message.getSeqNo() != null && !isSeqNoProvided) {
                throw new RuntimeException(
                        "SeqNo was provided for a message after it had not been provided for another message. " +
                                "SeqNo should either be provided for all messages or none of them.");
            }
            if (message.getSeqNo() == null && isSeqNoProvided) {
                throw new RuntimeException(
                        "SeqNo was not provided for a message after it had been provided for another message. " +
                                "SeqNo should either be provided for all messages or none of them.");
            }
        } else {
            isSeqNoProvided = message.getSeqNo() != null;
        }
        return message;
    }

    private YdbTransaction getTx(SendSettings sendSettings) {
        return sendSettings != null ? sendSettings.getTransaction() : null;
    }

    protected CompletableFuture<WriteAck> blockingSend(Message msg, SendSettings settings)
            throws QueueOverflowException, InterruptedException {
        return writeQueue.enqueue(validate(msg), getTx(settings));
    }

    protected CompletableFuture<WriteAck> blockingSend(Message msg, SendSettings settings, long timeout, TimeUnit unit)
            throws QueueOverflowException, InterruptedException, TimeoutException {
        return writeQueue.tryEnqueue(validate(msg), getTx(settings), timeout, unit);
    }

    protected CompletableFuture<WriteAck> nonblockingSend(Message msg, SendSettings settings)
            throws QueueOverflowException {
        return writeQueue.tryEnqueue(validate(msg), getTx(settings));
    }

    private class ListenerImpl implements WriteSession.Listener {
        @Override
        public void onStart(long lastSeqNo, String sessionId) {
            // resend all sent messages in writing queue
            Iterator<SentMessage> resend = writeQueue.updateSeqNo(lastSeqNo);
            stream.sendAll(() -> resend.hasNext() ? resend.next() : null);
            isReady = true;
            initFuture.complete(new InitResult(lastSeqNo));
            sendTask.run();
        }

        @Override
        public void onStop(Status status) {
            isReady = false;
        }

        @Override
        public void onAck(WriteAck ack) {
            writeQueue.confirmAck(ack);
        }

        @Override
        public void onClose(Status status) {
            initFuture.completeExceptionally(new UnexpectedResultException("Cannot init write session", status));
            shutdownFuture.complete(status);
            writeQueue.close(status);
        }
    }

    private class SendTask implements Runnable {
        @Override
        public void run() {
            if (!isReady) {
                logger.debug("[{}] Can't send data: current session is not ready yet", debugId);
                return;
            }

            stream.sendAll(writeQueue::nextMessageToSend);
        }
    }
}
