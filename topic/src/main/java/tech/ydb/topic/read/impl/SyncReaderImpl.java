package tech.ydb.topic.read.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.ReceiveSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * @author Nikolay Perfilov
 */
public class SyncReaderImpl extends ReaderImpl implements SyncReader {
    private static final Logger logger = LoggerFactory.getLogger(SyncReaderImpl.class);
    private static final int POLL_INTERVAL_SECONDS = 5;
    private final Queue<MessageBatchWrapper> batchesInQueue = new LinkedList<>();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition queueIsNotEmptyCondition = queueLock.newCondition();
    private int currentMessageIndex = 0;

    public SyncReaderImpl(TopicRpc topicRpc, ReaderSettings settings, CodecRegistry codecRegistry) {
        super(topicRpc, settings, codecRegistry);
    }

    private static class MessageBatchWrapper {
        private final List<Message> messages;
        private final CompletableFuture<Void> future;

        private MessageBatchWrapper(List<Message> messages, CompletableFuture<Void> future) {
            this.messages = messages;
            this.future = future;
        }
    }

    @Override
    public void init() {
        initImpl();
    }

    @Override
    public void initAndWait() {
        initImpl().join();
    }

    @Nullable
    public Message receiveInternal(ReceiveSettings receiveSettings, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (isStopped.get()) {
            throw new RuntimeException("Reader was stopped");
        }

        queueLock.lock();

        try {
            if (batchesInQueue.isEmpty()) {
                long millisToWait = TimeUnit.MILLISECONDS.convert(timeout, unit);
                Instant deadline = Instant.now().plusMillis(millisToWait);
                while (batchesInQueue.isEmpty()) {
                    millisToWait = Duration.between(Instant.now(), deadline).toMillis();
                    if (millisToWait <= 0) {
                        break;
                    }

                    logger.trace("No messages in queue. Waiting for {} ms...", millisToWait);
                    queueIsNotEmptyCondition.await(millisToWait, TimeUnit.MILLISECONDS);
                }

                if (batchesInQueue.isEmpty()) {
                    logger.trace("Still no messages in queue. Returning null");
                    return null;
                }
            }

            logger.trace("Taking a message with index {} from batch", currentMessageIndex);
            MessageBatchWrapper currentBatch = batchesInQueue.element();
            Message result = currentBatch.messages.get(currentMessageIndex);
            currentMessageIndex++;
            if (currentMessageIndex >= currentBatch.messages.size()) {
                logger.debug("Batch is read. signalling core reader impl");
                batchesInQueue.remove();
                currentMessageIndex = 0;
                currentBatch.future.complete(null);
            }
            if (receiveSettings.getTransaction() != null) {
                Status updateStatus = sendUpdateOffsetsInTransaction(receiveSettings.getTransaction(),
                        Collections.singletonMap(result.getPartitionSession().getPath(),
                                Collections.singletonList(result.getPartitionOffsets())),
                        UpdateOffsetsInTransactionSettings.newBuilder().build())
                        .join();
                if (!updateStatus.isSuccess()) {
                    throw new RuntimeException("Couldn't add message offset " + result.getOffset() + " to transaction "
                            + receiveSettings.getTransaction().getId() + ": " + updateStatus);
                }
            }
            return result;
        } finally {
            queueLock.unlock();
        }
    }

    @Override
    public Message receive(ReceiveSettings receiveSettings) throws InterruptedException {
        if (receiveSettings.getTimeout() != null) {
            return receiveInternal(receiveSettings, receiveSettings.getTimeout(), receiveSettings.getTimeoutTimeUnit());
        }

        Message result;
        // Poll to prevent infinite wait in case if reader was stopped
        do {
            result = receiveInternal(receiveSettings, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        } while (result == null);
        return result;
    }

    @Override
    protected CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event) {
        // Completes when all messages from this event are read by user
        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        if (isStopped.get()) {
            resultFuture.completeExceptionally(new RuntimeException("Reader was stopped"));
            return resultFuture;
        }
        if (event.getMessages().isEmpty()) {
            resultFuture.completeExceptionally(new RuntimeException("Batch has no messages"));
            return resultFuture;
        }

        queueLock.lock();

        try {
            logger.debug("Putting a message batch into queue and notifying in case receive method is waiting");
            batchesInQueue.add(new MessageBatchWrapper(event.getMessages(), resultFuture));
            queueIsNotEmptyCondition.signal();
        } finally {
            queueLock.unlock();
        }
        return resultFuture;
    }

    @Override
    protected void handleCommitResponse(long committedOffset, PartitionSession partitionSession) {
        if (logger.isDebugEnabled()) {
            logger.debug("CommitResponse received for partition session {} (partition {}) with committedOffset {}",
                    partitionSession.getId(), partitionSession.getPartitionId(), committedOffset);
        }
    }

    @Override
    protected void handleStartPartitionSessionRequest(YdbTopic.StreamReadMessage.StartPartitionSessionRequest request,
                                                      PartitionSession partitionSession,
                                                      Consumer<StartPartitionSessionSettings> confirmCallback) {
        confirmCallback.accept(null);
    }

    @Override
    protected void handleStopPartitionSession(YdbTopic.StreamReadMessage.StopPartitionSessionRequest request,
                                              PartitionSession partitionSession, Runnable confirmCallback) {
        confirmCallback.run();
    }

    @Override
    protected void handleClosePartitionSession(PartitionSession partitionSession) {
        logger.debug("ClosePartitionSession event received. Ignoring.");
    }

    @Override
    public void shutdown() {
        shutdownImpl().join();
    }
}
