package tech.ydb.topic.write.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import io.grpc.netty.shaded.io.netty.util.Timeout;
import io.grpc.netty.shaded.io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.StatusCodesProtos;
import tech.ydb.core.Status;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.core.utils.Async;
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
public class WriterImpl {
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);

    // TODO: add retry policy
    private static final int MAX_RECONNECT_COUNT = 5;
    private static final int RECONNECT_DELAY_SECONDS = 1;

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
    // Signalled when all messages are sent and WriteAcks are received
    private final PeriodicUpdateTokenTask periodicUpdateTokenTask = new PeriodicUpdateTokenTask();
    private WriteSession session;
    private String previousToken = null;
    private Boolean isSeqNoProvided = null;
    private long seqNo = 0;
    private int currentInFlightCount = 0;
    private long availableSizeBytes;
    private final AtomicBoolean writeRequestInProgress = new AtomicBoolean(false);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectCounter = new AtomicInteger(0);
    private final ScheduledThreadPoolExecutor reconnectExecutor = new ScheduledThreadPoolExecutor(1);
    // Future for flush method
    private CompletableFuture<WriteAck> lastAcceptedMessageFuture;

    public WriterImpl(TopicRpc topicRpc, WriterSettings settings) {
        this.topicRpc = topicRpc;
        this.settings = settings;
        this.session = new WriteSession(topicRpc,
                new WriterImpl.ServerResponseObserver()
        );
        this.availableSizeBytes = settings.getMaxSendBufferMemorySize();
        this.periodicUpdateTokenTask.start();
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

        CompletableFuture.runAsync(() -> encode(message), settings.getCompressionExecutor())
                .thenRun(() -> {
                    boolean haveNewMessagesToSend = false;
                    // Working with encodingMessages under synchronized incomingQueue to prevent deadlocks
                    // while working with free method
                    synchronized (incomingQueue) {
                        // Taking all encoded messages to sending queue
                        while (true) {
                            EnqueuedMessage encodedMessage = encodingMessages.poll();
                            if (encodedMessage != null
                                    && (encodedMessage.isCompressed() || settings.getCodec() == Codec.RAW)) {
                                if (encodedMessage.isCompressed()) {
                                    // message was actually encoded. Need to free some bytes
                                    long bytesFreed = encodedMessage.getUncompressedSizeBytes()
                                            - encodedMessage.getCompressedSizeBytes();
                                    if (bytesFreed > 0) {
                                        free(0, bytesFreed);
                                    }
                                }
                                if (logger.isTraceEnabled()) {
                                    logger.info("Adding message to sending queue");
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
                if (sendingQueue.isEmpty() || !writeRequestInProgress.compareAndSet(false, true)) {
                    logger.debug("Nothing to send or send request is already in progress");
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
            logger.info("Started encoding message");
        }
        if (settings.getCodec() == Codec.RAW) {
            return;
        }
        message.getMessage().setData(Encoder.encode(settings.getCodec(), message.getMessage().getData()));
        message.setCompressedSizeBytes(message.getMessage().getData().length);
        message.setCompressed(true);
    }

    private void updateTokenIfNeeded() {
        String token = topicRpc.getCallOptions().getAuthority();
        if (token == null || token.equals(previousToken)) {
            return;
        }
        previousToken = token;
        session.send(YdbTopic.StreamWriteMessage.FromClient.newBuilder()
                .setUpdateTokenRequest(YdbTopic.UpdateTokenRequest.newBuilder()
                        .setToken(token)
                        .build())
                .build());
    }

    protected CompletableFuture<InitResult> initImpl() {
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
        this.session = new WriteSession(topicRpc,
                new WriterImpl.ServerResponseObserver()
        );
        if (!isReconnecting.compareAndSet(true, false)) {
            logger.warn("Reconnecting flag was not set while reconnecting for some reason");
        }
        initImpl();
    }

    protected CompletableFuture<Void> shutdownImpl() {
        isStopped.set(true);
        periodicUpdateTokenTask.stop();
        reconnectExecutor.shutdown();
        return flushImpl();
    }

    private class PeriodicUpdateTokenTask implements TimerTask {
        private static final long UPDATE_TOKEN_PERIOD_SECONDS = 3600;
        private Timeout currentSchedule = null;

        void stop() {
            logger.info("stopping PeriodicUpdateTokenTask");
            if (currentSchedule != null) {
                currentSchedule.cancel();
                currentSchedule = null;
            }
        }

        void start() {
            logger.info("starting PeriodicUpdateTokenTask");
            // do not check token at the start, just schedule next
            scheduleNextTokenCheck();
        }

        @Override
        public void run(Timeout timeout) {
            if (timeout.isCancelled() || isStopped.get()) {
                return;
            }

            updateTokenIfNeeded();
            scheduleNextTokenCheck();
        }

        private void scheduleNextTokenCheck() {
            currentSchedule = Async.runAfter(this, UPDATE_TOKEN_PERIOD_SECONDS, TimeUnit.SECONDS);
        }
    }

    private class ServerResponseObserver implements StreamObserver<YdbTopic.StreamWriteMessage.FromServer> {
        private boolean working = true;


        @Override
        public void onNext(YdbTopic.StreamWriteMessage.FromServer message) {
            if (logger.isTraceEnabled()) {
                logger.info("ServerResponseObserver - onNext: {}", message);
            }

            if (!working) {
                return;
            }

            if (message.getStatus() != StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
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

        @Override
        public void onError(Status status) {
            // This session is not working anymore
            working = false;
            if (!status.getCode().isRetryable(true, false)) {
                logger.error("Non-retryable error occurred: " + status + ". Shutting down writer.");
                shutdownImpl();
                return;
            } else if (!isStopped.get()) {
                if (isReconnecting.compareAndSet(false, true)) {
                    int currentReconnectCounter = reconnectCounter.incrementAndGet();
                    if (currentReconnectCounter > MAX_RECONNECT_COUNT) {
                        if (isStopped.compareAndSet(false, true)) {
                            logger.error("Maximum retry count ({}}) exceeded. Shutting down writer.",
                                    MAX_RECONNECT_COUNT);
                        } else {
                            logger.debug("Maximum retry count ({}}) exceeded. But writer is already shut down.",
                                    MAX_RECONNECT_COUNT);
                        }
                    } else {
                        logger.warn("Retryable error occurred: " + status + ". Scheduling reconnect...");
                        reconnectExecutor.schedule(WriterImpl.this::reconnect, RECONNECT_DELAY_SECONDS,
                                TimeUnit.SECONDS);
                    }
                } else {
                    logger.debug("Retryable error occurred: " + status + ". Reconnect is already in progress.");
                }
            }
        }

        @Override
        public void onCompleted() {
            logger.info("ServerResponseObserver - onCompleted");
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
}
