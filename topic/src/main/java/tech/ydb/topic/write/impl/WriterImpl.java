package tech.ydb.topic.write.impl;

import java.util.Iterator;
import java.util.List;
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
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
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

    private volatile WriteSessionImpl session;
    private final WriterSettings settings;
    private final WriterQueue writeQueue;
    private final MessageSender messageSender;
    private final TopicRpc topicRpc;
    private final AtomicReference<CompletableFuture<InitResult>> initResultFutureRef = new AtomicReference<>(null);
    private final AtomicBoolean writeRequestInProgress = new AtomicBoolean();

    // Every writing stream has a sequential number (for debug purposes)
    private final AtomicLong sessionSeqNumberCounter = new AtomicLong(0);

    private Boolean isSeqNoProvided = null;

    public WriterImpl(TopicRpc topicRpc,
                      WriterSettings settings,
                      Executor compressionExecutor,
                      @Nonnull CodecRegistry codecRegistry) {
        super(settings.getLogPrefix(), topicRpc.getScheduler(), settings.getErrorsHandler());
        this.writeQueue = new WriterQueue(id, settings, codecRegistry, compressionExecutor, this::sendDataRequest);
        this.topicRpc = topicRpc;
        this.settings = settings;
        this.session = new WriteSessionImpl();
        this.messageSender = new MessageSender(settings.getCodec());

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
        session = new WriteSessionImpl();
        session.startAndInitialize();
    }

    @Override
    protected void onShutdown(String reason) {
        session.shutdown();
        if (initResultFutureRef.get() != null && !initResultFutureRef.get().isDone()) {
            initResultFutureRef.get().completeExceptionally(new RuntimeException(reason));
        }
    }

    private void sendDataRequest() {
        String streamid = session.sessionId;
        if (!session.isInitialized.get()) {
            logger.debug("[{}] Can't send data: current session is not yet initialized", streamid);
            return;
        }

        if (session.isStopped()) {
            logger.debug("[{}] Can't send data: current session has been already stopped", streamid);
            return;
        }

        boolean hasMore = true; // TODO: Replace by SerialExecutor
        while (hasMore) {
            if (!writeRequestInProgress.compareAndSet(false, true)) {
                logger.debug("[{}] Send request is already in progress", streamid);
                return;
            }

            try {
                SentMessage next = writeQueue.nextMessageToSend();
                while (next != null) {
                    messageSender.sendMessage(next);
                    next = writeQueue.nextMessageToSend();
                }
                messageSender.flush();
            } finally {
                if (!writeRequestInProgress.compareAndSet(true, false)) {
                    logger.error("[{}] Couldn't turn off writeRequestInProgress. Should not happen", streamid);
                }
            }
            hasMore = writeQueue.hasMore();
        }
    }

    private class WriteSessionImpl extends WriteSession {
        protected String sessionId;
        private final AtomicBoolean isInitialized = new AtomicBoolean(false);

        private WriteSessionImpl() {
            super(topicRpc, id + '.' + sessionSeqNumberCounter.incrementAndGet());
        }

        @Override
        public void startAndInitialize() {
            logger.debug("[{}] Session startAndInitialize called", streamId);
            start(this::processMessage).whenComplete(this::closeDueToError);

            YdbTopic.StreamWriteMessage.InitRequest.Builder initRequestBuilder = YdbTopic.StreamWriteMessage.InitRequest
                    .newBuilder()
                    .setPath(settings.getTopicPath());
            String producerId = settings.getProducerId();
            if (producerId != null) {
                initRequestBuilder.setProducerId(producerId);
            }
            String messageGroupId = settings.getMessageGroupId();
            Long partitionId = settings.getPartitionId();
            if (messageGroupId != null) {
                if (partitionId != null) {
                    throw new IllegalArgumentException("Both MessageGroupId and PartitionId are set in WriterSettings");
                }
                initRequestBuilder.setMessageGroupId(messageGroupId);
            } else if (partitionId != null) {
                initRequestBuilder.setPartitionId(partitionId);
            }
            send(YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                    .setInitRequest(initRequestBuilder)
                    .build());
        }

        private void onInitResponse(YdbTopic.StreamWriteMessage.InitResponse response) {
            sessionId = response.getSessionId();
            long lastSeqNo = response.getLastSeqNo();
            logger.info("[{}] Session with id {} (partition {}) initialized for topic \"{}\", lastSeqNo {}",
                    streamId, sessionId, response.getPartitionId(), settings.getTopicPath(), lastSeqNo);

            messageSender.setSession(this);
            Iterator<SentMessage> resend = writeQueue.updateSeqNo(lastSeqNo);
            while (resend.hasNext()) {
                messageSender.sendMessage(resend.next());
            }
            messageSender.flush();

            if (initResultFutureRef.get() != null) {
                initResultFutureRef.get().complete(new InitResult(lastSeqNo));
            }
            isInitialized.set(true);
            sendDataRequest();
        }

        // Shouldn't be called more than once at a time due to grpc guarantees
        private void onWriteResponse(YdbTopic.StreamWriteMessage.WriteResponse response) {
            List<YdbTopic.StreamWriteMessage.WriteResponse.WriteAck> acks = response.getAcksList();
            logger.debug("[{}] Received WriteResponse with {} WriteAcks", streamId, acks.size());
            WriteAck.Statistics statistics = null;
            if (response.getWriteStatistics() != null) {
                YdbTopic.StreamWriteMessage.WriteResponse.WriteStatistics src = response.getWriteStatistics();
                statistics = new WriteAck.Statistics(
                    ProtobufUtils.protoToDuration(src.getPersistingTime()),
                    ProtobufUtils.protoToDuration(src.getPartitionQuotaWaitTime()),
                    ProtobufUtils.protoToDuration(src.getTopicQuotaWaitTime()),
                    ProtobufUtils.protoToDuration(src.getMaxQueueWaitTime()),
                    ProtobufUtils.protoToDuration(src.getMinQueueWaitTime())
                );
            }

            for (YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack : acks) {
                writeQueue.confirmAck(ack.getSeqNo(), mapAck(statistics, ack));
            }
        }

        private void processMessage(YdbTopic.StreamWriteMessage.FromServer message) {
            if (message.getStatus() == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
                reconnectCounter.set(0);
            } else {
                Status status = Status.of(StatusCode.fromProto(message.getStatus()),
                        Issue.fromPb(message.getIssuesList()));
                logger.warn("[{}] Got non-success status in processMessage method: {}", streamId, status);
                closeDueToError(status, null);
                return;
            }
            if (message.hasInitResponse()) {
                onInitResponse(message.getInitResponse());
            } else if (message.hasWriteResponse()) {
                onWriteResponse(message.getWriteResponse());
            } else if (message.hasUpdateTokenResponse()) {
                logger.debug("[{}] got update token response", streamId);
            } else {
                logger.warn("[{}] got unknown type message", streamId);
            }
        }

        WriteAck mapAck(WriteAck.Statistics statistics, YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack) {
            logger.debug("[{}] Received WriteAck with seqNo {} and status {}", streamId, ack.getSeqNo(),
                    ack.getMessageWriteStatusCase());
            if (ack.hasSkipped()) {
                return new WriteAck(ack.getSeqNo(), WriteAck.State.ALREADY_WRITTEN, null, statistics);
            }
            if (ack.hasWrittenInTx()) {
                return new WriteAck(ack.getSeqNo(), WriteAck.State.WRITTEN_IN_TX, null, statistics);
            }
            if (ack.hasWritten()) {
                WriteAck.Details details = new WriteAck.Details(ack.getWritten().getOffset());
                return new WriteAck(ack.getSeqNo(), WriteAck.State.WRITTEN, details, statistics);
            }

            // Unknown type of write ack
            return null;
        }

        private void closeDueToError(Status status, Throwable th) {
            logger.info("[{}] Session {} closeDueToError called", streamId, sessionId);
            if (shutdown()) {
                // Signal writer to retry
                onSessionClosed(status, th);
            }
        }

        @Override
        protected void onStop() {
            logger.debug("[{}] Session {} onStop called", streamId, sessionId);
        }
    }
}
