package tech.ydb.topic.write.impl.multi;

import java.time.Duration;
import java.util.Arrays;
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
import tech.ydb.topic.write.impl.multi.MultiWriterTestSupport.ControlledWriter;

public class MultiWriterTopologyRefreshRaceTest {
    @Test
    public void registeredUnsentMessageReroutesAfterNeighborRefreshBeforeNewerMessage() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setRoutingMode(RoutingMode.BOUND).setMaxWriters(2).setMaxConcurrentInits(1).build());
        f.partitions = Arrays.asList(bound(0, true, -1, 0x40), bound(9, true, 0x40, 0x80),
                bound(8, true, 0x80, -1));
        MultiWriterImpl writer = f.create();
        CompletableFuture<InitResult> initialized = writer.init();
        f.runUntilIdle();
        Assert.assertTrue(initialized.isDone());
        initialized.join();
        try {
            writer.send(MultiMessage.of(new byte[] { 1 }, "ab")); // 62be... routes to partition 9.
            f.runUntilIdle();
            ControlledWriter neighbor = f.active(9);
            f.onCreate = lower -> {
                if (Long.valueOf(8).equals(lower.config.getPartitionId())
                        || Long.valueOf(0).equals(lower.config.getPartitionId())) {
                    lower.automaticInit = false;
                }
            };
            writer.send(MultiMessage.of(new byte[] { 2 }, "ключ")); // 8cfe... routes to partition 8.
            f.runUntilIdle();
            ControlledWriter initializing = f.active(8);
            CompletableFuture<MultiWriteResult> earlier = writer.send(MultiMessage.of(new byte[] { 3 }, "a"));
            f.runUntilIdle();
            Assert.assertFalse(f.writers.stream().anyMatch(lower -> !lower.stopping
                    && Long.valueOf(0).equals(lower.config.getPartitionId())));

            // A refresh for partition 9 also discovers that partition 0 split, without an Entry for 0.
            f.partitions = Arrays.asList(bound(0, false, -1, 0x40, 1, 2), bound(9, false, 0x40, 0x80, 10, 11),
                    bound(8, true, 0x80, -1), child(1, -1, 0x20, 0), child(2, 0x20, 0x40, 0),
                    child(10, 0x40, 0x60, 9), child(11, 0x60, 0x80, 9));
            neighbor.fail(Status.of(StatusCode.OVERLOADED));
            f.runUntilIdle();
            f.passTime(Duration.ofMillis(100));
            Assert.assertFalse(earlier.isDone());
            initializing.initialized.complete(new InitResult(0));
            f.runUntilIdle();
            long inactiveWriters = f.writers.stream().filter(lower -> !lower.stopping
                    && Long.valueOf(0).equals(lower.config.getPartitionId())).count();
            Assert.assertEquals("Known inactive parent must never get a new pinned sending writer", 0,
                    inactiveWriters);
            CompletableFuture<MultiWriteResult> newer = writer.send(MultiMessage.of(new byte[] { 4 }, "a"));
            f.runUntilIdle();
            ControlledWriter activeChild = f.active(1);
            Assert.assertEquals(2, activeChild.sent.size());
            Assert.assertEquals(Long.valueOf(3), activeChild.sent.get(0).message.getSeqNo());
            Assert.assertEquals(Long.valueOf(4), activeChild.sent.get(1).message.getSeqNo());
            activeChild.ack(4, 1);
            initializing.ack(2, 0);
            f.runUntilIdle();
            f.passTime(Duration.ofMillis(100));
            f.active(11).ack(1, 0);
            f.runUntilIdle();
            Assert.assertEquals(3, earlier.join().getSeqNo());
            Assert.assertEquals(4, newer.join().getSeqNo());
        } finally {
            writer.shutdown();
            f.runUntilIdle();
            f.passTime(f.settings.getCloseTimeout());
        }
    }

    private static PartitionInfo bound(long id, boolean active, int from, int to, long... children) {
        YdbTopic.DescribeTopicResult.PartitionInfo.Builder info =
                YdbTopic.DescribeTopicResult.PartitionInfo.newBuilder()
                        .setPartitionId(id).setActive(active).setKeyRange(range(from, to));
        for (long child : children) {
            info.addChildPartitionIds(child);
        }
        return new PartitionInfo(info.build());
    }

    private static PartitionInfo child(long id, int from, int to, long parent) {
        return new PartitionInfo(YdbTopic.DescribeTopicResult.PartitionInfo.newBuilder().setPartitionId(id)
                .setActive(true).setKeyRange(range(from, to)).addParentPartitionIds(parent).build());
    }

    private static YdbTopic.PartitionKeyRange range(int from, int to) {
        YdbTopic.PartitionKeyRange.Builder result = YdbTopic.PartitionKeyRange.newBuilder();
        if (from >= 0) {
            result.setFromBound(ByteString.copyFrom(new byte[] { (byte) from, 0, 0, 0, 0, 0, 0, 0 }));
        }
        if (to >= 0) {
            result.setToBound(ByteString.copyFrom(new byte[] { (byte) to, 0, 0, 0, 0, 0, 0, 0 }));
        }
        return result.build();
    }
}
