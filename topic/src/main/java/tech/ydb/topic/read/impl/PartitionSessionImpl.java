package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.Collections;
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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;

/**
 * @author Nikolay Perfilov
 */
public abstract class PartitionSessionImpl {
    private static final Logger logger = LoggerFactory.getLogger(PartitionSessionImpl.class);

    private final String debugId;
    private final PartitionSession sessionId;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final int maxBatchSize;
    private final MessageDecoder decoder;

    private final Queue<Batch> readingQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isReadingNow = new AtomicBoolean();
    private final NavigableMap<Long, CompletableFuture<Void>> commitFutures = new ConcurrentSkipListMap<>();
    private final ReentrantLock commitFuturesLock = new ReentrantLock();
    // Offset of the last read message + 1
    private long nextMessageOffset;
    private long lastCommittedOffset;

    PartitionSessionImpl(PartitionSession sessionId, String debugId, int maxBatchSize, long readFrom, long commitFrom,
            MessageDecoder decoder) {
        this.sessionId = sessionId;
        this.debugId = debugId;
        this.maxBatchSize = maxBatchSize;
        this.decoder = decoder;
        this.nextMessageOffset = readFrom;
        this.lastCommittedOffset = commitFrom;
    }

    abstract void commitRanges(List<OffsetsRange> rangeWrapper);
    abstract CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event);

    public PartitionSession getSessionId() {
        return sessionId;
    }

    public String getFullId() {
        return debugId;
    }

    public CompletableFuture<Void> addBatches(List<YdbTopic.StreamReadMessage.ReadResponse.Batch> batchList) {
        if (!isWorking.get()) {
            return CompletableFuture.completedFuture(null);
        }
        List<CompletableFuture<Void>> batchFutures = new LinkedList<>();
        for (YdbTopic.StreamReadMessage.ReadResponse.Batch batch: batchList) {
            if (batch.getMessageDataCount() == 0) {
                logger.error("[{}] Received empty batch. This shouldn't happen", debugId);
                return CompletableFuture.completedFuture(null);
            }

            BatchMeta meta = new BatchMeta(batch);
            List<MessageImpl> messages = new ArrayList<>();
            for (YdbTopic.StreamReadMessage.ReadResponse.MessageData msg: batch.getMessageDataList()) {
                // Messages can be removed by TTL so server can skip a lot of messages,
                // but reader has to commit a range including skipped offsets
                messages.add(new MessageImpl(this, meta, nextMessageOffset, msg));
                nextMessageOffset = msg.getOffset() + 1;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("[{}] Received a batch of {} messages (offsets {} - {})", debugId, messages.size(),
                        messages.get(0).getOffset(), messages.get(messages.size() - 1).getOffset());
            }

            Batch newBatch = new Batch(meta, messages);
            batchFutures.add(newBatch.getReadFuture());

            readingQueue.offer(newBatch);
            if (!newBatch.isReady()) {
                decoder.decode(debugId, newBatch, this::sendDataToReadersIfNeeded);
            }
        }

        sendDataToReadersIfNeeded();
        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture<?>[0]));
    }

    private void registerCommitFuture(OffsetsRange range, CompletableFuture<Void> resultFuture) {
        commitFuturesLock.lock();
        try {
            if (isWorking.get()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[{}] Offset range {} is requested to be committed. "
                            + "Last committed offset is {} (commit lag is {})",
                            debugId, range, lastCommittedOffset, range.getStart() - lastCommittedOffset);
                }
                commitFutures.put(range.getEnd(), resultFuture);
            } else {
                logger.info("[{}] Offset range {} is requested to be committed, but partition session " +
                        "is already closed", debugId, range);
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

        commit(Collections.singletonList(range));
    }

    public void commit(List<OffsetsRange> ranges) {
        if (isWorking.get()) {
            commitRanges(ranges);
            return;
        }

        logger.info("[{}] Offset ranges {} are requested to be committed, but partition session is already closed",
                debugId, ranges.stream().map(OffsetsRange::toString).collect(Collectors.joining(",")));
    }

    public void handleCommitResponse(long committedOffset) {
        if (committedOffset <= lastCommittedOffset) {
            logger.error("[{}] Commit response received. Committed offset: {} which is less than previous " +
                    "committed offset: {}.", debugId, committedOffset, lastCommittedOffset);
            return;
        }
        Map<Long, CompletableFuture<Void>> futuresToComplete = commitFutures.headMap(committedOffset, true);
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] Commit response received. Committed offset: {}. Previous committed offset: {} " +
                            "(diff is {} message(s)). Completing {} commit futures", debugId, committedOffset,
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
            List<MessageImpl> messagesToRead = new ArrayList<>(next.getMessages());

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
            DataReceivedEvent event = new DataReceivedEventImpl(this, messagesToRead);
            if (logger.isDebugEnabled()) {
                logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) is about " +
                                "to be called...", debugId, messagesToRead.size(),
                                    messagesToRead.get(0).getOffset(),
                                    messagesToRead.get(messagesToRead.size() - 1).getOffset());
            }
            handleDataReceivedEvent(event).whenComplete((res, th) -> {
                if (th != null) {
                    logger.error("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) finished"
                            + " with error: ", debugId, messagesToRead.size(),
                            messagesToRead.get(0).getOffset(),
                            messagesToRead.get(messagesToRead.size() - 1).getOffset(), th);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("[{}] DataReceivedEvent callback with {} message(s) (offsets {}-{}) "
                            + "successfully finished", debugId, messagesToRead.size(),
                            messagesToRead.get(0).getOffset(),
                            messagesToRead.get(messagesToRead.size() - 1).getOffset());
                }
                isReadingNow.set(false);
                batchesToRead.forEach(Batch::complete);
                sendDataToReadersIfNeeded();
            });
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("[{}] No need to send data to readers: reading is already being performed", debugId);
            }
        }
    }

    public void shutdown() {
        commitFuturesLock.lock();

        try {
            isWorking.set(false);
            logger.info("[{}] Partition session for {} is shutting down. Failing {} commit futures...", debugId,
                    sessionId.getPath(), commitFutures.size());
            commitFutures.values().forEach(f -> f.completeExceptionally(
                    new RuntimeException("" + sessionId + " is closed")
            ));
        } finally {
            commitFuturesLock.unlock();
        }
    }
}
