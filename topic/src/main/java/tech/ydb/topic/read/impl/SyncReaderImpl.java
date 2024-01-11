package tech.ydb.topic.read.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;

/**
 * @author Nikolay Perfilov
 */
public class SyncReaderImpl extends ReaderImpl implements SyncReader {
    private static final Logger logger = LoggerFactory.getLogger(SyncReaderImpl.class);
    private static final int POLL_INTERVAL_SECONDS = 5;
    private final Queue<MessageBatchWrapper> batchesInQueue = new LinkedList<>();
    private int currentMessageIndex = 0;

    public SyncReaderImpl(TopicRpc topicRpc, ReaderSettings settings) {
        super(topicRpc, settings);
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

    @Override
    @Nullable
    public Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        if (isStopped.get()) {
            throw new RuntimeException("Reader was stopped");
        }
        synchronized (batchesInQueue) {
            if (batchesInQueue.isEmpty()) {
                long millisToWait = TimeUnit.MILLISECONDS.convert(timeout, unit);
                Instant deadline = Instant.now().plusMillis(millisToWait);
                while (true) {
                    if (!batchesInQueue.isEmpty()) {
                        break;
                    }
                    Instant now = Instant.now();
                    if (now.isAfter(deadline)) {
                        break;
                    }
                    // Using Math.max to prevent rounding duration to 0 which would lead to infinite wait
                    millisToWait = Math.max(1, Duration.between(now, deadline).toMillis());
                    logger.trace("No messages in queue. Waiting for {} ms...", millisToWait);
                    batchesInQueue.wait(millisToWait);
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
            return result;
        }
    }

    @Override
    public Message receive() throws InterruptedException {
        Message result;
        // Poll to prevent infinite wait in case if reader was stopped
        do {
            result = receive(POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
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

        synchronized (batchesInQueue) {
            logger.debug("Putting a message batch into queue and notifying in case receive method is waiting");
            batchesInQueue.add(new MessageBatchWrapper(event.getMessages(), resultFuture));
            batchesInQueue.notify();
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
                                              @Nullable Long partitionId, Runnable confirmCallback) {
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
