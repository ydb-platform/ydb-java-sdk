package tech.ydb.topic.read.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.ReadResponse;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.utils.HideLoggers;
import tech.ydb.topic.utils.HideLoggersRule;

/**
 * @author Aleksandr Gorshenin
 */
public class BufferManagerTest {
    @Rule
    public final HideLoggersRule hideLogger = new HideLoggersRule();

    private static final class Resource implements Consumer<Long> {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong total = new AtomicLong(0);

        @Override
        public void accept(Long value) {
            count.incrementAndGet();
            total.addAndGet(value);
        }

        public void assertCalls(int expectedCount, long expectedTotal) {
            Assert.assertEquals("Unexpected resource requests count", expectedCount, count.get());
            Assert.assertEquals("Unexpected resource requests total value", expectedTotal, total.get());
        }
    }

    private static ReadResponse.MessageData msg(long offset, int size) {
        return ReadResponse.MessageData.newBuilder()
                .setOffset(offset)
                .setData(ByteString.copyFrom(new byte[size]))
                .build();
    }

    private static ReadResponse.Batch batch(long startOffset, int... sizes) {
        ReadResponse.Batch.Builder builder = ReadResponse.Batch.newBuilder();
        long offset = startOffset;
        for (int size : sizes) {
            builder.addMessageData(msg(offset++, size));
        }
        return builder.build();
    }

    private static ReadResponse.PartitionData partition(long partition, ReadResponse.Batch... batches) {
        ReadResponse.PartitionData.Builder builder = ReadResponse.PartitionData.newBuilder()
                .setPartitionSessionId(partition);
        for (ReadResponse.Batch batch : batches) {
            builder.addBatches(batch);
        }
        return builder.build();
    }

    @Test
    public void simpleUseTest() {
        Resource r = new Resource();
        BufferManager bm = new BufferManager("trace-1", 1234, r);

        r.assertCalls(0, 0);

        bm.init("s1");
        r.assertCalls(1, 1234);

        bm.allocate(500, Arrays.asList( // 500 = 100 + 100 + 250 + 40 + 5 + 5
                partition(1, batch(0, 100, 100), batch(2, 250), batch(6, 40, 5, 5), batch(20)), // empty batch
                partition(2)  // empty partition
        ));
        r.assertCalls(1, 1234);

        // release unknown partitions doesn't affect anything
        bm.releasePartition(3L);
        bm.releaseRange(4L, OffsetsRange.of(0, 100));
        r.assertCalls(1, 1234);

        // partial release more than 10% of buffer size - a new request
        bm.releaseRange(1L, OffsetsRange.of(1, 3));
        r.assertCalls(2, 1234 + 350);

        // double release - no more request
        bm.releaseRange(1L, OffsetsRange.of(1, 3));
        r.assertCalls(2, 1234 + 350);

        // partitial release less than 10% of buffer size - no additional request
        bm.releaseRange(1L, OffsetsRange.of(6, 10));
        r.assertCalls(2, 1234 + 350);

        bm.releasePartition(1L);
        r.assertCalls(3, 1234 + 500);

        // double release - no more request
        bm.releasePartition(1L);
        r.assertCalls(3, 1234 + 500);
    }

    @Test
    @HideLoggers(BufferManager.class)
    public void incorrectAllocateTest() {
        Resource r = new Resource();
        BufferManager bm = new BufferManager("trace-2", 20000, r);
        r.assertCalls(0, 0);

        bm.allocate(5000, Collections.emptyList());
        r.assertCalls(1, 5000);

        bm.allocate(4000, Arrays.asList(partition(1), partition(2)));
        r.assertCalls(2, 9000);
    }

    @Test
    public void zeroAllocateTest() {
        Resource r = new Resource();
        BufferManager bm = new BufferManager("trace-3", 20000, r);
        r.assertCalls(0, 0);

        // zero allocation is allowed
        bm.allocate(0, Arrays.asList(partition(1, batch(0, 100, 100), batch(2, 250), batch(6, 40, 5, 5), batch(20))));
        r.assertCalls(0, 0);

        // releases have no effect
        bm.releaseRange(1L, OffsetsRange.of(1, 3));
        bm.releasePartition(1L);
        r.assertCalls(0, 0);
    }

    @Test
    public void concurrentReleaseTest() {
        AtomicLong requested = new AtomicLong(0);
        BufferManager bm = new BufferManager("trace-4", 1 /* no threshold */, requested::addAndGet);

        Thread t = new Thread(() -> {
            for (int idx = 0; idx < 1000; idx += 1) {
                bm.allocate(100, Arrays.asList(partition(1, batch(idx, 100))));
            }
        });
        t.start();

        while (t.isAlive()) {
            bm.releasePartition(1L);
        }

        bm.releasePartition(1L);
        Assert.assertEquals(100000L, requested.get());
    }

    @Test
    public void intOverflowTest() {
        AtomicLong requested = new AtomicLong(0);
        BufferManager bm = new BufferManager("trace-5", 1000, requested::addAndGet);

        bm.allocate(100000, Arrays.asList(
                partition(1, batch(1, 10000, 10000, 10000)),
                partition(2, batch(1, 20000, 30000, 10000)),
                partition(3, batch(1, 40000, 40000, 40000))
        ));

        bm.releasePartition(1L);
        bm.releasePartition(2L);
        bm.releasePartition(3L);
        Assert.assertEquals(100000, requested.get());
    }

    @Test
    public void zeroMessagesTest() {
        AtomicLong requested = new AtomicLong(0);
        BufferManager bm = new BufferManager("trace-6", 1000, requested::addAndGet);

        bm.allocate(100000, Arrays.asList(partition(1, batch(1, 0, 0, 0))));

        bm.releasePartition(1L);
        Assert.assertEquals(100000, requested.get());
    }

    @Test
    public void skippedOffsetsInBatchTest() {
        AtomicLong requested = new AtomicLong(0);
        BufferManager bm = new BufferManager("skipped-offsets", 100, requested::addAndGet);

        ReadResponse.Batch batch = ReadResponse.Batch.newBuilder()
                .addAllMessageData(Arrays.asList(msg(0, 100), msg(1, 100), msg(100, 100), msg(101, 100)))
                .build();

        bm.allocate(400, Arrays.asList(partition(1, batch)));

        bm.releaseRange(1L, OffsetsRange.of(10, 100));
        Assert.assertEquals(0, requested.get());

        bm.releaseRange(1L, OffsetsRange.of(0, 50));
        Assert.assertEquals(200, requested.get());

        bm.releasePartition(1L);
        Assert.assertEquals(400, requested.get());
    }
}
