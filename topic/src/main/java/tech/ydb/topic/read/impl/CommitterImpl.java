package tech.ydb.topic.read.impl;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.description.OffsetsRange;

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
            logger.debug(
                    "[{}] Committing {} message(s), offsets [{},{}){}",
                    partitionSession.getFullId(),
                    messageCount,
                    offsetsToCommit.getStart(),
                    offsetsToCommit.getEnd(),
                    fromCommitter ? " from Committer" : ""
            );
        }
        return partitionSession.commitOffsetRange(offsetsToCommit);
    }
}
