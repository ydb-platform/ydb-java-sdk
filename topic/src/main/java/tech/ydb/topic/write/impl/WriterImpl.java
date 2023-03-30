package tech.ydb.topic.write.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.StatusCodesProtos;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.Encoder;
import tech.ydb.topic.utils.ProtoUtils;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public abstract class WriterImpl {
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);

    // TODO: add retry policy
    private static final int MAX_RECONNECT_COUNT = 0; // Inf
    private static final int EXP_BACKOFF_BASE_MS = 256;
    private static final int EXP_BACKOFF_CEILING_MS = 40000; // 40 sec (max delays would be 40-80 sec)
    private static final int EXP_BACKOFF_MAX_POWER = 7;

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
    private final Queue<EnqueuedMessage> sentMessages = new LinkedList<>();
    private final AtomicBoolean writeRequestInProgress = new AtomicBoolean(false);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final AtomicInteger reconnectCounter = new AtomicInteger(0);
    private final Executor compressionExecutor;

    private WriteSession session;
    private String currentSessionId;
    private Boolean isSeqNoProvided = null;
    private long seqNo = 0;
    private int currentInFlightCount = 0;
    private long availableSizeBytes;
    // Future for flush method
    private CompletableFuture<WriteAck> lastAcceptedMessageFuture;

    public WriterImpl(TopicRpc topicRpc, WriterSettings settings, Executor compressionExecutor) {
        this.topicRpc = topicRpc;
        this.settings = settings;
        this.session = new WriteSession(topicRpc);
        this.availableSizeBytes = settings.getMaxSendBufferMemorySize();
        this.compressionExecutor = compressionExecutor;
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
                    CompletableFuture<Void> result = new CompletableFuture<>();
                    result.completeExceptionally(new QueueOverflowException("Message queue in-flight limit reached"));
                    return result;
                }
            } else if (availableSizeBytes < message.getMessage().getData().length) {
                if (instant) {
                    CompletableFuture<Void> result = new CompletableFuture<>();
                    result.completeExceptionally(new QueueOverflowException("Message queue size limit reached"));
                    return result;
                }
            } else if (incomingQueue.isEmpty()) {
                // Enough space to put the message into the queue right now
                logger.trace("Putting a message into the queue right now, enough space in send buffer");
                acceptMessageIntoSendingQueue(message);
                return CompletableFuture.completedFuture(null);
            }

            logger.debug("Message queue send buffer is overflown. Putting the message into incoming waiting queue");
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
        if (logger.isDebugEnabled()) {
            logger.debug("Accepted 1 message of {} uncompressed bytes. Current In-flight: {}, AvailableSizeBytes: {}",
                    message.getUncompressedSizeBytes(), currentInFlightCount, availableSizeBytes);
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
                                        logger.trace("Message compressed from {} to {} bytes",
                                                encodedMessage.getUncompressedSizeBytes(),
                                                encodedMessage.getCompressedSizeBytes());
                                    }
                                    // message was actually encoded. Need to free some bytes
                                    long bytesFreed = encodedMessage.getUncompressedSizeBytes()
                                            - encodedMessage.getCompressedSizeBytes();
                                    // bytesFreed can be less than 0
                                    free(0, bytesFreed);

                                }
                                logger.debug("Adding message to sending queue");
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
                    logger.error("Exception while encoding message: ", throwable);
                    free(1, message.getSizeBytes());
                    message.getFuture().completeExceptionally(throwable);
                    return null;
                });
    }

    private void sendDataRequestIfNeeded() {
        while (true) {
            if (!initResultFuture.isDone()) {
                logger.debug("Can't send data -- init was not yet received");
                return;
            }
            Queue<EnqueuedMessage> messages;
            if (sendingQueue.isEmpty()) {
                logger.trace("Nothing to send -- sendingQueue is empty");
                return;
            }
            if (!writeRequestInProgress.compareAndSet(false, true)) {
                logger.debug("Send request is already in progress");
                return;
            }
            // This code can be run in one thread at a time due to acquiring writeRequestInProgress
            messages = new LinkedList<>(sendingQueue);
            // Checking second time under writeRequestInProgress "lock"
            if (messages.isEmpty()) {
                logger.debug("Nothing to send -- sendingQueue is empty #2");
            } else {
                sendingQueue.removeAll(messages);
                sentMessages.addAll(messages);
                sendMessages(messages);
            }
            if (!writeRequestInProgress.compareAndSet(true, false)) {
                logger.error("Couldn't turn off writeRequestInProgress. Should not happen");
            }
        }
    }

    private void encode(EnqueuedMessage message) {
        logger.trace("Started encoding message");
        if (settings.getCodec() == Codec.RAW) {
            return;
        }
        message.getMessage().setData(Encoder.encode(settings.getCodec(), message.getMessage().getData()));
        message.setCompressedSizeBytes(message.getMessage().getData().length);
        message.setCompressed(true);
        logger.trace("Successfully finished encoding message");
    }

    protected CompletableFuture<InitResult> initImpl() {
        logger.debug("initImpl started");
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

    private void sendMessages(Queue<EnqueuedMessage> messages) {
        logger.debug("Sending messages...");
        YdbTopic.StreamWriteMessage.WriteRequest.Builder writeRequestBuilder = YdbTopic.StreamWriteMessage.WriteRequest
                .newBuilder()
                .setCodec(ProtoUtils.toProto(settings.getCodec()));

        for (EnqueuedMessage message : messages) {
            long messageSeqNo = message.getMessage().getSeqNo() == null
                    ? ++seqNo
                    : message.getMessage().getSeqNo();
            message.setSeqNo(messageSeqNo);
            writeRequestBuilder.addMessages(YdbTopic.StreamWriteMessage.WriteRequest.MessageData.newBuilder()
                    .setSeqNo(messageSeqNo)
                    .setData(ByteString.copyFrom(message.getMessage().getData()))
                    .build());
        }
        session.send(YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setWriteRequest(writeRequestBuilder)
                .build());
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
                        "SeqNo was provided for a message after it had bot been provided for a another message. " +
                                "SeqNo should either be provided for all messages or none of them.");
            }
            if (message.getSeqNo() == null && isSeqNoProvided) {
                throw new RuntimeException(
                        "SeqNo was not provided for a message after it had been provided for a another message. " +
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
            if (logger.isDebugEnabled()) {
                logger.debug("Freed {} bytes in {} messages. Current In-flight: {}, current availableSize: {}",
                        sizeBytes, messageCount, currentInFlightCount, availableSizeBytes);
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
                        logger.trace("There are messages in incomingQueue still, but no space in send buffer");
                        return;
                    }
                    logger.trace("Putting a message into send buffer after freeing some space");
                    if (incomingMessage.future.complete(null)) {
                        acceptMessageIntoSendingQueue(incomingMessage.message);
                    }
                    incomingQueue.remove();
                }
                logger.trace("All messages from incomingQueue are accepted into send buffer");
            }
        }
    }

    private void reconnect() {
        this.session = new WriteSession(topicRpc);
        initImpl();
    }

    protected CompletableFuture<Void> shutdownImpl() {
        isStopped.set(true);
        return flushImpl()
                .thenRun(() -> session.finish());
    }

    private void shutdownImpl(String reason) {
        if (!initResultFuture.isDone()) {
            initImpl().completeExceptionally(new RuntimeException(reason));
        }
        shutdownImpl();
    }

    private void processMessage(YdbTopic.StreamWriteMessage.FromServer message) {
        logger.trace("processMessage called");
        if (message.getStatus() == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
            reconnectCounter.set(0);
        } else {
            logger.error("Got non-success status in processMessage method: {}", message);
            completeSession(Status.of(StatusCode.fromProto(message.getStatus()))
                            .withIssues(Issue.of("Got a message with non-success status: " + message,
                                    Issue.Severity.ERROR)), null);
        }
        if (message.hasInitResponse()) {
            currentSessionId = message.getInitResponse().getSessionId();
            logger.info("Session {} initialized", currentSessionId);
            long lastSeqNo = message.getInitResponse().getLastSeqNo();
            seqNo = lastSeqNo;
            // TODO: remember supported codecs for further validation
            if (!sentMessages.isEmpty()) {
                // resending messages that haven't received acks yet
                sendMessages(sentMessages);
            }
            initResultFuture.complete(new InitResult(lastSeqNo));
            sendDataRequestIfNeeded();
        } else if (message.hasWriteResponse()) {
            List<YdbTopic.StreamWriteMessage.WriteResponse.WriteAck> acks =
                    message.getWriteResponse().getAcksList();
            int inFlightFreed = 0;
            long bytesFreed = 0;
            for (YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack : acks) {
                synchronized (sentMessages) {
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
                            logger.warn("Received an ack for seqNo {}, but the oldest seqNo waiting for ack is {}",
                                    ack.getSeqNo(), sentMessage.getSeqNo());
                            sentMessage.getFuture().completeExceptionally(
                                    new RuntimeException("Didn't get ack from server for this message"));
                            inFlightFreed++;
                            bytesFreed += sentMessage.getSizeBytes();
                            sentMessages.remove();
                            // Checking next message waiting for ack
                        } else {
                            logger.info("Received an ack with seqNo {} which is older than the oldest message with " +
                                            "seqNo {} waiting for ack",
                                    ack.getSeqNo(), sentMessage.getSeqNo());
                            break;
                        }
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

    private void completeSession(Status status, Throwable th) {
        logger.info("CompleteSession called");
        // This session is not working anymore
        this.session.finish();

        if (th != null) {
            logger.error("Exception in writing stream session {}: {}", currentSessionId, th);
        } else {
            if (status.isSuccess()) {
                if (isStopped.get()) {
                    logger.info("Writing stream session {} closed successfully", currentSessionId);
                } else {
                    logger.error("Writing stream session {} was closed unexpectedly. Shutting down the whole writer.",
                            currentSessionId);
                    shutdownImpl("Writing stream session " + currentSessionId
                            + " was closed unexpectedly. Shutting down writer.");
                }
                return;
            }
            logger.error("Error in writing stream session {}: {}", (currentSessionId != null ? currentSessionId : ""),
                    status);
        }

        if (isStopped.get()) {
            return;
        }
        int currentReconnectCounter = reconnectCounter.incrementAndGet();
        if (MAX_RECONNECT_COUNT > 0 && currentReconnectCounter > MAX_RECONNECT_COUNT) {
            if (isStopped.compareAndSet(false, true)) {
                logger.error("Maximum retry count ({}}) exceeded. Shutting down writer.", MAX_RECONNECT_COUNT);
                shutdownImpl("Maximum retry count (" + MAX_RECONNECT_COUNT
                        + ") exceeded. Shutting down writer with error: " + (th != null ? th : status));
            } else {
                logger.debug("Maximum retry count ({}}) exceeded. But writer is already shut down.",
                        MAX_RECONNECT_COUNT);
            }
        } else {
            logger.warn("Retry #" + currentReconnectCounter + ". Scheduling reconnect...");
            int delayMs = currentReconnectCounter <= EXP_BACKOFF_MAX_POWER
                    ? EXP_BACKOFF_BASE_MS * (1 << currentReconnectCounter)
                    : EXP_BACKOFF_CEILING_MS;
            // Add jitter
            delayMs = delayMs + ThreadLocalRandom.current().nextInt(delayMs);
            topicRpc.getScheduler().schedule(this::reconnect, delayMs, TimeUnit.MILLISECONDS);
        }
    }
}
