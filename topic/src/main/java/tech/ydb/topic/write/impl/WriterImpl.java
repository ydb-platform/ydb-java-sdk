package tech.ydb.topic.write.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.StatusCodesProtos;
import tech.ydb.core.Status;
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

import static tech.ydb.topic.YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.MessageWriteStatusCase.SKIPPED;
import static tech.ydb.topic.YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.MessageWriteStatusCase.WRITTEN;
import static tech.ydb.topic.YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.Skipped.Reason.REASON_ALREADY_WRITTEN;
import static tech.ydb.topic.YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.Skipped.Reason.REASON_UNSPECIFIED;

/**
 * @author Nikolay Perfilov
 */
public abstract class WriterImpl {
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);

    // TODO: add retry policy
    private static final int MAX_RECONNECT_COUNT = 0; // Inf
    private static final int RECONNECT_DELAY_SECONDS = 5;

    private final WriterSettings settings;
    private final TopicRpc topicRpc;
    private CompletableFuture<InitResult> initResultFuture = new CompletableFuture<>();
    // Messages that are waiting for being put into sending queue due to queue overflow
    private final Queue<IncomingMessage> incomingQueue = new LinkedList<>();
    // Messages that are currently encoding
    private final Queue<EnqueuedMessage> encodingMessages = new LinkedList<>();
    // Messages that are taken into sending buffer, are already compressed and are waiting for being sent
    private final Queue<EnqueuedMessage> sendingQueue = new LinkedList<>();
    // Messages that are currently trying to be sent and haven't received a response from server yet
    private final Queue<EnqueuedMessage> sentMessages = new LinkedList<>();

    private WriteSession session;
    private Boolean isSeqNoProvided = null;
    private long seqNo = 0;
    private int currentInFlightCount = 0;
    private long availableSizeBytes;
    private final AtomicBoolean writeRequestInProgress = new AtomicBoolean(false);
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final AtomicInteger reconnectCounter = new AtomicInteger(0);
    private final ScheduledThreadPoolExecutor reconnectExecutor = new ScheduledThreadPoolExecutor(1);
    // Future for flush method
    private CompletableFuture<WriteAck> lastAcceptedMessageFuture;
    private final Executor compressionExecutor;

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
                if (logger.isTraceEnabled()) {
                    logger.trace("Enough space to put the message into the queue right now");
                }
                acceptMessageIntoSendingQueue(message);
                return CompletableFuture.completedFuture(null);
            }

            // Message queue is overflown. Putting it in incoming waiting queue
            logger.debug("Message queue is overflown. Putting it in incoming waiting queue");
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
        this.encodingMessages.add(message);

        CompletableFuture.runAsync(() -> encode(message), compressionExecutor)
                .thenRun(() -> {
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
                                    // message was actually encoded. Need to free some bytes
                                    long bytesFreed = encodedMessage.getUncompressedSizeBytes()
                                            - encodedMessage.getCompressedSizeBytes();
                                    if (bytesFreed > 0) {
                                        free(0, bytesFreed);
                                    }
                                }
                                if (logger.isTraceEnabled()) {
                                    logger.debug("Adding message to sending queue");
                                }
                                synchronized (sendingQueue) {
                                    sendingQueue.add(encodedMessage);
                                }
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
                    long bytesFreed = message.isCompressed()
                            ? message.getCompressedSizeBytes()
                            : message.getUncompressedSizeBytes();
                    free(1, bytesFreed);
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
            synchronized (sendingQueue) {
                if (sendingQueue.isEmpty()) {
                    logger.debug("Nothing to send");
                    return;
                }
                if (!writeRequestInProgress.compareAndSet(false, true)) {
                    logger.debug("Send request is already in progress");
                    return;
                }
                messages = new LinkedList<>();
                messages.addAll(sendingQueue);
                sendingQueue.clear();
            }
            sendMessages(messages);
            if (!writeRequestInProgress.compareAndSet(true, false)) {
                logger.error("Couldn't turn off writeRequestInProgress");
            }
        }
    }

    private void encode(EnqueuedMessage message) {
        if (logger.isTraceEnabled()) {
            logger.trace("Started encoding message");
        }
        if (settings.getCodec() == Codec.RAW) {
            return;
        }
        message.getMessage().setData(Encoder.encode(settings.getCodec(), message.getMessage().getData()));
        message.setCompressedSizeBytes(message.getMessage().getData().length);
        message.setCompressed(true);
        if (logger.isTraceEnabled()) {
            logger.trace("Successfully finished encoding message");
        }
    }

    private void sendToken(String token) {
        if (isStopped.get() || !initResultFuture.isDone()) {
            return;
        }
        session.send(YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setUpdateTokenRequest(YdbTopic.UpdateTokenRequest.newBuilder()
                        .setToken(token)
                        .build())
                .build());
    }

    protected CompletableFuture<InitResult> initImpl() {
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
            sentMessages.add(message);
        }
        session.send(YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setWriteRequest(writeRequestBuilder)
                .build());
    }

    // First future completes when message is put (or declined) into sending buffer
    // Second future completes on receiving write ack from server
    protected CompletableFuture<CompletableFuture<WriteAck>> sendImpl(Message message) {
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

        return tryToEnqueue(enqueuedMessage, false).thenApply(v -> enqueuedMessage.getFuture());
    }

    protected CompletableFuture<Void> flushImpl() {
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
            // Try to add waiting messages into sending buffer
            for (IncomingMessage incomingMessage : incomingQueue) {
                if (incomingMessage.message.getUncompressedSizeBytes() > availableSizeBytes
                        || currentInFlightCount >= settings.getMaxSendBufferMessagesCount()) {
                    return;
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Enough space to put a message into the queue after freeing some space");
                }
                if (incomingMessage.future.complete(null)) {
                    acceptMessageIntoSendingQueue(incomingMessage.message);
                }
            }
        }
    }

    private void reconnect() {
        this.session.finish();
        this.session = new WriteSession(topicRpc);
        initImpl();
    }

    protected CompletableFuture<Void> shutdownImpl() {
        isStopped.set(true);
        reconnectExecutor.shutdown();
        return flushImpl()
                .thenRun(() -> session.finish());
    }

    private void processMessage(YdbTopic.StreamWriteMessage.FromServer message) {
        if (logger.isTraceEnabled()) {
            logger.debug("ServerResponseObserver - onNext: {}", message);
        }

        if (!isWorking.get()) {
            return;
        }

        if (message.getStatus() == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
            reconnectCounter.set(0);
        } else {
            logger.error("Unexpected behaviour: got non-success status in onNext method");
            shutdownImpl();
            return;
        }
        if (message.hasInitResponse()) {
            long lastSeqNo = message.getInitResponse().getLastSeqNo();
            seqNo = lastSeqNo;
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
                while (true) {
                    EnqueuedMessage sentMessage = sentMessages.peek();
                    if (sentMessage == null) {
                        break;
                    }
                    if (sentMessage.getSeqNo() == ack.getSeqNo()) {
                        processWriteAck(sentMessage, ack);
                        inFlightFreed++;
                        bytesFreed += sentMessage.getSizeBytes();
                        // TODO: sync to ensure its the same message
                        sentMessages.poll();
                        break;
                    }
                    if (sentMessage.getSeqNo() < ack.getSeqNo()) {
                        // An older message hasn't received an Ack while a newer message has
                        sentMessage.getFuture().completeExceptionally(
                                new RuntimeException("Didn't get ack from server for this message"));
                        inFlightFreed++;
                        bytesFreed += sentMessage.getSizeBytes();
                        sentMessages.poll();
                        break;
                    }
                    // Received an ack for a message older than the oldest message waiting for Ack. Ignoring
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
        // This session is not working anymore
        boolean stoppedWorking = isWorking.compareAndSet(true, false);
        if (isStopped.get()) {
            return;
        }
        if (!stoppedWorking) {
            logger.debug("Error occurred on session that had already have errors: " + status);
            return;
        }
        int currentReconnectCounter = reconnectCounter.incrementAndGet();
        if (MAX_RECONNECT_COUNT > 0 && currentReconnectCounter > MAX_RECONNECT_COUNT) {
            if (isStopped.compareAndSet(false, true)) {
                logger.error("Maximum retry count ({}}) exceeded. Shutting down writer.", MAX_RECONNECT_COUNT);
            } else {
                logger.debug("Maximum retry count ({}}) exceeded. But writer is already shut down.",
                        MAX_RECONNECT_COUNT);
            }
        } else {
            logger.warn("Error occurred: " + status + ". Retry #" + currentReconnectCounter
                    + ". Scheduling reconnect...");
            reconnectExecutor.schedule(WriterImpl.this::reconnect, RECONNECT_DELAY_SECONDS,
                    TimeUnit.SECONDS);
        }

    }
}
