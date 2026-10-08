package tech.ydb.topic.read.impl;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.impl.SerialExecutor;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.CommitOffsetAcknowledgementEvent;
import tech.ydb.topic.read.impl.events.CommitOffsetAcknowledgementEventImpl;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;
import tech.ydb.topic.read.impl.events.PartitionSessionClosedEventImpl;
import tech.ydb.topic.read.impl.events.PartitionSessionEndedEventImpl;
import tech.ydb.topic.read.impl.events.StartPartitionSessionEventImpl;
import tech.ydb.topic.read.impl.events.StopPartitionSessionEventImpl;
import tech.ydb.topic.settings.StartPartitionSessionSettings;

/**
 * @author Nikolay Perfilov
 */
public class ReadPartition implements ReadSession.PartitionControl {
    private static final Logger logger = LoggerFactory.getLogger(ReadPartition.class);

    private enum State {
        /** Partition is created */
        CREATED,
        /** StartEvent is sent to user to confirm */
        INITED,
        /** Partition is active */
        STARTED,
        /** StopEvent is sent to user to confirm */
        PRE_STOPPED,
        /** Partition is confirmed by user to stop */
        STOPPED,
        /** Partition is closed and removed from reader */
        CLOSED;

        static final EnumSet<State> IS_ACTIVE = EnumSet.of(CREATED, INITED, STARTED, PRE_STOPPED);
    }

    private final String traceID;
    private final ReadSession session;
    private final PartitionSession partition;

    private final ReaderMetrics metrics;
    private final Executor dataExecutor;
    private final Executor controlExecutor;

    private final ReadPartitionCommitter committer;
    private final ReadPartitionDecoder decoder;
    private final ReadPartitionQueue queue;

    private final AtomicReference<State> state = new AtomicReference<>(State.CREATED);
    private final AtomicReference<CommitOffsetAcknowledgementEvent> commitOffsetAck = new AtomicReference<>(null);
    private final AtomicReference<PartitionSessionEndedEventImpl> partitonEnd = new AtomicReference<>(null);
    private volatile boolean hasUnprocessedMessages = false;
    private volatile boolean isPaused = true;

    ReadPartition(String traceID, ReadSession session, PartitionSession partition, long lastCommittedOffset) {
        this.traceID = traceID;
        this.session = session;
        this.partition = partition;

        ReadConfig config = session.getConfig();
        MessageDecoder sessionDecoder = session.getDecoder();

        this.metrics = config.getMetrics();
        this.dataExecutor = new SerialExecutor(config.getDataExecutor());
        this.controlExecutor = config.getControlExecutor();

        this.committer = new ReadPartitionCommitter(traceID, session, partition, lastCommittedOffset);
        this.decoder = new ReadPartitionDecoder(traceID, sessionDecoder, partition, committer, this::sendDataToReaders);
        this.queue = new ReadPartitionQueue(traceID, decoder, config.getMaxBatchSize(), lastCommittedOffset);

        logger.info("[{}] started for {}", traceID, partition);
    }

    @Override
    public boolean isActive() {
        return State.IS_ACTIVE.contains(state.get());
    }

    public PartitionSession getPartition() {
        return partition;
    }

    @Override
    public void confirmRangeProcessed(OffsetsRange range) {
        decoder.releaseRange(range);
        hasUnprocessedMessages = session.getBufferManager().releaseRange(partition.getId(), range);
        trySendPartitionEnded();
        sendDataToReaders();
    }

    public void confirmPartitionEnded(List<TopicPartition> childs) {
        PartitionSessionEndedEventImpl event = new PartitionSessionEndedEventImpl(partition, childs);
        logger.info("[{}] got EndPartitionSession with child partitions {}", traceID, event.getChildsString());
        partitonEnd.set(event);
        trySendPartitionEnded();
    }

    public void confirmCommittedOffset(long committedOffset) {
        committer.updateCommittedOffset(committedOffset);
        committer.completePendingCommits();
        commitOffsetAck.set(new CommitOffsetAcknowledgementEventImpl(partition, committedOffset));
        sendDataToReaders();
    }

    public void unpause() {
        isPaused = false;
        if (state.get() != State.CREATED) {
            logger.info("[{}] was unpaused and ready to send data", traceID);
            sendDataToReaders();
        }
    }

    private void trySendCommitOffsetAck() {
        CommitOffsetAcknowledgementEvent event = commitOffsetAck.getAndSet(null);
        if (event != null) {
            session.getHandler().onCommitAck(event);
        }
    }

    private void trySendPartitionEnded() {
        if (hasUnprocessedMessages) {
            return;
        }
        PartitionSessionEndedEventImpl event = partitonEnd.getAndSet(null);
        if (event != null) {
            dataExecutor.execute(() -> session.getHandler().onPartitionEnded(event));
            logger.info("[{}] has finished processing and unpaused child partitions {}", traceID,
                    event.getChildsString());
            session.releasePartitions(event.getChilds());
        }
    }

    public boolean addBatches(List<YdbTopic.StreamReadMessage.ReadResponse.Batch> batchList) {
        if (!isActive()) {
            return false;
        }

        long messagesCount = batchList.stream()
                .mapToInt(YdbTopic.StreamReadMessage.ReadResponse.Batch::getMessageDataCount)
                .sum();
        metrics.reportReceivedMessages(messagesCount, partition.getPath());

        if (messagesCount > 0) {
            hasUnprocessedMessages = true;
            queue.addBatches(batchList);
            sendDataToReaders();
        }

        return isActive();
    }

