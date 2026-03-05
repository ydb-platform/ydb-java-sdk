package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

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
public abstract class PartitionSessionImpl {
    private static final Logger logger = LoggerFactory.getLogger(PartitionSessionImpl.class);

    private final String fullId;
    private final PartitionSession sessionId;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final int maxBatchSize;
    private final MessageDecoder decoder;

    private final Queue<Batch> readingQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isReadingNow = new AtomicBoolean();
    private final NavigableMap<Long, CompletableFuture<Void>> commitFutures = new ConcurrentSkipListMap<>();
    private final ReentrantLock commitFuturesLock = new ReentrantLock();
    // Offset of the last read message + 1
    private long lastReadOffset;
    private long lastCommittedOffset;

    PartitionSessionImpl(YdbTopic.StreamReadMessage.StartPartitionSessionRequest request, String fullId,
            int maxBatchSize, String consumerName, MessageDecoder decoder) {
        YdbTopic.StreamReadMessage.PartitionSession s = request.getPartitionSession();
        this.sessionId = new PartitionSession(s.getPartitionSessionId(), s.getPartitionId(), s.getPath());
        this.fullId = fullId;
        this.maxBatchSize = maxBatchSize;
        this.decoder = decoder;
        this.lastReadOffset = request.getCommittedOffset();
        this.lastCommittedOffset = request.getCommittedOffset();

        logger.info("[{}] Partition session is started for Topic \"{}\" and Consumer \"{}\". CommittedOffset: {}. " +
                "Partition offsets: {}-{}", fullId, sessionId.getPath(), consumerName, lastReadOffset,
                request.getPartitionOffsets().getStart(), request.getPartitionOffsets().getEnd());
    }

