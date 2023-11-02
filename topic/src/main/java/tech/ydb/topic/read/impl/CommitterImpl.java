package tech.ydb.topic.read.impl;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.read.OffsetsRange;

/**
 * @author Nikolay Perfilov
 */
public class CommitterImpl {
    private static final Logger logger = LoggerFactory.getLogger(CommitterImpl.class);
    private final PartitionSessionImpl partitionSession;
    private final int messageCount;
    private final OffsetsRange offsetsToCommit;

    public CommitterImpl(PartitionSessionImpl partitionSession, int messageCount, OffsetsRange offsetsToCommit) {
        this.partitionSession = partitionSession;
        this.messageCount = messageCount;
        this.offsetsToCommit = offsetsToCommit;
    }


    public CompletableFuture<Void> commit() {
        return commitImpl(true);
    }

    public CompletableFuture<Void> commitImpl(boolean fromCommitter) {
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] partition session {} (partition {}): committing {} message(s), offsets" +
                            " [{},{})" + (fromCommitter ? " from Committer" : ""), partitionSession.getPath(),
                    partitionSession.getId(), partitionSession.getPartitionId(), messageCount,
                    offsetsToCommit.getStart(), offsetsToCommit.getEnd());
        }
        return partitionSession.commitOffsetRange(offsetsToCommit);
    }
}
