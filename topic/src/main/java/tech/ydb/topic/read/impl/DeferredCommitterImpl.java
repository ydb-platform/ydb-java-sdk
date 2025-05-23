package tech.ydb.topic.read.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.DeferredCommitter;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;

/**
 * @author Nikolay Perfilov
 */
public class DeferredCommitterImpl implements DeferredCommitter {
    private static final Logger logger = LoggerFactory.getLogger(DeferredCommitterImpl.class);

    private final Map<PartitionSessionImpl, PartitionRanges> rangesByPartition = new ConcurrentHashMap<>();

    private static class PartitionRanges {
        private final PartitionSessionImpl partitionSession;
        private final DisjointOffsetRangeSet ranges = new DisjointOffsetRangeSet();
        private final ReentrantLock rangesLock = new ReentrantLock();

        private PartitionRanges(PartitionSessionImpl partitionSession) {
            this.partitionSession = partitionSession;
        }

        private void add(OffsetsRange offsetRange) {
            try {
                rangesLock.lock();

                try {
                    ranges.add(offsetRange);
                } finally {
                    rangesLock.unlock();
                }
            } catch (RuntimeException exception) {
                String errorMessage = "[" + partitionSession.getFullId() + "] Error adding new offset range to " +
                        "DeferredCommitter for partition session " + partitionSession.getId() + " (partition " +
                        partitionSession.getPartitionId() + "): " + exception.getMessage();
                logger.error(errorMessage);
                throw new RuntimeException(errorMessage, exception);
            }
        }

        private void commit() {
            List<OffsetsRange> rangesToCommit;
            rangesLock.lock();
            try {
                rangesToCommit = ranges.getRangesAndClear();
            } finally {
                rangesLock.unlock();
            }
            partitionSession.commitOffsetRanges(rangesToCommit);
        }
    }

    @Override
    public void add(Message message) {
        MessageImpl messageImpl = (MessageImpl) message;
        PartitionRanges partitionRanges = rangesByPartition
                .computeIfAbsent(messageImpl.getPartitionSessionImpl(), PartitionRanges::new);
        partitionRanges.add(messageImpl.getOffsetsToCommit());
    }

    @Override
    public void add(DataReceivedEvent event) {
        DataReceivedEventImpl eventImpl = (DataReceivedEventImpl) event;
        PartitionRanges partitionRanges = rangesByPartition
                .computeIfAbsent(eventImpl.getPartitionSessionImpl(), PartitionRanges::new);
        partitionRanges.add(eventImpl.getOffsetsToCommit());
    }

    @Override
    public void commit() {
        rangesByPartition.forEach((session, partitionRanges) -> {
            partitionRanges.commit();
        });
    }
}
