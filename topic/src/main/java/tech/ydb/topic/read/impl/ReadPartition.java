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
    }

    @Override
    public boolean isActive() {
        return State.IS_ACTIVE.contains(state.get());
    }

    @Override
    public void confirmRangeProcessed(OffsetsRange range) {
        session.getBufferManager().releaseRange(partition.getId(), range);
        decoder.releaseRange(range);
        sendDataToReaders();
    }

    public PartitionSession getPartition() {
        return partition;
    }

    public void confirmCommittedOffset(long committedOffset) {
        committer.updateCommittedOffset(committedOffset);
        committer.completePendingCommits();
        commitOffsetAck.set(new CommitOffsetAcknowledgementEventImpl(partition, committedOffset));
        sendDataToReaders();
    }

    private void sendCommitOffsetAck() {
        CommitOffsetAcknowledgementEvent event = commitOffsetAck.getAndSet(null);
        if (event != null) {
            session.getHandler().onCommitAck(event);
        }
    }

    void recordCommitRequest(List<OffsetsRange> ranges) {
        committer.recordCommitRequest(ranges);
    }

    long getCommitOffsetLag() {
        return committer.getCommitOffsetLag();
    }

    public boolean addBatches(List<YdbTopic.StreamReadMessage.ReadResponse.Batch> batchList) {
        if (!isActive()) {
            return false;
        }
        queue.addBatches(batchList);
        long messagesCount = 0;
        for (YdbTopic.StreamReadMessage.ReadResponse.Batch batch : batchList) {
            messagesCount += batch.getMessageDataCount();
        }
        metrics.reportReceivedMessages(messagesCount, partition.getPath());
        sendDataToReaders();
        return isActive();
    }

    private void sendDataToReaders() {
        dataExecutor.execute(() -> {
            sendCommitOffsetAck(); // ack may be sent even state is not active
            while (state.get() == State.STARTED || state.get() == State.PRE_STOPPED) {
                List<Message> list = queue.getNextBatch();
                if (list == null) {
                    return;
                }

                DataReceivedEventImpl event = new DataReceivedEventImpl(partition, committer, list);
                session.getHandler().onData(this, event);
                sendCommitOffsetAck();
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
        logger.info("[{}] with state {} was stopped", traceID, old);

        if (old != State.STOPPED && old != State.CREATED) {
            PartitionSessionClosedEventImpl event = new PartitionSessionClosedEventImpl(partition);
            // partition close event doesn't use partition's executors
            controlExecutor.execute(() -> session.getHandler().onPartitionClosed(event));
        }
    }

    public void start(long committed, OffsetsRange offsets) {
        logger.info("[{}] Received StartPartitionSessionRequest for {} and consumer \"{}\" with committedOffset {} and "
                + "partitionOffsets {}", traceID, partition, session.getConfig().getConsumerName(), committed, offsets);

        committer.updateCommittedOffset(committed);
        queue.updateLastReadOffset(committed);
        // partition start event doesn't use partition's executors
        controlExecutor.execute(() -> {
            if (state.compareAndSet(State.CREATED, State.INITED)) {
                session.getHandler().onPartitionStarted(new StartEvent(committed, offsets));
                return;
            }
            logger.info("[{}] skipped onStart event because the partition session is already {}", traceID, state.get());
        });
    }

    public void stop(long committed) {
        logger.info("[{}] Received graceful StopPartitionSessionRequest for {} with committedOffset {}",
                traceID, partition, committed);

        if (state.compareAndSet(State.CREATED, State.STOPPED)) {
            logger.info("[{}] was auto stopped because the partition is not initialized yet", traceID);
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
            logger.warn("[{}] skipped onStop event because the partition session is already {}", traceID, state.get());
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

            String consumer = session.getConfig().getConsumerName();
            if (state.compareAndSet(State.INITED, State.STARTED)) {
                if (commitTo != null) {
                    committer.updateCommittedOffset(commitTo);
                    queue.updateLastReadOffset(commitTo); // avoid lags of commits
                }

                logger.info("[{}] Sending StartPartitionSessionResponse for {} and consumer \"{}\" with readOffset {} "
                        + "and commitOffset {}", traceID, partition, consumer, readFrom, commitTo);
                session.sendStartPartition(partition, readFrom, commitTo);
                sendDataToReaders();
            } else {
                logger.warn("[{}] Need to send StartPartitionSessionResponse, but the partition session is already {}",
                        traceID, state.get());
            }
        }
    }

    private class StopEvent extends StopPartitionSessionEventImpl {
        StopEvent(long committedOffset) {
            super(partition, committedOffset);
        }

        @Override
        public void confirm() {
            if (state.compareAndSet(State.PRE_STOPPED, State.STOPPED)) {
                logger.info("[{}] Sending StopPartitionSessionResponse for {}", traceID, partition);
                session.sendStopPartition(partition);
            } else {
                logger.warn("[{}] Need to send StopPartitionSessionResponse, but the partition session is already {}",
                        traceID, partition);
            }
        }
    }
}
