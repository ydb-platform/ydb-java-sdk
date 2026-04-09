package tech.ydb.topic.write.impl;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.impl.GrpcStreamRetrier;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public abstract class WriterImpl extends GrpcStreamRetrier {
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);

    private final WriterQueue writeQueue;
    private final WriteSessionFactory sessionFactory;
    private final AtomicReference<CompletableFuture<InitResult>> initResultFutureRef = new AtomicReference<>(null);
    private final AtomicBoolean writeRequestInProgress = new AtomicBoolean();

    private volatile WriteSession session = null;
    private Boolean isSeqNoProvided = null;

    public WriterImpl(TopicRpc topicRpc,
                      WriterSettings settings,
                      Executor compressionExecutor,
                      @Nonnull CodecRegistry codecRegistry) {
        super(settings.getLogPrefix(), topicRpc.getScheduler(), settings.getErrorsHandler());
        this.writeQueue = new WriterQueue(id, settings, codecRegistry, compressionExecutor, this::sendDataRequest);
        this.sessionFactory = new WriteSessionFactory(topicRpc, settings);

        String message = "Writer" +
                " (generated id " + id + ")" +
                " created for topic \"" + settings.getTopicPath() + "\"" +
                " with producerId \"" + settings.getProducerId() + "\"" +
                " and messageGroupId \"" + settings.getMessageGroupId() + "\"";
        logger.info(message);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected String getStreamName() {
        return "Writer";
    }

    protected CompletableFuture<InitResult> initImpl() {
        logger.info("[{}] initImpl called", id);
        if (initResultFutureRef.compareAndSet(null, new CompletableFuture<>())) {
            session = sessionFactory.createNextSession();
            session.startAndInitialize();
        } else {
            logger.warn("[{}] Init is called on this writer more than once. Nothing is done", id);
        }
        return initResultFutureRef.get();
    }

    protected CompletableFuture<Void> flushImpl() {
        return writeQueue.flush();
    }

    private Message validate(Message message) {
        if (isStopped.get()) {
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

    @Override
    protected void onStreamReconnect() {
        session = sessionFactory.createNextSession();
        session.startAndInitialize();
    }

    @Override
    protected void onShutdown(String reason) {
        session.shutdown();
        if (initResultFutureRef.get() != null && !initResultFutureRef.get().isDone()) {
            initResultFutureRef.get().completeExceptionally(new RuntimeException(reason));
        }
    }

    void onInit(String streamId, long lastSeqNo) {
        reconnectCounter.set(0);
        Iterator<SentMessage> resend = writeQueue.updateSeqNo(lastSeqNo);
        session.sendAll(() -> resend.hasNext() ? resend.next() : null);
        if (initResultFutureRef.get() != null) {
            initResultFutureRef.get().complete(new InitResult(lastSeqNo));
        }
        sendDataRequest();
    }

    void onAck(WriteAck ack) {
        writeQueue.confirmAck(ack);
    }

    private void sendDataRequest() {
        if (!session.isStarted()) {
            logger.debug("[{}] Can't send data: current session is not yet initialized", id);
            return;
        }

        boolean hasMore = true; // TODO: Replace by SerialExecutor
        while (hasMore) {
            if (!writeRequestInProgress.compareAndSet(false, true)) {
                logger.debug("[{}] Send request is already in progress", id);
                return;
            }

            try {
                session.sendAll(writeQueue::nextMessageToSend);
            } finally {
                if (!writeRequestInProgress.compareAndSet(true, false)) {
                    logger.error("[{}] Couldn't turn off writeRequestInProgress. Should not happen", id);
                }
            }
            hasMore = writeQueue.hasMore();
        }
    }

    private class WriteSessionFactory {
        private final TopicRpc rpc;
        private final WriterSettings settings;
        private final AtomicLong sessionCounter = new AtomicLong(0);

        WriteSessionFactory(TopicRpc rpc, WriterSettings settings) {
            this.rpc = rpc;
            this.settings = settings;
        }

        public WriteSession createNextSession() {
            String streamID = id + '.' + sessionCounter.incrementAndGet();
            return new WriteSession(WriterImpl.this, rpc, streamID, settings);
        }
    }
}
