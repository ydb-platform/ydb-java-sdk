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

/**
 *
 * @author Aleksandr Gorshenin
 */
class MessageCommitterImpl implements MessageCommitter {
    private static final Logger logger = LoggerFactory.getLogger(ReaderImpl.class);

    private final ReadPartitionSession session;

    private final NavigableMap<Long, CompletableFuture<Void>> commitFutures = new TreeMap<>();
    private final ReentrantLock commitFuturesLock = new ReentrantLock();

    private volatile long lastCommittedOffset;

    MessageCommitterImpl(ReadPartitionSession session, long lastCommittedOffset) {
        this.session = session;
        this.lastCommittedOffset = lastCommittedOffset;
    }

    private RuntimeException partitionIsClosedException() {
        return new RuntimeException("" + session.getPartition() + " is already stopped");
    }

    public void confirmCommit(long committedOffset) {
        if (committedOffset <= lastCommittedOffset) { // never happens
            logger.error("{} received commit response. Committed offset: {} which is less than previous " +
                    "committed offset: {}.", session, committedOffset, lastCommittedOffset);
            return;
        }

        commitFuturesLock.lock();
        try {
            Map<Long, CompletableFuture<Void>> confirmed = commitFutures.headMap(committedOffset, true);

            logger.debug("{} received commit response. Committed offset: {}. "
                    + "Previous committed offset: {} (diff is {} message(s)). Completing {} commit futures", session,
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
        CompletableFuture<Void> future = new CompletableFuture<>();
        logger.debug("{} Offset range {} is requested to be committed. Last committed offset is {} (commit lag is {})",
                session, range, lastCommittedOffset, range.getStart() - lastCommittedOffset);

        commitFuturesLock.lock();
        try {
            if (session.commitOffsets(Collections.singletonList(range))) {
                commitFutures.put(range.getEnd(), future);
            } else {
                logger.info("{} Offset range {} is requested to be committed, but partition session is already stopped",
                        session, range);
                future.completeExceptionally(partitionIsClosedException());
            }
        } finally {
            commitFuturesLock.unlock();
        }

        return future;
    }

    @Override
    public void commitRanges(List<OffsetsRange> ranges) {
        session.commitOffsets(ranges);
    }

    public void failPendingCommits() {
        commitFuturesLock.lock();
        try {
            logger.info("{} for {} is stopping. Failing {} commit futures...", session,
                    session.getPartition().getPath(), commitFutures.size());
            commitFutures.values().forEach(f -> f.completeExceptionally(partitionIsClosedException()));
            commitFutures.clear();
        } finally {
            commitFuturesLock.unlock();
        }
    }
}
