package tech.ydb.topic.read.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.impl.DebugTools;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.ReceiveSettings;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * @author Nikolay Perfilov
 */
public class SyncReaderImpl implements SyncReader {
    private static final Logger logger = LoggerFactory.getLogger(SyncReaderImpl.class);

    private static final int POLL_INTERVAL_SECONDS = 5;

    private final String debugId;
    private final LazyExecutor decompressor;
    private final ReadConfig config;
    private final ReaderImpl impl;

    private final CompletableFuture<Void> initFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();

    private final ConcurrentHashMap<PartitionSession, PartitionSession> activePartitions = new ConcurrentHashMap<>();
    private final Queue<MessageWrapper> queue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock waitingLock = new ReentrantLock();
    private final Condition waitingCondition = waitingLock.newCondition();

    private volatile String sessionId = null;

    public SyncReaderImpl(TopicRpc topicRpc, ReaderSettings settings, @Nonnull CodecRegistry codecRegistry) {
        this.debugId = DebugTools.createDebugId(settings.getLogPrefix());
        this.decompressor = new LazyExecutor("reader[" + debugId + "]-decoder", settings.getDecompressionExecutor());

        this.config = new ReadConfig(codecRegistry, Runnable::run, decompressor, settings);
        this.impl = new ReaderImpl(topicRpc, debugId, settings, config, new SyncHandler());

        String readerName = settings.getReaderName();
        String consumerName = settings.getConsumerName();
        logger.info("Reader{} (generated id {}) created for topic(s) {} and {}",
                readerName != null ? (" '" + readerName + "'") : "",
                debugId,
                settings.getTopics().stream().map(t -> "\"" + t.getPath() + "\"").collect(Collectors.joining(", ")),
                consumerName != null ? (" consumer \"" + consumerName + "\"") : "without a consumer"
        );
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void init() {
        impl.start();
    }

    @Override
    public void initAndWait() {
        impl.start();
        initFuture.join();
    }


    @Override
    public void shutdown() {
        impl.close();

        waitingLock.lock();
        try {
            waitingCondition.signalAll();
        } finally {
            waitingLock.unlock();
        }

        shutdownFuture.join();
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

    private MessageWrapper waitReadyMessage(long timeout, TimeUnit unit) throws InterruptedException {
        long millisToWait = TimeUnit.MILLISECONDS.convert(timeout, unit);
        Instant deadline = Instant.now().plusMillis(millisToWait);

        waitingLock.lock();
        try {
            MessageWrapper next = queue.poll();
            while (next == null) {
                millisToWait = Duration.between(Instant.now(), deadline).toMillis();
                if (millisToWait <= 0) {
                    logger.trace("Still no messages in queue. Returning null");
                    return null;
                }

                logger.trace("No messages in queue. Waiting for {} ms...", millisToWait);
                waitingCondition.await(millisToWait, TimeUnit.MILLISECONDS);
                if (impl.isClosed()) {
                    throw new RuntimeException("Reader was stopped");
                }
                next = queue.poll();
            }
            return next;
        } finally {
            waitingLock.unlock();
        }
    }

    @Nullable
    public Message receiveInternal(ReceiveSettings receiveSettings, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (impl.isClosed()) {
            throw new RuntimeException("Reader was stopped");
        }

        while (true) {
            MessageWrapper next = queue.poll();
            if (next == null) {
                next = waitReadyMessage(timeout, unit);
                if (next == null) {
                    return null;
                }
            }

            if (!activePartitions.containsKey(next.getPartition())) {
                continue;
            }

            Message result = next.getMessage();
            if (receiveSettings.getTransaction() != null) {
                // TODO: Implement batching for message committing
                List<PartitionOffsets> offsets = Collections.singletonList(new PartitionOffsets(
                        result.getPartitionSession(),
                        Collections.singletonList(result.getRangeToCommit())
                ));
                Status updateStatus = impl.updateOffsetsInTransaction(
                        receiveSettings.getTransaction(),
                        Collections.singletonMap(result.getPartitionSession().getPath(), offsets),
                        UpdateOffsetsInTransactionSettings.newBuilder().build()
                ).join();
                if (!updateStatus.isSuccess()) {
                    throw new RuntimeException("Couldn't add message offset " + result.getOffset()
                            + " to transaction " + receiveSettings.getTransaction().getId() + ": " + updateStatus);
                }
            }

            next.confirm();
            return result;
        }
    }

    private class SyncHandler implements ReaderImpl.Handler {
        @Override
        public void handleSessionStarted(String sessionId) {
            SyncReaderImpl.this.sessionId = sessionId;
            initFuture.complete(null);
        }

        @Override
        public void handleReaderClosed(Status status) {
            shutdownFuture.complete(null);
        }

        @Override
        public void handleDataReceivedEvent(ReaderImpl.Releaser releaser, DataReceivedEvent event) {
            if (impl.isClosed()) {
                return;
            }
            if (event.getMessages().isEmpty()) {
                releaser.releaseRange(event.getPartitionSession(), event.getRangeToCommit());
                return;
            }

            PartitionSession ps = event.getPartitionSession();
            int messagesCount = event.getMessages().size();
            long offsetStart = event.getMessages().get(0).getOffset();
            long offsetEnd = event.getMessages().get(event.getMessages().size() - 1).getOffset();
            logger.debug("{} Putting a batch into queueData with {} message(s) (offsets {}-{}) from {}",
                    debugId, messagesCount, offsetStart, offsetEnd, ps);

            Runnable confirm = () -> releaser.releaseRange(ps, event.getRangeToCommit());
            for (Message msg: event.getMessages()) {
                if (msg.getRangeToCommit().getEnd() == event.getRangeToCommit().getEnd()) { // last message in batch
                    queue.offer(new MessageWrapper(ps, msg, confirm));
                } else {
                    queue.offer(new MessageWrapper(ps, msg, null));
                }
            }

            waitingLock.lock();
            try {
                waitingCondition.signalAll();
            } finally {
                waitingLock.unlock();
            }
        }

        @Override
        public void handleCommitResponse(long committedOffset, PartitionSession partitionSession) {
            logger.debug("CommitResponse received for{} with committedOffset {}", partitionSession, committedOffset);
        }

        @Override
        public void handleStartPartitionSessionRequest(StartPartitionSessionEvent event) {
            activePartitions.put(event.getPartitionSession(), event.getPartitionSession());
            event.confirm();
        }

        @Override
        public void handleStopPartitionSession(StopPartitionSessionEvent event) {
            activePartitions.remove(event.getPartitionSession());
            // TODO: wait for all commits
            event.confirm();
        }

        @Override
        public void handleClosePartitionSession(PartitionSession partition) {
            activePartitions.remove(partition);
        }
    }

    private static class MessageWrapper {
        private final PartitionSession partition;
        private final Message msg;
        private final Runnable confirm;

        private MessageWrapper(PartitionSession partition, Message msg, Runnable confirm) {
            this.partition = partition;
            this.msg = msg;
            this.confirm = confirm;
        }

        Message getMessage() {
            return msg;
        }

        PartitionSession getPartition() {
            return partition;
        }

        void confirm() {
            if (confirm != null) {
                confirm.run();
            }
        }
    }
}
