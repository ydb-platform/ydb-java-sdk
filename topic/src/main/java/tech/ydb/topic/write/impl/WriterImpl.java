package tech.ydb.topic.write.impl;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.GrpcStreamRetrier;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.Encoder;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public abstract class WriterImpl extends GrpcStreamRetrier {
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);

    private WriteSessionImpl session;
    private final WriterSettings settings;
    private final TopicRpc topicRpc;
    private final AtomicReference<CompletableFuture<InitResult>> initResultFutureRef = new AtomicReference<>(null);
    // Messages that are waiting for being put into sending queue due to queue overflow
    private final Queue<IncomingMessage> incomingQueue = new LinkedList<>();
    // Messages that are currently encoding
    private final Queue<EnqueuedMessage> encodingMessages = new LinkedList<>();
    // Messages that are taken into send buffer, are already compressed and are waiting for being sent
    private final Queue<EnqueuedMessage> sendingQueue = new ConcurrentLinkedQueue<>();
    // Messages that are currently trying to be sent and haven't received a response from server yet
    private final Deque<EnqueuedMessage> sentMessages = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean writeRequestInProgress = new AtomicBoolean();
    private final Executor compressionExecutor;
    private final long maxSendBufferMemorySize;

    // Every writing stream has a sequential number (for debug purposes)
    private final AtomicLong sessionSeqNumberCounter = new AtomicLong(0);

    private Boolean isSeqNoProvided = null;
    private int currentInFlightCount = 0;
    private long availableSizeBytes;
    // Future for flush method
    private CompletableFuture<WriteAck> lastAcceptedMessageFuture;

    public WriterImpl(TopicRpc topicRpc, WriterSettings settings, Executor compressionExecutor) {
        super(topicRpc.getScheduler());
        this.topicRpc = topicRpc;
        this.settings = settings;
        this.session = new WriteSessionImpl();
        this.availableSizeBytes = settings.getMaxSendBufferMemorySize();
        this.maxSendBufferMemorySize = settings.getMaxSendBufferMemorySize();
        this.compressionExecutor = compressionExecutor;
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
        synchronized (incomingQueue) {
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
            } else if (availableSizeBytes <= message.getMessage().getData().length) {
                if (instant) {
                    logger.info("[{}] Rejecting a message due to reaching message queue size limit of {} bytes", id,
                            settings.getMaxSendBufferMemorySize());
                    CompletableFuture<Void> result = new CompletableFuture<>();
                    result.completeExceptionally(new QueueOverflowException("Message queue size limit of "
                            + settings.getMaxSendBufferMemorySize() + " bytes reached"));
                    return result;
                } else {
                    logger.info("[{}] Message queue size limit of {} bytes reached. Putting the message into incoming" +
                            " waiting queue", id, settings.getMaxSendBufferMemorySize());
                }
            } else if (incomingQueue.isEmpty()) {
                acceptMessageIntoSendingQueue(message);
                return CompletableFuture.completedFuture(null);
            }

            IncomingMessage incomingMessage = new IncomingMessage(message);
            incomingQueue.add(incomingMessage);
            return incomingMessage.future;
        }
    }

    // should be done under synchronized incomingQueue
    private void acceptMessageIntoSendingQueue(EnqueuedMessage message) {
        this.lastAcceptedMessageFuture = message.getFuture();
        this.currentInFlightCount++;
        this.availableSizeBytes -= message.getUncompressedSizeBytes();
        if (logger.isTraceEnabled()) {
            logger.trace("[{}] Accepted 1 message of {} uncompressed bytes. Current In-flight: {}, " +
                            "AvailableSizeBytes: {} ({} / {} acquired)", id, message.getUncompressedSizeBytes(),
                    currentInFlightCount, availableSizeBytes, maxSendBufferMemorySize - availableSizeBytes,
                    maxSendBufferMemorySize);
        }
        this.encodingMessages.add(message);

        CompletableFuture
                .runAsync(() -> encode(message), compressionExecutor)
                .thenRunAsync(this::moveEncodedMessagesToSendingQueue)
                .exceptionally((throwable) -> {
                    logger.error("[{}] Exception while encoding message: ", id, throwable);
                    free(1, message.getSizeBytes());
                    message.getFuture().completeExceptionally(throwable);
                    message.setProcessingFailed(true);
                    moveEncodedMessagesToSendingQueue();
                    return null;
                });
    }

    private void encode(EnqueuedMessage message) {
        logger.trace("[{}] Started encoding message", id);
        if (settings.getCodec() == Codec.RAW) {
            return;
        }
        message.getMessage().setData(Encoder.encode(settings.getCodec(), message.getMessage().getData()));
        message.setCompressedSizeBytes(message.getMessage().getData().length);
        message.setCompressed(true);
        logger.trace("[{}] Successfully finished encoding message", id);
    }

    private void moveEncodedMessagesToSendingQueue() {
        boolean haveNewMessagesToSend = false;
        // Working with encodingMessages under synchronized incomingQueue to prevent deadlocks
        // while working with free method
        synchronized (incomingQueue) {
            // Taking all encoded messages to sending queue
            while (true) {
                EnqueuedMessage encodedMessage = encodingMessages.peek();
                if (encodedMessage == null) {
                    break;
                }
                if (encodedMessage.isProcessingFailed()) {
                    encodingMessages.remove();
                } else if (encodedMessage.isCompressed() || settings.getCodec() == Codec.RAW) {
                    encodingMessages.remove();
                    if (encodedMessage.isCompressed()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("[{}] Message compressed from {} to {} bytes", id,
                                    encodedMessage.getUncompressedSizeBytes(),
                                    encodedMessage.getCompressedSizeBytes());
                        }
                        // message was actually encoded. Need to free some bytes
                        long bytesFreed = encodedMessage.getUncompressedSizeBytes()
                                - encodedMessage.getCompressedSizeBytes();
                        // bytesFreed can be less than 0
                        free(0, bytesFreed);
                    }
                    logger.debug("[{}] Adding message to sending queue", id);
                    sendingQueue.add(encodedMessage);
                    haveNewMessagesToSend = true;
                } else {
                    break;
                }
            }
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
    protected CompletableFuture<CompletableFuture<WriteAck>> sendImpl(Message message, boolean instant) {
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

        EnqueuedMessage enqueuedMessage = new EnqueuedMessage(message);

        return tryToEnqueue(enqueuedMessage, instant).thenApply(v -> enqueuedMessage.getFuture());
    }

    protected CompletableFuture<Void> flushImpl() {
        if (this.lastAcceptedMessageFuture == null) {
            return CompletableFuture.completedFuture(null);
        }
        synchronized (incomingQueue) {
            return this.lastAcceptedMessageFuture.isDone()
                    ? CompletableFuture.completedFuture(null)
                    : this.lastAcceptedMessageFuture.thenApply(v -> null);
        }
    }

    private void free(int messageCount, long sizeBytes) {
        synchronized (incomingQueue) {
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
                    if (incomingMessage.message.getUncompressedSizeBytes() > availableSizeBytes
                            || currentInFlightCount >= settings.getMaxSendBufferMessagesCount()) {
                        logger.trace("[{}] There are messages in incomingQueue still, but no space in send buffer", id);
                        return;
                    }
                    logger.trace("[{}] Putting a message into send buffer after freeing some space", id);
                    if (incomingMessage.future.complete(null)) {
                        acceptMessageIntoSendingQueue(incomingMessage.message);
                    }
                    incomingQueue.remove();
                }
                logger.trace("[{}] All messages from incomingQueue are accepted into send buffer", id);
            }
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
        protected String sessionId = "";
        private final String fullId;
        private final MessageSender messageSender;
        private final AtomicBoolean isInitialized = new AtomicBoolean(false);

        private WriteSessionImpl() {
            super(topicRpc);
            this.fullId = id + '.' + sessionSeqNumberCounter.incrementAndGet();
            this.messageSender = new MessageSender(settings);
        }

        public void startAndInitialize() {
            logger.debug("[{}] Session {} startAndInitialize called", fullId, sessionId);
            start(this::processMessage).whenComplete(this::onSessionClosing);

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
                    logger.debug("[{}] Can't send data: current session is not yet initialized",
                            fullId);
                    return;
                }
                if (!isWorking.get()) {
                    logger.debug("[{}] Can't send data: current session has been already stopped",
                            fullId);
                    return;
                }
                Queue<EnqueuedMessage> messages;
                if (sendingQueue.isEmpty()) {
                    logger.trace("[{}] Nothing to send -- sendingQueue is empty", fullId);
                    return;
                }
                if (!writeRequestInProgress.compareAndSet(false, true)) {
                    logger.debug("[{}] Send request is already in progress", fullId);
                    return;
                }
                // This code can be run in one thread at a time due to acquiring writeRequestInProgress
                messages = new LinkedList<>(sendingQueue);
                // Checking second time under writeRequestInProgress "lock"
                if (messages.isEmpty()) {
                    logger.debug("[{}] Nothing to send -- sendingQueue is empty #2", fullId);
                } else {
                    sendingQueue.removeAll(messages);
                    sentMessages.addAll(messages);
                    messageSender.sendMessages(messages);
                    logger.debug("[{}] Sent {} messages to server", fullId, messages.size());
                }
                if (!writeRequestInProgress.compareAndSet(true, false)) {
                    logger.error("[{}] Couldn't turn off writeRequestInProgress. Should not happen", fullId);
                }
            }
        }

        private void onInitResponse(YdbTopic.StreamWriteMessage.InitResponse response) {
            sessionId = response.getSessionId();
            logger.info("[{}] Session {} initialized", fullId, sessionId);
            long lastSeqNo = response.getLastSeqNo();
            long actualLastSeqNo = lastSeqNo;
            // If there are messages that were already sent before reconnect but haven't received acks,
            // their highest seqNo should also be taken in consideration when calculating next seqNo automatically
            if (!sentMessages.isEmpty()) {
                actualLastSeqNo = Math.max(lastSeqNo, sentMessages.getLast().getSeqNo());
            }
            messageSender.setSession(this);
            messageSender.setSeqNo(actualLastSeqNo);
            // TODO: remember supported codecs for further validation
            if (!sentMessages.isEmpty()) {
                // resending messages that haven't received acks yet
                logger.info("Resending {} messages that haven't received ack's yet into new session...",
                        sentMessages.size());
                messageSender.sendMessages(sentMessages);
            }
            if (initResultFutureRef.get() != null) {
                initResultFutureRef.get().complete(new InitResult(lastSeqNo));
            }
            isInitialized.set(true);
            sendDataRequestIfNeeded();
        }

        private void onWriteResponse(YdbTopic.StreamWriteMessage.WriteResponse response) {
            List<YdbTopic.StreamWriteMessage.WriteResponse.WriteAck> acks = response.getAcksList();
            int inFlightFreed = 0;
            long bytesFreed = 0;
            for (YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack : acks) {
                while (true) {
                    EnqueuedMessage sentMessage = sentMessages.peek();
                    if (sentMessage == null) {
                        break;
                    }
                    if (sentMessage.getSeqNo() == ack.getSeqNo()) {
                        processWriteAck(sentMessage, ack);
                        inFlightFreed++;
                        bytesFreed += sentMessage.getSizeBytes();
                        sentMessages.remove();
                        break;
                    }
                    if (sentMessage.getSeqNo() < ack.getSeqNo()) {
                        // An older message hasn't received an Ack while a newer message has
                        logger.warn("[{}] Received an ack for seqNo {}, but the oldest seqNo waiting for ack is {}",
                                fullId, ack.getSeqNo(), sentMessage.getSeqNo());
                        sentMessage.getFuture().completeExceptionally(
                                new RuntimeException("Didn't get ack from server for this message"));
                        inFlightFreed++;
                        bytesFreed += sentMessage.getSizeBytes();
                        sentMessages.remove();
                        // Checking next message waiting for ack
                    } else {
                        logger.info("[{}] Received an ack with seqNo {} which is older than the oldest message with " +
                                "seqNo {} waiting for ack", fullId, ack.getSeqNo(), sentMessage.getSeqNo());
                        break;
                    }
                }
            }
            free(inFlightFreed, bytesFreed);
        }

        private void processMessage(YdbTopic.StreamWriteMessage.FromServer message) {
            logger.debug("[{}] processMessage called", fullId);
            if (message.getStatus() == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
                reconnectCounter.set(0);
            } else {
                logger.warn("[{}] Got non-success status in processMessage method: {}", fullId, message);
                onSessionClosed(Status.of(StatusCode.fromProto(message.getStatus()))
                        .withIssues(Issue.of("Got a message with non-success status: " + message,
                                Issue.Severity.ERROR)), null);
                return;
            }
            if (message.hasInitResponse()) {
                onInitResponse(message.getInitResponse());
            } else if (message.hasWriteResponse()) {
                onWriteResponse(message.getWriteResponse());
            }
        }

        private void processWriteAck(EnqueuedMessage message,
                                     YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack) {
            WriteAck resultAck;
            switch (ack.getMessageWriteStatusCase()) {
                case WRITTEN:
                    WriteAck.Details details = new WriteAck.Details(ack.getWritten().getOffset());
                    resultAck = new WriteAck(ack.getSeqNo(), WriteAck.State.WRITTEN, details);
                    break;
                case SKIPPED:
                    switch (ack.getSkipped().getReason()) {
                        case REASON_ALREADY_WRITTEN:
                            resultAck = new WriteAck(ack.getSeqNo(), WriteAck.State.ALREADY_WRITTEN, null);
                            break;
                        case REASON_UNSPECIFIED:
                        default:
                            message.getFuture().completeExceptionally(
                                    new RuntimeException("Unknown WriteAck skipped reason"));
                            return;
                    }
                    break;

                default:
                    message.getFuture().completeExceptionally(
                            new RuntimeException("Unknown WriteAck state"));
                    return;
            }
            message.getFuture().complete(resultAck);
        }

        private void onSessionClosing(Status status, Throwable th) {
            logger.info("[{}] Session {} onSessionClosing called", fullId, sessionId);
            if (isWorking.get()) {
                shutdown();
                // Signal writer to retry
                onSessionClosed(status, th);
            }
        }

        @Override
        protected void onStop() {
            logger.debug("[{}] Session {} onStop called", fullId, sessionId);
        }
    }
}
