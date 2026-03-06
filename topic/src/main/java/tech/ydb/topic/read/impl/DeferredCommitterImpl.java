package tech.ydb.topic.read.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.read.DeferredCommitter;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.events.DataReceivedEventImpl;

/**
 * @author Nikolay Perfilov
 */
public class DeferredCommitterImpl implements DeferredCommitter {
    private static final Logger logger = LoggerFactory.getLogger(DeferredCommitterImpl.class);

    private final Map<PartitionSessionImpl, DisjointOffsetRangeSet> rangesBySession = new ConcurrentHashMap<>();

    private RuntimeException wrapExceptionWithSession(PartitionSessionImpl session, RuntimeException ex) {
        String errorMessage = "[" + session.getFullId() + "] Error adding new offset range to " +
                "DeferredCommitter for " + session.getSessionId() + ": " + ex.getMessage();
        logger.error(errorMessage);
        return new RuntimeException(errorMessage, ex);
    }

    @Override
    public void add(Message message) {
        PartitionSessionImpl session = ((MessageImpl) message).getPartitionSessionImpl();
        DisjointOffsetRangeSet range = rangesBySession.computeIfAbsent(session, s -> new DisjointOffsetRangeSet());
        try {
            range.add(message.getPartitionOffsets().getOffsets());
        } catch (RuntimeException exception) {
            throw wrapExceptionWithSession(session, exception);
        }
    }

    @Override
    public void add(DataReceivedEvent event) {
        PartitionSessionImpl session = ((DataReceivedEventImpl) event).getPartitionSessionImpl();
        DisjointOffsetRangeSet range = rangesBySession.computeIfAbsent(session, s -> new DisjointOffsetRangeSet());
        try {
            range.add(event.getPartitionOffsets().getOffsets());
        } catch (RuntimeException exception) {
            throw wrapExceptionWithSession(session, exception);
        }
    }

    @Override
    public void commit() {
        rangesBySession.forEach((session, ranges) -> {
            session.commit(ranges.getRangesAndClear());
        });
    }
}
