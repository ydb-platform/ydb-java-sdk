package tech.ydb.topic.read.impl;

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
        INIT,
        ACTIVE,
        STOPPED,
        CLOSED
    }

    private final String traceID;
    private final ReadSession session;
    private final PartitionSession partition;

    private final ReadPartitionCommitter committer;
    private final ReadPartitionDecoder decoder;
    private final ReadPartitionQueue queue;

    private final ReaderMetrics metrics;
    private final Executor dataExecutor;
    private final Executor controlExecutor;

    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

    ReadPartition(String traceID, ReadSession session, PartitionSession partition, long lastCommittedOffset) {
        this.traceID = traceID;
        this.session = session;
        this.partition = partition;

        ReadConfig config = session.getConfig();
        MessageDecoder sessionDecoder = session.getDecoder();

        this.committer = new ReadPartitionCommitter(traceID, session, partition, lastCommittedOffset);
        this.decoder = new ReadPartitionDecoder(traceID, sessionDecoder, partition, committer, this::sendDataToReaders);
        this.queue = new ReadPartitionQueue(traceID, decoder, config.getMaxBatchSize(), lastCommittedOffset);

        this.metrics = config.getMetrics();
        this.dataExecutor = new SerialExecutor(config.getDataExecutor());
        this.controlExecutor = new SerialExecutor(config.getDataExecutor());
    }

    @Override
    public boolean isActive() {
        return state.get() == State.INIT || state.get() == State.ACTIVE;
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
        controlExecutor.execute(() -> {
            long lastAck = committer.completePendingCommits();
            session.getHandler().onCommitAck(new CommitOffsetAcknowledgementEventImpl(partition, lastAck));
        });
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
            while (state.get() == State.ACTIVE) {
                List<Message> list = queue.getNextBatch();
                if (list == null) {
                    return;
                }
                session.getHandler().onData(this, new DataReceivedEventImpl(partition, committer, list));
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
        committer.failPendingCommits();
        logger.info("[{}] stopped", traceID);

        if (old != State.STOPPED) { // if partition was already stopped by user, there is no need to send a closed event
            PartitionSessionClosedEventImpl event = new PartitionSessionClosedEventImpl(partition);
            controlExecutor.execute(() -> session.getHandler().onPartitionClosed(event));
        }
    }

    public void start(long committed, OffsetsRange offsets) {
        logger.info("[{}] Received StartPartitionSessionRequest for {} and consumer \"{}\" with committedOffset {} and "
                + "partitionOffsets {}", traceID, partition, session.getConfig().getConsumerName(), committed, offsets);

        committer.updateCommittedOffset(committed);
        queue.updateLastReadOffset(committed);
        controlExecutor.execute(() -> {
            if (state.get() == State.INIT) {
                session.getHandler().onPartitionStarted(new StartEvent(committed, offsets));
            }
        });
    }

    public void stop(long committed) {
        logger.info("[{}] Received graceful StopPartitionSessionRequest for {} with committedOffset {}",
                traceID, partition, committed);

        committer.updateCommittedOffset(committed);
        controlExecutor.execute(() -> {
            if (state.get() == State.ACTIVE) {
                session.getHandler().onPartitionStopped(new StopEvent(committed));
            }
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

            if (commitTo != null) {
                committer.updateCommittedOffset(commitTo);
                queue.updateLastReadOffset(commitTo); // avoid lags of commits
            }

            String consumer = session.getConfig().getConsumerName();
            if (state.compareAndSet(State.INIT, State.ACTIVE)) {
                logger.info("[{}] Sending StartPartitionSessionResponse for {} and consumer \"{}\" with readOffset {} "
                        + "and commitOffset {}", traceID, partition, consumer, readFrom, commitTo);
                session.sendStartPartition(partition, readFrom, commitTo);
            } else {
                logger.warn("[{}] Need to send StartPartitionSessionResponse, but reading session is already closed",
                        traceID);
            }
        }
    }

    private class StopEvent extends StopPartitionSessionEventImpl {
        StopEvent(long committedOffset) {
            super(partition, committedOffset);
        }

        @Override
        public void confirm() {
            if (state.compareAndSet(State.ACTIVE, State.STOPPED)) {
                logger.info("[{}] Sending StopPartitionSessionResponse for {}", traceID, partition);
                session.sendStopPartition(partition);
            } else {
                logger.info("[{}] Need to send StopPartitionSessionResponse for {}, " +
                        "but reading session is already closed", traceID, partition);
            }
        }
    }
}
