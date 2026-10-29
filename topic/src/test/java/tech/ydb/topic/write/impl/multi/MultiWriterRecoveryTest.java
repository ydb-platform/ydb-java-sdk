package tech.ydb.topic.write.impl.multi;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.settings.MultiWriterSettings.RoutingMode;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.MultiMessage;
import tech.ydb.topic.write.MultiWriteResult;
import tech.ydb.topic.write.MultiWriteResult.ConfirmationSource;
import tech.ydb.topic.write.impl.multi.MultiWriterTestSupport.ControlledWriter;

/** Client algorithm tests; final server history cut is verified separately against live YDB. */
public class MultiWriterRecoveryTest {
    @Test
    public void lostAckUsesInactiveProducerHistoryAndResendsTailBeforeNewMessage() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> persisted = writer.send(message(1));
        CompletableFuture<MultiWriteResult> unsaved = writer.send(message(2));
        f.runUntilIdle();
        ControlledWriter parent = f.active(0);
        f.histories.put("owner-0", 1L);
        f.partitions = split();
        parent.fail(Status.of(StatusCode.OVERLOADED));
        CompletableFuture<MultiWriteResult> newer = writer.send(message(3));
        refresh(f);
        Assert.assertTrue(persisted.isDone());
        Assert.assertEquals(ConfirmationSource.PRODUCER_HISTORY, persisted.join().getSource());
        Assert.assertNull(persisted.join().getPartitionId());
        Assert.assertNull(persisted.join().getOffset());
        ControlledWriter child = f.active(1);
        Assert.assertEquals(2, child.sent.size());
        Assert.assertEquals(Long.valueOf(2), child.sent.get(0).message.getSeqNo());
        Assert.assertEquals(Long.valueOf(3), child.sent.get(1).message.getSeqNo());
        Assert.assertArrayEquals(parent.sent.get(1).message.getData(), child.sent.get(0).message.getData());
        Assert.assertEquals(parent.sent.get(1).message.getCreateTimestamp(),
                child.sent.get(0).message.getCreateTimestamp());
        Assert.assertTrue(f.writers.stream().anyMatch(probe -> "owner-0".equals(probe.config.getProducerId())
                && probe.config.getPartitionId() == null && probe.stopping));
        child.ack(3, 1);
        f.runUntilIdle();
        Assert.assertEquals(2, unsaved.join().getSeqNo());
        Assert.assertEquals(3, newer.join().getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void acceptsLateSuccessfulAckWhileOldWriterIsClosing() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        ControlledWriter parent = f.active(0);
        parent.automaticShutdown = false;
        f.partitions = split();
        parent.diagnostic(Status.of(StatusCode.OVERLOADED));
        f.runUntilIdle();
        Assert.assertTrue(parent.stopping);
        parent.ack(1, 29);
        f.runUntilIdle();
        Assert.assertTrue(pending.isDone());
        Assert.assertEquals(ConfirmationSource.ACK, pending.join().getSource());
        Assert.assertEquals(Long.valueOf(29), pending.join().getOffset());
        parent.finishShutdown();
        refresh(f);
        Assert.assertEquals(1, parent.sent.size());
        shutdown(writer, f);
    }

