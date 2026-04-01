package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;

/**
 * @author Nikolay Perfilov
 */
public abstract class ReadPartitionSession {
    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    private final String traceID;
    private final ReadSession session;
    private final PartitionSession partition;
    private final int maxBatchSize;
    private final MessageDecoder decoder;
    private final MessageCommitterImpl committer;
    private volatile long lastReadOffset;

    private volatile boolean isStopped = false;

    private final Queue<Batch> readingQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isReadingNow = new AtomicBoolean();

    ReadPartitionSession(String traceID, ReadSession session, PartitionSession partition, long lastCommittedOffset) {
        this.traceID = traceID;
        this.session = session;
        this.partition = partition;
        this.maxBatchSize = session.getMaxBatchSize();
        this.decoder = session.getMessageDecoder();
        this.committer = new MessageCommitterImpl(this, lastCommittedOffset);
        this.lastReadOffset = lastCommittedOffset;
    }

    @Override
    public String toString() {
        return "[" + traceID + "]";
    }

    public PartitionSession getPartition() {
        return partition;
    }

    boolean commitOffsets(List<OffsetsRange> ranges) {
        if (isStopped) {
            logger.info("{} Offset ranges {} are requested to be committed, but partition session is already closed",
                    this, ranges.stream().map(OffsetsRange::toString).collect(Collectors.joining(",")));
            return false;
        }
        session.sendCommitOffsetRequest(partition, ranges);
        return true;
    }

    void confirmCommit(long committedOffset) {
        committer.confirmCommit(committedOffset);
    }

    public void stop() {
        isStopped = true;
        committer.failPendingCommits();
        logger.info("{} stopped", this);
    }

    abstract CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event);

    public CompletableFuture<Void> addBatches(List<YdbTopic.StreamReadMessage.ReadResponse.Batch> batchList) {
        if (isStopped) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> batchFutures = new LinkedList<>();
        for (YdbTopic.StreamReadMessage.ReadResponse.Batch batch: batchList) {
            if (batch.getMessageDataCount() == 0) {
                logger.error("{} Received empty batch. This shouldn't happen", this);
                return CompletableFuture.completedFuture(null);
            }

            BatchMeta meta = new BatchMeta(batch);
            List<MessageImpl> messages = new ArrayList<>();
            for (YdbTopic.StreamReadMessage.ReadResponse.MessageData msg: batch.getMessageDataList()) {
                if (lastReadOffset > msg.getOffset()) {
                    logger.error("{} Received a message with offset {} which is less than last read offset {} ",
                            this, msg.getOffset(), lastReadOffset);
                    lastReadOffset = msg.getOffset();
                }

                OffsetsRange commitRange = OffsetsRange.of(lastReadOffset, msg.getOffset() + 1);
                messages.add(new MessageImpl(partition, committer, meta, commitRange, msg));
                lastReadOffset = commitRange.getEnd();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("[{}] Received a batch of {} messages (offsets {} - {})", traceID, messages.size(),
                        messages.get(0).getOffset(), messages.get(messages.size() - 1).getOffset());
            }

            Batch newBatch = new Batch(meta, messages);
            batchFutures.add(newBatch.getReadFuture());

            readingQueue.offer(newBatch);
            if (!newBatch.isReady()) {
                decoder.decode(traceID, newBatch, this::sendDataToReadersIfNeeded);
            }
        }

        sendDataToReadersIfNeeded();
        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture<?>[0]));
    }

    public void sendDataToReadersIfNeeded() {
        if (isStopped) {
            return;
        }

        if (isReadingNow.compareAndSet(false, true)) {
            List<Batch> batchesToRead = new ArrayList<>();

            Batch next = readingQueue.peek();
            if (next == null || !next.isReady()) {
                isReadingNow.set(false);
                return;
            }
            next = readingQueue.poll();

            batchesToRead.add(next);
            List<Message> messagesToRead = new ArrayList<>(next.getMessages());

            int batchSize = messagesToRead.size();
            while (maxBatchSize <= 0 || batchSize < maxBatchSize) {
                next = readingQueue.peek();
                if (next == null || !next.isReady()) {
                    break;
                }
                if (maxBatchSize > 0 && next.getMessages().size() + batchSize > maxBatchSize) {
                    break;
                }

                next = readingQueue.poll();

                batchesToRead.add(next);
                messagesToRead.addAll(next.getMessages());
                batchSize += next.getMessages().size();
            }

            // Should be called maximum in 1 thread at a time
            DataReceivedEvent event = new DataReceivedEventImpl(partition, committer, messagesToRead);
            if (logger.isDebugEnabled()) {
                logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) is about " +
                                "to be called...", traceID, messagesToRead.size(),
                                    messagesToRead.get(0).getOffset(),
                                    messagesToRead.get(messagesToRead.size() - 1).getOffset());
            }
            handleDataReceivedEvent(event).whenComplete((res, th) -> {
                if (th != null) {
                    logger.error("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) finished"
                            + " with error: ", traceID, messagesToRead.size(),
                            messagesToRead.get(0).getOffset(),
                            messagesToRead.get(messagesToRead.size() - 1).getOffset(), th);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) "
                            + "successfully finished", traceID, messagesToRead.size(),
                            messagesToRead.get(0).getOffset(),
                            messagesToRead.get(messagesToRead.size() - 1).getOffset());
                }
                isReadingNow.set(false);
                batchesToRead.forEach(Batch::complete);
                sendDataToReadersIfNeeded();
            });
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("[{}] No need to send data to readers: reading is already being performed", traceID);
            }
        }
    }
}
