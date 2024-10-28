package tech.ydb.topic.read.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.TransactionMessageAccumulator;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * @author Nikolay Perfilov
 */
public class TransactionMessageAccumulatorImpl implements TransactionMessageAccumulator {
    private static final Logger logger = LoggerFactory.getLogger(TransactionMessageAccumulatorImpl.class);

    private final AsyncReader reader;
    private final Map<String, Map<PartitionSession, PartitionRanges>> rangesByTopic = new ConcurrentHashMap<>();

    private static class PartitionRanges {
        private final PartitionSession partitionSession;
        private final DisjointOffsetRangeSet ranges = new DisjointOffsetRangeSet();
        private final ReentrantLock rangesLock = new ReentrantLock();

        private PartitionRanges(PartitionSession partitionSession) {
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
                String errorMessage = "Error adding new offset range to DeferredCommitter for partition session " +
                        partitionSession.getId() + " (partition " + partitionSession.getPartitionId() + "): " +
                        exception.getMessage();
                logger.error(errorMessage);
                throw new RuntimeException(errorMessage, exception);
            }
        }

        private List<OffsetsRange> getOffsetsRanges() {
            rangesLock.lock();

            try {
                return ranges.getRangesAndClear();
            } finally {
                rangesLock.unlock();
            }
        }
    }

    TransactionMessageAccumulatorImpl(AsyncReader reader) {
        this.reader = reader;
    }

    @Override
    public void add(Message message) {
        MessageImpl messageImpl = (MessageImpl) message;
        PartitionRanges partitionRanges = rangesByTopic
                .computeIfAbsent(message.getPartitionSession().getPath(), path -> new ConcurrentHashMap<>())
                .computeIfAbsent(message.getPartitionSession(), PartitionRanges::new);
        partitionRanges.add(messageImpl.getOffsetsToCommit());
    }

    @Override
    public void add(DataReceivedEvent event) {
        DataReceivedEventImpl eventImpl = (DataReceivedEventImpl) event;
        PartitionRanges partitionRanges = rangesByTopic
                .computeIfAbsent(event.getPartitionSession().getPath(), path -> new ConcurrentHashMap<>())
                .computeIfAbsent(event.getPartitionSession(), PartitionRanges::new);
        partitionRanges.add(eventImpl.getOffsetsToCommit());
    }

    @Override
    public CompletableFuture<Status> updateOffsetsInTransaction(YdbTransaction transaction,
                                                                UpdateOffsetsInTransactionSettings settings) {
        Map<String, List<PartitionOffsets>> offsets = new HashMap<>();
        rangesByTopic.forEach((path, topicRanges) -> {
            offsets.put(path, topicRanges.entrySet().stream()
                    .map(partitionRange ->
                            new PartitionOffsets(partitionRange.getKey(), partitionRange.getValue().getOffsetsRanges()))
                    .collect(Collectors.toList()));
        });
        return reader.updateOffsetsInTransaction(transaction, offsets, settings);
    }

}