    abstract void commitRanges(List<OffsetsRange> rangeWrapper);
    abstract CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event);

    public PartitionSession getSessionId() {
        return sessionId;
    }

    public String getFullId() {
        return fullId;
    }

    public void setLastReadOffset(long lastReadOffset) {
        this.lastReadOffset = lastReadOffset;
    }

    public void setLastCommittedOffset(long lastCommittedOffset) {
        this.lastCommittedOffset = lastCommittedOffset;
    }

    public CompletableFuture<Void> addBatches(List<YdbTopic.StreamReadMessage.ReadResponse.Batch> batches) {
        if (!isWorking.get()) {
            return CompletableFuture.completedFuture(null);
        }
        List<CompletableFuture<Void>> batchFutures = new LinkedList<>();
        batches.forEach(batch -> {
            BatchMeta batchMeta = new BatchMeta(batch);
            Batch newBatch = new Batch(batchMeta);
            List<YdbTopic.StreamReadMessage.ReadResponse.MessageData> batchMessages = batch.getMessageDataList();
            if (!batchMessages.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[{}] Received a batch of {} messages (offsets {} - {})", fullId, batchMessages.size(),
                            batchMessages.get(0).getOffset(), batchMessages.get(batchMessages.size() - 1).getOffset());
                }
            } else {
                logger.error("[{}] Received empty batch. This shouldn't happen", fullId);
            }
            batchMessages.forEach(messageData -> {
                long commitOffsetFrom = lastReadOffset;
                long messageOffset = messageData.getOffset();
                long newReadOffset = messageOffset + 1;
                if (newReadOffset > lastReadOffset) {
                    lastReadOffset = newReadOffset;
                    if (logger.isTraceEnabled()) {
                        logger.trace("[{}] Received a message with offset {}. lastReadOffset is now {}", fullId,
                                messageOffset, lastReadOffset);
                    }
                } else {
                    logger.error("[{}] Received a message with offset {} which is less than last read offset {} ",
                            fullId, messageOffset, lastReadOffset);
                }
                newBatch.addMessage(new MessageImpl(this, batchMeta, commitOffsetFrom, messageData));
            });
            batchFutures.add(newBatch.getReadFuture());

            readingQueue.offer(newBatch);
            if (!newBatch.isReady()) {
                decoder.decode(fullId, newBatch, this::sendDataToReadersIfNeeded);
            }
        });

        sendDataToReadersIfNeeded();
        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture<?>[0]));
    }

    private void registerCommitFuture(OffsetsRange range, CompletableFuture<Void> resultFuture) {
        commitFuturesLock.lock();
        try {
            if (isWorking.get()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[{}] Offset {} is requested to be committed. "
                            + "Last committed offset is {} (commit lag is {})",
                            fullId, range, lastCommittedOffset, range.getStart() - lastCommittedOffset);
                }
                commitFutures.put(range.getEnd(), resultFuture);
            } else {
                logger.info("[{}] Offset range [{}, {}) is requested to be committed, but partition session " +
                        "is already closed", fullId, range.getStart(), range.getEnd());
                resultFuture.completeExceptionally(new RuntimeException("" + sessionId + " is already closed"));
            }
        } finally {
            commitFuturesLock.unlock();
        }
    }

    public void commit(OffsetsRange range, CompletableFuture<Void> resultFuture) {
        if (resultFuture != null) {
            registerCommitFuture(range, resultFuture);
        }
        List<OffsetsRange> rangeWrapper = new ArrayList<>(1);
        rangeWrapper.add(range);
        commitRanges(rangeWrapper);
    }

    public void commit(List<OffsetsRange> ranges) {
        commitRanges(ranges);
    }

    public void handleCommitResponse(long committedOffset) {
        if (committedOffset <= lastCommittedOffset) {
            logger.error("[{}] Commit response received. Committed offset: {} which is less than previous " +
                    "committed offset: {}.", fullId, committedOffset, lastCommittedOffset);
            return;
        }
        Map<Long, CompletableFuture<Void>> futuresToComplete = commitFutures.headMap(committedOffset, true);
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] Commit response received. Committed offset: {}. Previous committed offset: {} " +
                            "(diff is {} message(s)). Completing {} commit futures", fullId, committedOffset,
                    lastCommittedOffset, committedOffset - lastCommittedOffset, futuresToComplete.size());
        }
        lastCommittedOffset = committedOffset;
        futuresToComplete.values().forEach(future -> future.complete(null));
        futuresToComplete.clear();
    }

    public void sendDataToReadersIfNeeded() {
        if (!isWorking.get()) {
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
            long commitFrom = next.getFirstCommitOffsetFrom();
            long commitTo = next.getLastOffset() + 1;

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
                commitTo = next.getLastOffset() + 1;
            }

            // Should be called maximum in 1 thread at a time
            OffsetsRange offsetsToCommit = new OffsetsRangeImpl(commitFrom, commitTo);
            DataReceivedEvent event = new DataReceivedEventImpl(this, messagesToRead, offsetsToCommit);
            if (logger.isDebugEnabled()) {
                logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) is about " +
                                "to be called...", fullId, messagesToRead.size(),
                                    messagesToRead.get(0).getOffset(),
                                    messagesToRead.get(messagesToRead.size() - 1).getOffset());
            }
            handleDataReceivedEvent(event).whenComplete((res, th) -> {
                if (th != null) {
                    logger.error("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) finished"
                            + " with error: ", fullId, messagesToRead.size(),
                            messagesToRead.get(0).getOffset(),
                            messagesToRead.get(messagesToRead.size() - 1).getOffset(), th);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) "
                            + "successfully finished", fullId, messagesToRead.size(),
                            messagesToRead.get(0).getOffset(),
                            messagesToRead.get(messagesToRead.size() - 1).getOffset());
                }
                isReadingNow.set(false);
                batchesToRead.forEach(Batch::complete);
                sendDataToReadersIfNeeded();
            });
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("[{}] No need to send data to readers: reading is already being performed", fullId);
            }
        }
    }

    public void shutdown() {
        commitFuturesLock.lock();

        try {
            isWorking.set(false);
            logger.info("[{}] Partition session for {} is shutting down. Failing {} commit futures...", fullId,
                    sessionId.getPath(), commitFutures.size());
            commitFutures.values().forEach(f -> f.completeExceptionally(
                    new RuntimeException("" + sessionId + " is closed")
            ));
        } finally {
            commitFuturesLock.unlock();
        }
    }
}