    private void sendDataToReaders() {
        dataExecutor.execute(() -> {
            trySendCommitOffsetAck(); // ack may be sent even state is not active
            if (isPaused) {
                return;
            }
            while (state.get() == State.STARTED || state.get() == State.PRE_STOPPED) {
                List<Message> list = queue.getNextBatch();
                if (list == null) {
                    return;
                }

                DataReceivedEventImpl event = new DataReceivedEventImpl(partition, committer, list);

                int messagesCount = event.getMessages().size();
                long offsetStart = event.getMessages().get(0).getOffset();
                long offsetEnd = event.getMessages().get(event.getMessages().size() - 1).getOffset();
                logger.debug("[{}] onData with {} message(s) (offsets {}-{}) is about to be called...",
                        traceID, messagesCount, offsetStart, offsetEnd);
                session.getHandler().onData(this, event);
                logger.debug("[{}] onData with {} message(s) (offsets {}-{}) successfully finished",
                        traceID, messagesCount, offsetStart, offsetEnd);

                trySendCommitOffsetAck();
            }
        });
    }

    public void close() {
        State old = state.getAndSet(State.CLOSED);
        if (old == State.CLOSED) {
            return;
        }

        session.getBufferManager().releasePartition(partition.getId());
        decoder.close();
        committer.close();
        logger.info("[{}] with state {} was closed", traceID, old);

        if (old != State.STOPPED && old != State.CREATED) {
            PartitionSessionClosedEventImpl event = new PartitionSessionClosedEventImpl(partition);
            // partition close event doesn't use partition's executors
            controlExecutor.execute(() -> session.getHandler().onPartitionClosed(event));
        }
    }

    public void start(long committed, OffsetsRange offsets) {
        logger.info("[{}] got StartPartitionSessionRequest with committed offset {} and partition offsets {}",
                traceID, committed, offsets);

        committer.updateCommittedOffset(committed);
        queue.updateLastReadOffset(committed);
        // partition start event doesn't use partition's executors
        controlExecutor.execute(() -> {
            if (state.compareAndSet(State.CREATED, State.INITED)) {
                session.getHandler().onPartitionStarted(new StartEvent(committed, offsets));
                return;
            }
            logger.info("[{}] skipped start confirmation event because is already {}", traceID, state.get());
        });
    }

    public void stop(long committed) {
        logger.info("[{}] got graceful StopPartitionSessionRequest with committedOffset {}", traceID, committed);

        if (state.compareAndSet(State.CREATED, State.STOPPED)) {
            logger.info("[{}] stop was auto confirmed the partition is not initialized yet", traceID);
            session.sendStopPartition(partition);
            return;
        }

        committer.updateCommittedOffset(committed);
        controlExecutor.execute(() -> {
            if (state.compareAndSet(State.INITED, State.STOPPED)) {
                logger.info("[{}] was auto stopped because the partition start is not confirmed yet", traceID);
                session.sendStopPartition(partition);

                // partition close event doesn't use partition's executors
                PartitionSessionClosedEventImpl event = new PartitionSessionClosedEventImpl(partition);
                session.getConfig().getControlExecutor().execute(() -> session.getHandler().onPartitionClosed(event));
                return;
            }
            if (state.compareAndSet(State.STARTED, State.PRE_STOPPED)) {
                session.getHandler().onPartitionStopped(new StopEvent(committed));
                return;
            }
            logger.warn("[{}] skipped onStop because the partition session is already {}", traceID, state.get());
        });
    }

    private class StartEvent extends StartPartitionSessionEventImpl {
        StartEvent(long committed, OffsetsRange offsets) {
            super(partition, committed, offsets);
        }

        @Override
        public void confirm(StartPartitionSessionSettings settings) {
            Long readFrom = settings != null ? settings.getReadOffset() : null;
            Long commitTo = settings != null ? settings.getCommitOffset() : null;

            if (!state.compareAndSet(State.INITED, State.STARTED)) {
                logger.warn("[{}] needs to send StartPartitionSessionResponse, but the partition session is already {}",
                        traceID, state.get());
                return;
            }

            if (commitTo != null) {
                committer.updateCommittedOffset(commitTo);
                queue.updateLastReadOffset(commitTo); // avoid lags of commits
            }

            logger.info("[{}] sent StartPartitionSessionResponse with readOffset {} and commitOffset {}", traceID,
                    readFrom, commitTo);
            session.sendStartPartition(partition, readFrom, commitTo);
            sendDataToReaders();
        }
    }

    private class StopEvent extends StopPartitionSessionEventImpl {
        StopEvent(long committedOffset) {
            super(partition, committedOffset);
        }

        @Override
        public void confirm() {
            if (state.compareAndSet(State.PRE_STOPPED, State.STOPPED)) {
                logger.info("[{}] sent StopPartitionSessionResponse", traceID);
                session.sendStopPartition(partition);
            } else {
                logger.warn("[{}] needs to send StopPartitionSessionResponse, but the partition session is already {}",
                        traceID, partition);
            }
        }
    }
}
