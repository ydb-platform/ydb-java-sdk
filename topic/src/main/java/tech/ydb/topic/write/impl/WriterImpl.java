package tech.ydb.topic.write.impl;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
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
    private final TopicRpc topicRpc;
    private final AtomicReference<CompletableFuture<InitResult>> initResultFutureRef = new AtomicReference<>(null);
    // Messages that are waiting for being put into sending queue due to queue overflow
    private final Queue<IncomingMessage> incomingQueue = new LinkedList<>();
    private final ReentrantLock incomingQueueLock = new ReentrantLock();
    // Messages that are currently encoding
    private final Deque<EnqueuedMessage> encodingMessages = new LinkedList<>();
    // Messages that are taken into send buffer, are already compressed and are waiting for being sent
    private final Queue<EnqueuedMessage> sendingQueue = new ConcurrentLinkedQueue<>();
    // Messages that are currently trying to be sent and haven't received a response from server yet
    private final Deque<EnqueuedMessage> sentMessages = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean writeRequestInProgress = new AtomicBoolean();
    private final Executor compressionExecutor;
    private final long maxSendBufferMemorySize;

    // Every writing stream has a sequential number (for debug purposes)
    private final AtomicLong sessionSeqNumberCounter = new AtomicLong(0);

    /**
     * Register for custom codec. User can specify custom codec which is local to TopicClient
     */
    private final CodecRegistry codecRegistry;

    private Boolean isSeqNoProvided = null;
    private int currentInFlightCount = 0;
    private long availableSizeBytes;
    // Future for flush method
    private CompletableFuture<WriteAck> lastAcceptedMessageFuture;

    public WriterImpl(TopicRpc topicRpc,
                      WriterSettings settings,
                      Executor compressionExecutor,
                      @Nonnull CodecRegistry codecRegistry) {
        super(settings.getLogPrefix(), topicRpc.getScheduler(), settings.getErrorsHandler());
        this.topicRpc = topicRpc;
        this.settings = settings;
        this.session = new WriteSessionImpl();
        this.availableSizeBytes = settings.getMaxSendBufferMemorySize();
        this.maxSendBufferMemorySize = settings.getMaxSendBufferMemorySize();
        this.compressionExecutor = compressionExecutor;
        this.codecRegistry = codecRegistry;
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

    private static class IncomingMessage {
        private final EnqueuedMessage message;
        private final CompletableFuture<Void> future = new CompletableFuture<>();

        private IncomingMessage(EnqueuedMessage message) {
            this.message = message;
        }
    }

    public CompletableFuture<Void> tryToEnqueue(EnqueuedMessage message, boolean instant) {
        incomingQueueLock.lock();

        try {
            if (currentInFlightCount >= settings.getMaxSendBufferMessagesCount()) {
                if (instant) {
                    logger.info("[{}] Rejecting a message due to reaching message queue in-flight limit of {}", id,
                            settings.getMaxSendBufferMessagesCount());
                    CompletableFuture<Void> result = new CompletableFuture<>();
                    result.completeExceptionally(new QueueOverflowException("Message queue in-flight limit of "
                            + settings.getMaxSendBufferMessagesCount() + " reached"));
                    return result;
                } else {
                    logger.info("[{}] Message queue in-flight limit of {} reached. Putting the message into incoming " +
                            "waiting queue", id, settings.getMaxSendBufferMessagesCount());
                }
            } else if (availableSizeBytes < message.getSize()) {
                if (instant) {
                    String errorMessage = "[" + id + "] Rejecting a message of " +
                            message.getSize() +
                            " bytes: not enough space in message queue. Buffer currently has " + currentInFlightCount +
                            " messages with " + availableSizeBytes + " / " + settings.getMaxSendBufferMemorySize() +
                            " bytes available";
                    logger.info(errorMessage);
                    CompletableFuture<Void> result = new CompletableFuture<>();
                    result.completeExceptionally(new QueueOverflowException(errorMessage));
                    return result;
                } else {
                    logger.info("[{}] Can't accept a message of {} bytes into message queue. Buffer currently has " +
                                    "{} messages with {} / {} bytes available. Putting the message into incoming " +
                                    "waiting queue.", id, message.getSize(), currentInFlightCount,
                            availableSizeBytes, settings.getMaxSendBufferMemorySize());
                }
            } else if (incomingQueue.isEmpty()) {
                acceptMessageIntoSendingQueue(message);
                return CompletableFuture.completedFuture(null);
            }

            IncomingMessage incomingMessage = new IncomingMessage(message);
            incomingQueue.add(incomingMessage);
            return incomingMessage.future;
        } finally {
            incomingQueueLock.unlock();
        }
    }

    // should be done under incomingQueueLock
    private void acceptMessageIntoSendingQueue(EnqueuedMessage message) {
        this.lastAcceptedMessageFuture = message.getFuture();
        this.currentInFlightCount++;
        this.availableSizeBytes -= message.getOriginalSize();
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] Accepted 1 message of {} uncompressed bytes. Current In-flight: {}, " +
                            "AvailableSizeBytes: {} ({} / {} acquired)", id, message.getOriginalSize(),
                    currentInFlightCount, availableSizeBytes, maxSendBufferMemorySize - availableSizeBytes,
                    maxSendBufferMemorySize);
        }

        this.encodingMessages.add(message);

        if (message.isReady()) {
            moveEncodedMessagesToSendingQueue();
        } else {
            CompletableFuture
                    .runAsync(() -> message.encode(id, settings.getCodec(), codecRegistry), compressionExecutor)
                    .thenRun(this::moveEncodedMessagesToSendingQueue);
        }
    }

    private void moveEncodedMessagesToSendingQueue() {
        boolean haveNewMessagesToSend = false;
        // Working with encodingMessages under incomingQueueLock to prevent deadlocks while working with free method
        incomingQueueLock.lock();

        try {
            // Taking all encoded messages to sending queue
            while (true) {
                EnqueuedMessage msg = encodingMessages.pollFirst();
                if (msg == null) {
                    break;
                }

                if (!msg.isReady()) {
                    encodingMessages.addFirst(msg);
                    break;
                }

                IOException error = msg.getCompressError();
                if (error != null) { // just skip
                    logger.warn("[{}] Message wasn't sent because of processing error", id, error);
                    free(1, msg.getOriginalSize());
                    continue;
                }

                if (msg.getOriginalSize() != msg.getSize()) {
                    logger.trace("[{}] Message compressed from {} to {} bytes", id, msg.getOriginalSize(),
                            msg.getSize());
                    // message was actually encoded. Need to free some bytes
                    long bytesFreed = msg.getOriginalSize() - msg.getSize();
                    // bytesFreed can be less than 0
                    free(0, bytesFreed);
                }

                logger.debug("[{}] Adding message to sending queue", id);
                sendingQueue.add(msg);
                haveNewMessagesToSend = true;
            }
        } finally {
            incomingQueueLock.unlock();
        }
        if (haveNewMessagesToSend) {
            session.sendDataRequestIfNeeded();
        }
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

    // Outer future completes when message is put (or declined) into send buffer
    // Inner future completes on receiving write ack from server
    protected CompletableFuture<CompletableFuture<WriteAck>> sendImpl(Message message, SendSettings sendSettings,
                                                                      boolean instant) {
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

        EnqueuedMessage enqueuedMessage = new EnqueuedMessage(message, sendSettings, settings.getCodec() == Codec.RAW);

        return tryToEnqueue(enqueuedMessage, instant).thenApply(v -> enqueuedMessage.getFuture());
    }

    /**
     * Create a wrapper upon the future for the flush method.
     *
     * @return an empty Future if successful. Throw CompletionException when an error occurs.
     */
    protected CompletableFuture<Void> flushImpl() {
        if (this.lastAcceptedMessageFuture == null) {
            return CompletableFuture.completedFuture(null);
        }
        incomingQueueLock.lock();

        try {
            return this.lastAcceptedMessageFuture.thenApply(v -> null);
        } finally {
            incomingQueueLock.unlock();
        }
    }

    private void free(int messageCount, long sizeBytes) {
        incomingQueueLock.lock();

        try {
            currentInFlightCount -= messageCount;
            availableSizeBytes += sizeBytes;
            if (logger.isTraceEnabled()) {
                logger.trace("[{}] Freed {} bytes in {} messages. Current In-flight: {}, current availableSize: {} " +
                                "({} / {} acquired)", id, sizeBytes, messageCount, currentInFlightCount,
                        availableSizeBytes, maxSendBufferMemorySize - availableSizeBytes, maxSendBufferMemorySize);
            }

            // Try to add waiting messages into send buffer
            if (sizeBytes > 0 && !incomingQueue.isEmpty()) {
                while (true) {
                    IncomingMessage incomingMessage = incomingQueue.peek();
                    if (incomingMessage == null) {
                        break;
                    }
                    if (incomingMessage.message.getOriginalSize() > availableSizeBytes
                            || currentInFlightCount >= settings.getMaxSendBufferMessagesCount()) {
                        logger.trace("[{}] There are messages in incomingQueue still, but no space in send buffer", id);
                        return;
                    }
                    logger.trace("[{}] Putting a message into send buffer after freeing some space", id);
                    incomingQueue.remove();
                    if (incomingMessage.future.complete(null)) {
                        acceptMessageIntoSendingQueue(incomingMessage.message);
                    }
                }
                logger.trace("[{}] All messages from incomingQueue are accepted into send buffer", id);
            }
        } finally {
            incomingQueueLock.unlock();
        }
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

    private class WriteSessionImpl extends WriteSession {
        protected String sessionId;
        private final MessageSender messageSender;
        private final AtomicBoolean isInitialized = new AtomicBoolean(false);

        private WriteSessionImpl() {
            super(topicRpc, id + '.' + sessionSeqNumberCounter.incrementAndGet());
            this.messageSender = new MessageSender(settings);
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

        private void sendDataRequestIfNeeded() {
            while (true) {
                if (!isInitialized.get()) {
                    logger.debug("[{}] Can't send data: current session is not yet initialized", streamId);
                    return;
                }
                if (!isWorking.get()) {
                    logger.debug("[{}] Can't send data: current session has been already stopped",
                            streamId);
                    return;
                }
                Queue<EnqueuedMessage> messages;
                if (sendingQueue.isEmpty()) {
                    logger.trace("[{}] Nothing to send -- sendingQueue is empty", streamId);
                    return;
                }
                if (!writeRequestInProgress.compareAndSet(false, true)) {
                    logger.debug("[{}] Send request is already in progress", streamId);
                    return;
                }
                // This code can be run in one thread at a time due to acquiring writeRequestInProgress
                messages = new LinkedList<>(sendingQueue);
                // Checking second time under writeRequestInProgress "lock"
                if (messages.isEmpty()) {
                    logger.debug("[{}] Nothing to send -- sendingQueue is empty #2", streamId);
                } else {
                    sendingQueue.removeAll(messages);
                    sentMessages.addAll(messages);
                    messageSender.sendMessages(messages);
                    logger.debug("[{}] Sent {} messages to server", streamId, messages.size());
                }
                if (!writeRequestInProgress.compareAndSet(true, false)) {
                    logger.error("[{}] Couldn't turn off writeRequestInProgress. Should not happen", streamId);
                }
            }
        }

        private void onInitResponse(YdbTopic.StreamWriteMessage.InitResponse response) {
            sessionId = response.getSessionId();
            long lastSeqNo = response.getLastSeqNo();
            long actualLastSeqNo = lastSeqNo;
            logger.info("[{}] Session with id {} (partition {}) initialized for topic \"{}\", lastSeqNo {}," +
                            " actualLastSeqNo {}", streamId, sessionId, response.getPartitionId(),
                    settings.getTopicPath(), lastSeqNo, actualLastSeqNo);
            messageSender.setSession(this);
            messageSender.setSeqNo(actualLastSeqNo);
            // TODO: remember supported codecs for further validation
            if (!sentMessages.isEmpty()) {
                // resending messages that haven't received acks yet
                logger.info("[{}] Resending {} messages that haven't received ack's yet into new session...", streamId,
                        sentMessages.size());
                messageSender.sendMessages(sentMessages);
            }
            if (initResultFutureRef.get() != null) {
                initResultFutureRef.get().complete(new InitResult(lastSeqNo));
            }
            isInitialized.set(true);
            sendDataRequestIfNeeded();
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
            int inFlightFreed = 0;
            long bytesFreed = 0;
            for (YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack : acks) {
                while (true) {
                    EnqueuedMessage sentMessage = sentMessages.peek();
                    if (sentMessage == null) {
                        break;
                    }
                    if (sentMessage.getSeqNo() == ack.getSeqNo()) {
                        inFlightFreed++;
                        bytesFreed += sentMessage.getSize();
                        sentMessages.remove();
                        processWriteAck(sentMessage, statistics, ack);
                        break;
                    }
                    if (sentMessage.getSeqNo() < ack.getSeqNo()) {
                        // An older message hasn't received an Ack while a newer message has
                        logger.warn("[{}] Received an ack for seqNo {}, but the oldest seqNo waiting for ack is {}",
                                streamId, ack.getSeqNo(), sentMessage.getSeqNo());
                        sentMessage.getFuture().completeExceptionally(
                                new RuntimeException("Didn't get ack from server for this message"));
                        inFlightFreed++;
                        bytesFreed += sentMessage.getSize();
                        sentMessages.remove();
                        // Checking next message waiting for ack
                    } else {
                        logger.warn("[{}] Received an ack with seqNo {} which is older than the oldest message with " +
                                "seqNo {} waiting for ack", streamId, ack.getSeqNo(), sentMessage.getSeqNo());
                        break;
                    }
                }
            }
            free(inFlightFreed, bytesFreed);
        }

        private void processMessage(YdbTopic.StreamWriteMessage.FromServer message) {
            logger.debug("[{}] processMessage called", streamId);
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
            }
        }

        private void processWriteAck(EnqueuedMessage message, WriteAck.Statistics statistics,
                                     YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack) {
            logger.debug("[{}] Received WriteAck with seqNo {} and status {}", streamId, ack.getSeqNo(),
                    ack.getMessageWriteStatusCase());
            WriteAck resultAck;
            switch (ack.getMessageWriteStatusCase()) {
                case WRITTEN:
                    WriteAck.Details details = new WriteAck.Details(ack.getWritten().getOffset());
                    resultAck = new WriteAck(ack.getSeqNo(), WriteAck.State.WRITTEN, details, statistics);
                    break;
                case SKIPPED:
                    switch (ack.getSkipped().getReason()) {
                        case REASON_ALREADY_WRITTEN:
                            resultAck = new WriteAck(ack.getSeqNo(), WriteAck.State.ALREADY_WRITTEN, null, statistics);
                            break;
                        case REASON_UNSPECIFIED:
                        default:
                            message.getFuture().completeExceptionally(
                                    new RuntimeException("Unknown WriteAck skipped reason"));
                            return;
                    }
                    break;
                case WRITTEN_IN_TX:
                    resultAck = new WriteAck(ack.getSeqNo(), WriteAck.State.WRITTEN_IN_TX, null, statistics);
                    break;
                default:
                    message.getFuture().completeExceptionally(
                            new RuntimeException("Unknown WriteAck state"));
                    return;
            }
            message.getFuture().complete(resultAck);
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
