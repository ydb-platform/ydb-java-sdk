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

    private final NavigableMap<Long, PendingCommit> commitFutures = new TreeMap<>();
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

    public long confirmCommit(long committedOffset) {
        if (committedOffset <= lastCommittedOffset) { // never happens
            logger.error("[{}] received commit response. Committed offset: {} which is less than previous " +
                    "committed offset: {}.", traceID, committedOffset, lastCommittedOffset);
            return 0;
        }

        commitFuturesLock.lock();
        try {
            Map<Long, PendingCommit> confirmed = commitFutures.headMap(committedOffset, true);

            logger.debug("[{}] received commit response. Committed offset: {}. "
                    + "Previous committed offset: {} (diff is {} message(s)). Completing {} commit futures", traceID,
                    committedOffset, lastCommittedOffset, committedOffset - lastCommittedOffset, confirmed.size());

            lastCommittedOffset = committedOffset;
            long acknowledgedMessages = 0;
            for (PendingCommit pending : confirmed.values()) {
                acknowledgedMessages += pending.messages;
                pending.future.complete(null);
            }
            confirmed.clear();
            return acknowledgedMessages;
        } finally {
            commitFuturesLock.unlock();
        }
    }

    private PendingCommit registerCommit(OffsetsRange range) {
        PendingCommit pending = commitFutures.computeIfAbsent(range.getEnd(), offset -> new PendingCommit());
        pending.messages += range.getEnd() - range.getStart();
        return pending;
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
            future = registerCommit(range).future;
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
        commitFuturesLock.lock();
        try {
            ranges.forEach(this::registerCommit);
        } finally {
            commitFuturesLock.unlock();
        }
        if (!session.commitOffsets(partition, ranges)) {
            failPendingCommits();
        }
    }

    public void failPendingCommits() {
        commitFuturesLock.lock();
        try {
            if (commitFutures.isEmpty()) {
                return;
            }

            logger.info("[{}] for {} is stopping. Failing {} commit futures...", traceID, partition.getPath(),
                    commitFutures.size());
            commitFutures.values().forEach(pending ->
                    pending.future.completeExceptionally(partitionIsClosedException()));
            commitFutures.clear();
        } finally {
            commitFuturesLock.unlock();
        }
    }

    private static class PendingCommit {
        private final CompletableFuture<Void> future = new CompletableFuture<>();
        private long messages;
    }
}
