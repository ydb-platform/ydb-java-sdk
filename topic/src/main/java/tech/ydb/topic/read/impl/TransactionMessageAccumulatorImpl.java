package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.PartitionOffsets;
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
    private final Map<String, Map<PartitionSessionImpl, DisjointOffsetRangeSet>> rangesByTopic;

    TransactionMessageAccumulatorImpl(AsyncReader reader) {
        this.reader = reader;
        this.rangesByTopic = new ConcurrentHashMap<>();
    }

    private RuntimeException wrapExceptionWithSession(PartitionSessionImpl session, RuntimeException ex) {
        String errorMessage = "[" + session.getFullId() + "] Error adding new offset range to " +
                "TransactionMessageAccumulator for " + session.getSessionId() + ": " + ex.getMessage();
        logger.error(errorMessage);
        return new RuntimeException(errorMessage, ex);
    }

    @Override
    public void add(Message message) {
        PartitionSessionImpl session = ((MessageImpl) message).getPartitionSessionImpl();

        DisjointOffsetRangeSet range = rangesByTopic
                .computeIfAbsent(message.getPartitionSession().getPath(), path -> new ConcurrentHashMap<>())
                .computeIfAbsent(session, s -> new DisjointOffsetRangeSet());
        try {
            range.add(message.getPartitionOffsets().getOffsets());
        } catch (RuntimeException exception) {
            throw wrapExceptionWithSession(session, exception);
        }

    }

    @Override
    public void add(DataReceivedEvent event) {
        PartitionSessionImpl session = ((DataReceivedEventImpl) event).getPartitionSessionImpl();

        DisjointOffsetRangeSet range = rangesByTopic
                .computeIfAbsent(event.getPartitionSession().getPath(), path -> new ConcurrentHashMap<>())
                .computeIfAbsent(session, s -> new DisjointOffsetRangeSet());
        try {
            range.add(event.getPartitionOffsets().getOffsets());
        } catch (RuntimeException exception) {
            throw wrapExceptionWithSession(session, exception);
        }
    }

    @Override
    public CompletableFuture<Status> updateOffsetsInTransaction(YdbTransaction transaction,
                                                                UpdateOffsetsInTransactionSettings settings) {
        Map<String, List<PartitionOffsets>> offsets = new HashMap<>();
        rangesByTopic.forEach((path, topicRanges) -> {
            List<PartitionOffsets> topicOffsets = new ArrayList<>();
            topicRanges.forEach((session, range) -> {
                topicOffsets.add(new PartitionOffsets(session.getSessionId(), range.getRangesAndClear()));
            });

            offsets.put(path, topicOffsets);
        });
        return reader.updateOffsetsInTransaction(transaction, offsets, settings);
    }

}
