package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.MessageCommitter;
import tech.ydb.topic.read.PartitionSession;

/**
 *
 * @author Aleksandr Gorshenin
 */
class ReadPartitionCommitter implements MessageCommitter {
    private static final Logger logger = LoggerFactory.getLogger(ReadPartition.class);

    private final String traceID;
    private final ReadSession session;
    private final PartitionSession partition;

    private final NavigableMap<Long, CompletableFuture<Void>> commitFutures = new TreeMap<>();
    private final ReentrantLock commitFuturesLock = new ReentrantLock();

    private final AtomicLong lastCommittedOffset;

    ReadPartitionCommitter(String traceID, ReadSession session, PartitionSession partition, long lastCommittedOffset) {
        this.traceID = traceID;
        this.session = session;
        this.partition = partition;
        this.lastCommittedOffset = new AtomicLong(lastCommittedOffset);
    }

    private RuntimeException partitionIsClosedException() {
        return new RuntimeException("" + partition + " is already stopped");
    }

    public void updateCommittedOffset(long offset) {
        long old = lastCommittedOffset.get();
        if (old == lastCommittedOffset.accumulateAndGet(offset, Math::max)) {
            return;
        }
        logger.debug("[{}] Updated last committed offset: {}. Previous committed offset: {} (diff is {} message(s)).",
                traceID, offset, old, offset - old);
    }

    public long completePendingCommits() {
        List<CompletableFuture<Void>> completed = new ArrayList<>();
        commitFuturesLock.lock();
        long last = lastCommittedOffset.get();
        try {
            Map<Long, CompletableFuture<Void>> ready = commitFutures.headMap(last, true);
            if (!ready.isEmpty()) {
                logger.debug("[{}] Completing {} commit futures by confirmed offset {}", traceID, ready.size(), last);
                ready.values().forEach(completed::add);
                ready.clear();
            }
        } finally {
            commitFuturesLock.unlock();
        }
        completed.forEach(f -> f.complete(null));
        return last;
    }

    @Override
    public CompletableFuture<Void> commit(OffsetsRange range) {
        long confirmed = lastCommittedOffset.get();
        logger.debug(
                "[{}] Offset range {} is requested to be committed. Last committed offset is {} (commit lag is {})",
                traceID, range, confirmed, range.getStart() - confirmed
        );

        CompletableFuture<Void> future;
        commitFuturesLock.lock();
        try {
            future = commitFutures.get(range.getEnd());
            if (future == null) {
                future = new CompletableFuture<>();
                commitFutures.put(range.getEnd(), future);
            }
        } finally {
            commitFuturesLock.unlock();
        }

        if (!session.commitOffsets(partition, Collections.singletonList(range))) {
            logger.info("[{}] Offset range {} is requested to be committed, but partition session is already stopped",
                    traceID, range);
            future.completeExceptionally(partitionIsClosedException());

            commitFuturesLock.lock();
            try {
                commitFutures.remove(range.getEnd());
            } finally {
                commitFuturesLock.unlock();
            }
        }

        return future;
    }

    @Override
    public void commitRanges(List<OffsetsRange> ranges) {
        session.commitOffsets(partition, ranges);
    }

    public void close() {
        completePendingCommits();
        List<CompletableFuture<Void>> failed = new ArrayList<>();
        commitFuturesLock.lock();
        try {
            if (commitFutures.isEmpty()) {
                return;
            }

            commitFutures.values().forEach(failed::add);
            commitFutures.clear();
        } finally {
            commitFuturesLock.unlock();
        }
        logger.info("[{}] for {} is stopping. Failing {} commit futures...", traceID, partition, failed.size());
        failed.forEach(f -> f.completeExceptionally(partitionIsClosedException()));
    }
}
