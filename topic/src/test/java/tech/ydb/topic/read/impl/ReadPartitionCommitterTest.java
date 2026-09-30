package tech.ydb.topic.read.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.PartitionSession;

public class ReadPartitionCommitterTest {
    @Test
    public void acknowledgementsCountBatchedAndRepeatedRanges() {
        ReadSession session = Mockito.mock(ReadSession.class);
        PartitionSession partition = new PartitionSession(1, 42, "/topic");
        Mockito.when(session.commitOffsets(Mockito.eq(partition), Mockito.anyList())).thenReturn(true);
        ReadPartitionCommitter committer = new ReadPartitionCommitter("partition", session, partition, 0);
        OffsetsRange first = OffsetsRange.of(0, 10);
        List<OffsetsRange> batch = Arrays.asList(first, OffsetsRange.of(20, 25));

        committer.commitRanges(batch);
        CompletableFuture<Void> future = committer.commit(first);
        Assert.assertSame(future, committer.commit(OffsetsRange.of(5, 10)));
        Assert.assertFalse(future.isDone());
        committer.updateCommittedOffset(10);
        Assert.assertEquals(25, committer.completePendingCommits());
        Assert.assertTrue(future.isDone());
        Assert.assertFalse(future.isCompletedExceptionally());
        Assert.assertEquals(0, committer.completePendingCommits());
        committer.updateCommittedOffset(25);
        Assert.assertEquals(5, committer.completePendingCommits());
        Mockito.verify(session).commitOffsets(partition, batch);
    }

    @Test
    public void rejectedBatchFailsPendingCommitsWithoutAcknowledgingThem() {
        ReadSession session = Mockito.mock(ReadSession.class);
        PartitionSession partition = new PartitionSession(1, 42, "/topic");
        Mockito.when(session.commitOffsets(Mockito.eq(partition), Mockito.anyList())).thenReturn(true, false);
        ReadPartitionCommitter committer = new ReadPartitionCommitter("partition", session, partition, 0);
        CompletableFuture<Void> future = committer.commit(OffsetsRange.of(0, 10));

        committer.commitRanges(Collections.singletonList(OffsetsRange.of(20, 25)));

        Assert.assertTrue(future.isCompletedExceptionally());
        committer.updateCommittedOffset(25);
        Assert.assertEquals(0, committer.completePendingCommits());
    }
}
