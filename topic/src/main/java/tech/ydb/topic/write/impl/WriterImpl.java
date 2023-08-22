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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.ReaderWriterBaseImpl;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.Encoder;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public abstract class WriterImpl extends ReaderWriterBaseImpl<WriteSession> {
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);
    private final WriterSettings settings;
    private final TopicRpc topicRpc;
    private CompletableFuture<InitResult> initResultFuture = new CompletableFuture<>();
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
    private final MessageSender messageSender;
    private final long maxSendBufferMemorySize;

    private Boolean isSeqNoProvided = null;
    private int currentInFlightCount = 0;
    private long availableSizeBytes;
    // Future for flush method
    private CompletableFuture<WriteAck> lastAcceptedMessageFuture;

    public WriterImpl(TopicRpc topicRpc, WriterSettings settings, Executor compressionExecutor) {
        super(topicRpc.getScheduler());
        this.topicRpc = topicRpc;
        this.settings = settings;
        this.session = new WriteSession(topicRpc);
        this.availableSizeBytes = settings.getMaxSendBufferMemorySize();
        this.maxSendBufferMemorySize = settings.getMaxSendBufferMemorySize();
        this.compressionExecutor = compressionExecutor;
        this.messageSender = new MessageSender(session, settings);
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
    protected String getSessionType() {
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
                    logger.trace("[{}] Rejecting a message due to reaching message queue in-flight limit", id);
                    CompletableFuture<Void> result = new CompletableFuture<>();
                    result.completeExceptionally(new QueueOverflowException("Message queue in-flight limit reached"));
                    return result;
                } else {
                    logger.debug("[{}] Message queue in-flight limit reached. Putting the message into incoming " +
                            "waiting queue", id);
                }
            } else if (availableSizeBytes <= message.getMessage().getData().length) {
                if (instant) {
                    logger.trace("[{}] Rejecting a message due to reaching message queue size limit", id);
                    CompletableFuture<Void> result = new CompletableFuture<>();
                    result.completeExceptionally(new QueueOverflowException("Message queue size limit reached"));
                    return result;
                } else {
                    logger.debug("[{}] Message queue size limit reached. Putting the message into incoming waiting " +
                                    "queue", id);
                }
            } else if (incomingQueue.isEmpty()) {
                logger.trace("[{}] Putting a message into the queue right now, enough space in send buffer", id);
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

        CompletableFuture.runAsync(() -> encode(message), compressionExecutor)
                .thenRunAsync(() -> {
                    boolean haveNewMessagesToSend = false;
                    // Working with encodingMessages under synchronized incomingQueue to prevent deadlocks
                    // while working with free method
                    synchronized (incomingQueue) {
                        // Taking all encoded messages to sending queue
                        while (true) {
                            EnqueuedMessage encodedMessage = encodingMessages.peek();
                            if (encodedMessage != null
                                    && (encodedMessage.isCompressed() || settings.getCodec() == Codec.RAW)) {
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
                        sendDataRequestIfNeeded();
                    }
                })
                .exceptionally((throwable) -> {
                    logger.error("[{}] Exception while encoding message: ", id, throwable);
                    free(1, message.getSizeBytes());
                    message.getFuture().completeExceptionally(throwable);
                    return null;
                });
    }

    private void sendDataRequestIfNeeded() {
        while (true) {
            if (isReconnecting.get()) {
                logger.debug("[{}] Can't send data: reconnect is in progress", id);
                return;
            }
            if (!initResultFuture.isDone()) {
                logger.debug("[{}] Can't send data: init was not yet received", id);
                return;
            }
            Queue<EnqueuedMessage> messages;
            if (sendingQueue.isEmpty()) {
                logger.trace("[{}] Nothing to send -- sendingQueue is empty", id);
                return;
            }
            if (!writeRequestInProgress.compareAndSet(false, true)) {
                logger.debug("[{}] Send request is already in progress", id);
                return;
            }
            // This code can be run in one thread at a time due to acquiring writeRequestInProgress
            messages = new LinkedList<>(sendingQueue);
            // Checking second time under writeRequestInProgress "lock"
            if (messages.isEmpty()) {
                logger.debug("[{}] Nothing to send -- sendingQueue is empty #2", id);
            } else {
                sendingQueue.removeAll(messages);
                synchronized (messageSender) {
                    sentMessages.addAll(messages);
                    messageSender.sendMessages(messages);
                }
                logger.trace("[{}] Sent {} messages to server", id, messages.size());
            }
            if (!writeRequestInProgress.compareAndSet(true, false)) {
                logger.error("[{}] Couldn't turn off writeRequestInProgress. Should not happen", id);
            }
        }
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

    protected CompletableFuture<InitResult> initImpl() {
        logger.debug("[{}] initImpl started", id);
        session.start(this::processMessage).whenComplete(this::completeSession);

        initResultFuture = new CompletableFuture<>();
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
                throw new RuntimeException("Both MessageGroupId and PartitionId are set in WriterSettings");
            }
            initRequestBuilder.setMessageGroupId(messageGroupId);
        } else if (partitionId != null) {
            initRequestBuilder.setPartitionId(partitionId);
        }
        session.send(YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setInitRequest(initRequestBuilder)
                .build());
        return initResultFuture;
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
    protected void reconnect() {
        logger.info("[{}] Reconnect #{} started. Creating new WriteSession", id, reconnectCounter.get());
        this.session = new WriteSession(topicRpc);
        synchronized (messageSender) {
            messageSender.setSession(session);
        }
        initImpl();
    }

    @Override
    protected void onShutdown(String reason) {
        if (!initResultFuture.isDone()) {
            initImpl().completeExceptionally(new RuntimeException(reason));
        }
    }

    @Override
    protected void onSessionStop() {
        logger.info("[{}] Write session is stopped", id);
    }

    private void processMessage(YdbTopic.StreamWriteMessage.FromServer message) {
        logger.debug("[{}] processMessage called", id);
        if (message.getStatus() == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
            reconnectCounter.set(0);
        } else {
            logger.warn("[{}] Got non-success status in processMessage method: {}", id, message);
            completeSession(Status.of(StatusCode.fromProto(message.getStatus()))
                            .withIssues(Issue.of("Got a message with non-success status: " + message,
                                    Issue.Severity.ERROR)), null);
            return;
        }
        if (message.hasInitResponse()) {
            currentSessionId = message.getInitResponse().getSessionId();
            logger.info("[{}] Session {} initialized", id, currentSessionId);
            if (!isReconnecting.compareAndSet(true, false)) {
                logger.warn("[{}] Couldn't reset reconnect flag. Shouldn't happen", id);
            }
            long lastSeqNo = message.getInitResponse().getLastSeqNo();
            synchronized (messageSender) {
                long realLastSeqNo = lastSeqNo;
                // If there are messages that were already sent before reconnect but haven't received acks,
                // their highest seqNo should also be taken in consideration when calculating next seqNo automatically
                if (!sentMessages.isEmpty()) {
                    realLastSeqNo = Math.max(lastSeqNo, sentMessages.getLast().getSeqNo());
                }
                messageSender.setSeqNo(realLastSeqNo);
                // TODO: remember supported codecs for further validation
                if (!sentMessages.isEmpty()) {
                    // resending messages that haven't received acks yet
                    messageSender.sendMessages(sentMessages);
                }
            }
            initResultFuture.complete(new InitResult(lastSeqNo));
            sendDataRequestIfNeeded();
        } else if (message.hasWriteResponse()) {
            List<YdbTopic.StreamWriteMessage.WriteResponse.WriteAck> acks =
                    message.getWriteResponse().getAcksList();
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
                        logger.warn("[{}] Received an ack for seqNo {}, but the oldest seqNo waiting for ack is {}", id,
                                ack.getSeqNo(), sentMessage.getSeqNo());
                        sentMessage.getFuture().completeExceptionally(
                                new RuntimeException("Didn't get ack from server for this message"));
                        inFlightFreed++;
                        bytesFreed += sentMessage.getSizeBytes();
                        sentMessages.remove();
                        // Checking next message waiting for ack
                    } else {
                        logger.info("[{}] Received an ack with seqNo {} which is older than the oldest message with " +
                                        "seqNo {} waiting for ack", id, ack.getSeqNo(), sentMessage.getSeqNo());
                        break;
                    }
                }
            }
            free(inFlightFreed, bytesFreed);
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
}