    @Test
    public void incompleteSplitSnapshotWaitsForChildren() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        ControlledWriter parent = f.active(0);
        f.partitions = Collections.singletonList(split().get(0));
        parent.fail(Status.of(StatusCode.OVERLOADED));
        f.runUntilIdle();
        Assert.assertFalse(pending.isDone());
        Assert.assertFalse(f.writers.stream().anyMatch(lower -> "owner-1".equals(lower.config.getProducerId())));
        f.partitions = split();
        refresh(f);
        f.active(1).ack(1, 0);
        f.runUntilIdle();
        Assert.assertTrue(pending.isDone());
        Assert.assertEquals(ConfirmationSource.ACK, pending.join().getSource());
        shutdown(writer, f);
    }

    @Test
    public void overloadedActivePartitionDoesNotInventChildrenOrReplay() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        int before = f.writers.size();
        f.active(0).fail(Status.of(StatusCode.OVERLOADED));
        f.runUntilIdle();
        Assert.assertFalse(pending.isDone());
        f.passTime(Duration.ofSeconds(1));
        Assert.assertTrue(pending.isCompletedExceptionally());
        Assert.assertEquals(before, f.writers.size());
        writer.shutdown();
        f.runUntilIdle();
    }

    @Test
    public void repeatedSplitSignalsDoNotDuplicateHistoryProbesOrLogicalCompletion() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        ControlledWriter parent = f.active(0);
        f.partitions = split();
        parent.fail(Status.of(StatusCode.OVERLOADED));
        parent.diagnostic(Status.of(StatusCode.OVERLOADED));
        parent.diagnostic(Status.of(StatusCode.OVERLOADED));
        refresh(f);
        Assert.assertEquals(1, f.writers.stream().filter(probe -> "owner-0".equals(probe.config.getProducerId())
                && probe.config.getPartitionId() == null).count());
        ControlledWriter child = f.active(1);
        Assert.assertEquals(1, child.sent.size());
        parent.diagnostic(Status.of(StatusCode.OVERLOADED));
        f.runUntilIdle();
        Assert.assertEquals(1, child.sent.size());
        child.ack(1, 0);
        f.runUntilIdle();
        Assert.assertTrue(pending.isDone());
        pending.join();
        shutdown(writer, f);
    }

    @Test
    public void cascadePreservesOriginalRecoveryDeadline() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        f.partitions = split();
        f.active(0).fail(Status.of(StatusCode.OVERLOADED));
        refresh(f);
        ControlledWriter child = f.active(1);
        Assert.assertEquals(Long.valueOf(1), child.sent.get(0).message.getSeqNo());
        f.passTime(Duration.ofMillis(500));
        child.fail(Status.of(StatusCode.OVERLOADED));
        f.runUntilIdle();
        Assert.assertFalse(pending.isDone());
        f.passTime(Duration.ofMillis(400));
        Assert.assertTrue("Second recovery must use the original start time", pending.isCompletedExceptionally());
        writer.shutdown();
        f.runUntilIdle();
    }

    @Test
    public void bootstrapIncludesInactiveAncestorHistoryWithoutPinningIt() {
        MultiWriterTestSupport f = support();
        f.partitions = split();
        f.histories.put("owner-0", 19L);
        f.histories.put("owner-1", 7L);
        f.histories.put("owner-2", 11L);
        MultiWriterImpl writer = f.create();
        CompletableFuture<InitResult> init = writer.init();
        f.runUntilIdle();
        Assert.assertTrue(init.isDone());
        Assert.assertEquals(19, init.join().getSeqNo());
        Assert.assertEquals(3, f.writers.size());
        ControlledWriter parent = f.writers.stream().filter(lower -> "owner-0".equals(lower.config.getProducerId()))
                .findFirst().get();
        Assert.assertNull(parent.config.getPartitionId());
        Assert.assertTrue(parent.stopping);
        shutdown(writer, f);
    }

    static MultiWriterTestSupport support() {
        MultiWriterTestSupport support = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setRoutingMode(RoutingMode.BOUND).setRecoveryTimeout(Duration.ofSeconds(1)).build());
        support.partitions = Collections.singletonList(bound(0, true, null, null, new long[0], new long[0]));
        return support;
    }

    private static MultiWriterImpl initialized(MultiWriterTestSupport support) {
        MultiWriterImpl writer = support.create();
        CompletableFuture<InitResult> init = writer.init();
        support.runUntilIdle();
        Assert.assertTrue(init.isDone());
        init.join();
        return writer;
    }

    private static MultiMessage message(int value) {
        // Go's Murmur64 golden vector for "a" is 071717d2d36b6b11, in child 1.
        return MultiMessage.of(new byte[] { (byte) value }, "a");
    }

    static void refresh(MultiWriterTestSupport support) {
        support.runUntilIdle();
        support.passTime(Duration.ofMillis(100));
    }

    static List<PartitionInfo> split() {
        byte[] middle = { (byte) 0x80, 0, 0, 0, 0, 0, 0, 0 };
        return Arrays.asList(
                bound(0, false, null, null, new long[0], new long[] { 1, 2 }),
                bound(1, true, null, middle, new long[] { 0 }, new long[0]),
                bound(2, true, middle, null, new long[] { 0 }, new long[0]));
    }

    private static PartitionInfo bound(long id, boolean active, byte[] from, byte[] to,
            long[] parents, long[] children) {
        YdbTopic.PartitionKeyRange.Builder range = YdbTopic.PartitionKeyRange.newBuilder();
        if (from != null) {
            range.setFromBound(ByteString.copyFrom(from));
        }
        if (to != null) {
            range.setToBound(ByteString.copyFrom(to));
        }
        YdbTopic.DescribeTopicResult.PartitionInfo.Builder info = YdbTopic.DescribeTopicResult.PartitionInfo.newBuilder()
                .setPartitionId(id).setActive(active).setKeyRange(range);
        for (long parent : parents) {
            info.addParentPartitionIds(parent);
        }
        for (long child : children) {
            info.addChildPartitionIds(child);
        }
        return new PartitionInfo(info.build());
    }

    private static void shutdown(MultiWriterImpl writer, MultiWriterTestSupport support) {
        CompletableFuture<Void> closed = writer.shutdown();
        support.runUntilIdle();
        Assert.assertTrue(closed.isDone());
        closed.join();
    }
}
