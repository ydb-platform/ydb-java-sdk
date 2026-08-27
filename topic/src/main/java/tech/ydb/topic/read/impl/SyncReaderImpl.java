package tech.ydb.topic.read.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.OffsetsRange;
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
public class SyncReaderImpl extends ReaderImpl implements SyncReader {
    private static final Logger logger = LoggerFactory.getLogger(SyncReaderImpl.class);
    private static final int POLL_INTERVAL_SECONDS = 5;
    private final Queue<MessageWrapper> queue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock waitingLock = new ReentrantLock();
    private final Condition waitingCondition = waitingLock.newCondition();

    private volatile String sessionId = null;

    public SyncReaderImpl(TopicRpc topicRpc, ReaderSettings settings, @Nonnull CodecRegistry codecRegistry) {
        super(topicRpc, settings, codecRegistry);
    }

    private static class MessageWrapper {
        private final Message msg;
        private final ReadPartitionSession session;
        private final OffsetsRange rangeToRelease;

        private MessageWrapper(Message msg, ReadPartitionSession session, OffsetsRange rangeToRelease) {
            this.msg = msg;
            this.session = session;
            this.rangeToRelease = rangeToRelease;
        }

        boolean isActive() {
            return !session.isStopped();
        }

        Message getMessage() {
            return msg;
        }

        void release() {
            if (rangeToRelease != null) {
                session.releaseRange(rangeToRelease);
            }
        }
    }

    @Override
    public String getSessionId() {
        return sessionId;
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

        long millisToWait = TimeUnit.MILLISECONDS.convert(timeout, unit);
        Instant deadline = Instant.now().plusMillis(millisToWait);

        while (true) {
            while (queue.isEmpty()) {
                millisToWait = Duration.between(Instant.now(), deadline).toMillis();
                if (millisToWait <= 0) {
                    logger.trace("Still no messages in queue. Returning null");
                    return null;
                }

                waitingLock.lock();
                try {
                    logger.trace("No messages in queue. Waiting for {} ms...", millisToWait);
                    waitingCondition.await(millisToWait, TimeUnit.MILLISECONDS);
                    if (isStopped.get()) {
                        throw new RuntimeException("Reader was stopped");
                    }
                } finally {
                    waitingLock.unlock();
                }
            }

            MessageWrapper next = queue.poll();
            if (!next.isActive()) {
                next.release();
                continue;
            }

            Message result = next.getMessage();
            if (receiveSettings.getTransaction() != null) {
                // TODO: Implement batching for message committing
                List<PartitionOffsets> offsets = Collections.singletonList(new PartitionOffsets(
                        result.getPartitionSession(),
                        Collections.singletonList(result.getRangeToCommit())
                ));
                Status updateStatus = updateOffsetsInTransaction(
                        receiveSettings.getTransaction(),
                        Collections.singletonMap(result.getPartitionSession().getPath(), offsets),
                        UpdateOffsetsInTransactionSettings.newBuilder().build()
                ).join();
                if (!updateStatus.isSuccess()) {
                    throw new RuntimeException("Couldn't add message offset " + result.getOffset()
                            + " to transaction " + receiveSettings.getTransaction().getId() + ": " + updateStatus);
                }
            }

            next.release();
            return result;
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
    Executor getDataHandlerExecutor() {
        return Runnable::run;
    }

    @Override
    protected void handleDataReceivedEvent(ReadPartitionSession session, DataReceivedEvent event) {
        if (isStopped.get() || event.getMessages().isEmpty()) {
            session.releaseRange(event.getRangeToCommit());
            return;
        }

        int messagesCount = event.getMessages().size();
        long offsetStart = event.getMessages().get(0).getOffset();
        long offsetEnd = event.getMessages().get(event.getMessages().size() - 1).getOffset();
        logger.debug("{} Putting a batch into queueData with {} message(s) (offsets {}-{})",
                session, messagesCount, offsetStart, offsetEnd);

        for (Message msg: event.getMessages()) {
            if (msg.getRangeToCommit().getEnd() == event.getRangeToCommit().getEnd()) { // last message in batch
                queue.offer(new MessageWrapper(msg, session, event.getRangeToCommit()));
            } else {
                queue.offer(new MessageWrapper(msg, session, null));
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
    protected void handleSessionStarted(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    protected void handleCommitResponse(long committedOffset, PartitionSession partitionSession) {
        if (logger.isDebugEnabled()) {
            logger.debug("CommitResponse received for partition session {} (partition {}) with committedOffset {}",
                    partitionSession.getId(), partitionSession.getPartitionId(), committedOffset);
        }
    }

    @Override
    protected void handleStartPartitionSessionRequest(StartPartitionSessionEvent event) {
        event.confirm();
    }

    @Override
    protected void handleStopPartitionSession(StopPartitionSessionEvent event) {
        // TODO: wait for all commits
        event.confirm();
    }

    @Override
    protected void handleClosePartitionSession(PartitionSession partition) {
        // TODO: clean reading queue
        logger.debug("ClosePartitionSession event received. Ignoring.");
    }

    @Override
    public void shutdown() {
        CompletableFuture<Void> impl = shutdownImpl();

        waitingLock.lock();
        try {
            waitingCondition.signalAll();
        } finally {
            waitingLock.unlock();
        }

        impl.join();
    }
}
