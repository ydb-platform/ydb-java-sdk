package tech.ydb.topic.read.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
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

    private volatile long lastCommittedOffset;

    ReadPartitionCommitter(String traceID, ReadSession session, PartitionSession partition, long lastCommittedOffset) {
        this.traceID = traceID;
        this.session = session;
        this.partition = partition;
        this.lastCommittedOffset = lastCommittedOffset;
    }

    private RuntimeException partitionIsClosedException() {
        return new RuntimeException("" + partition + " is already stopped");
    }

    public void confirmCommit(long committedOffset) {
        if (committedOffset <= lastCommittedOffset) { // never happens
            logger.error("[{}] received commit response. Committed offset: {} which is less than previous " +
                    "committed offset: {}.", traceID, committedOffset, lastCommittedOffset);
            return;
        }

        commitFuturesLock.lock();
        try {
            Map<Long, CompletableFuture<Void>> confirmed = commitFutures.headMap(committedOffset, true);

            logger.debug("[{}] received commit response. Committed offset: {}. "
                    + "Previous committed offset: {} (diff is {} message(s)). Completing {} commit futures", traceID,
                    committedOffset, lastCommittedOffset, committedOffset - lastCommittedOffset, confirmed.size());

            lastCommittedOffset = committedOffset;
            confirmed.values().forEach(future -> future.complete(null));
            confirmed.clear();
        } finally {
            commitFuturesLock.unlock();
        }
    }

    @Override
    public CompletableFuture<Void> commit(OffsetsRange range) {
        logger.debug(
                "[{}] Offset range {} is requested to be committed. Last committed offset is {} (commit lag is {})",
                traceID, range, lastCommittedOffset, range.getStart() - lastCommittedOffset
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

    public void failPendingCommits() {
        commitFuturesLock.lock();
        try {
            if (commitFutures.isEmpty()) {
                return;
            }

            logger.info("[{}] for {} is stopping. Failing {} commit futures...", traceID, partition.getPath(),
                    commitFutures.size());
            commitFutures.values().forEach(f -> f.completeExceptionally(partitionIsClosedException()));
            commitFutures.clear();
        } finally {
            commitFuturesLock.unlock();
        }
    }
}
