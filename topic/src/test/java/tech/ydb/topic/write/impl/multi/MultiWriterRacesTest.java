package tech.ydb.topic.write.impl.multi;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.settings.MultiWriterSettings;
import tech.ydb.topic.settings.MultiWriterSettings.RoutingMode;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.MultiMessage;
import tech.ydb.topic.write.MultiWriteResult;
import tech.ydb.topic.write.WriteAck;
import tech.ydb.topic.write.impl.multi.MultiWriterTestSupport.ControlledWriter;

public class MultiWriterRacesTest {
    @Test
    public void queuedFlushStillCompletesWhenEarlierAckStartsShutdown() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        writer.send(message("a", 1));
        f.runUntilIdle();
        f.active(0).ack(1, 0);
        CompletableFuture<Void> flush = writer.flush();
        CompletableFuture<Void> closed = writer.shutdown();
        f.runUntilIdle();
        Assert.assertTrue(flush.isDone());
        Assert.assertTrue(closed.isDone());
        flush.join();
        closed.join();
    }

    @Test
    public void admissionQueuedBehindTopologyUpdateReroutesBeforeSubmit() throws Exception {
        MultiWriterTestSupport f = MultiWriterRecoveryTest.support();
        MultiWriterImpl writer = initialized(f);
        writer.send(message("a", 1));
        f.runUntilIdle();
        CompletableFuture<List<PartitionInfo>> describe = new CompletableFuture<>();
        f.nextDescribe = describe;
        f.active(0).fail(Status.of(StatusCode.OVERLOADED));
        f.runUntilIdle();
        f.partitions = MultiWriterRecoveryTest.split();
        describe.complete(f.partitions); // The topology event is queued; admission still sees the old parent.
        CompletableFuture<MultiWriteResult> newlyAdmitted = writer.send(message("a", 2));
        MultiWriterRecoveryTest.refresh(f);
        ControlledWriter child = f.active(1);
        Assert.assertEquals(2, child.sent.size());
        Assert.assertEquals(Long.valueOf(1), child.sent.get(0).message.getSeqNo());
        Assert.assertEquals(Long.valueOf(2), child.sent.get(1).message.getSeqNo());
        child.ack(2, 1);
        f.runUntilIdle();
        Assert.assertTrue(newlyAdmitted.isDone());
        Assert.assertEquals(Long.valueOf(1), newlyAdmitted.join().getPartitionId());
        shutdown(writer, f);
    }

    @Test
    public void recoveryProbeEvictsIdleOccupantFromFullPoolWithoutProducerOverlap() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setRoutingMode(RoutingMode.BOUND).setMaxWriters(2).setMaxConcurrentInits(2).build());
        f.partitions = Arrays.asList(bound(0, true, -1, 0x40), bound(9, true, 0x40, 0x80),
                bound(8, true, 0x80, -1));
        f.onCreate = created -> Assert.assertTrue("Probe and sender of the same producer must not overlap",
                f.writers.stream().noneMatch(old -> old.config.getProducerId().equals(created.config.getProducerId())
                        && !old.stopped.isDone()));
        MultiWriterImpl writer = initialized(f);
        writer.send(message("ab", 1)); // 62be... belongs to idle partition 9.
        f.runUntilIdle();
        ControlledWriter idle = f.active(9);
        idle.ack(1, 0);
        f.runUntilIdle();
        CompletableFuture<MultiWriteResult> recovery = writer.send(message("a", 2)); // 0717... parent 0.
        f.runUntilIdle();
        ControlledWriter parent = f.active(0);
        CompletableFuture<MultiWriteResult> independent = writer.send(message("ключ", 3)); // 8cfe... partition 8.
        f.runUntilIdle();
        Assert.assertEquals(2, f.writers.stream().filter(lower -> !lower.stopping).count());
        f.partitions = Arrays.asList(bound(0, false, -1, 0x40, 1, 2), bound(9, true, 0x40, 0x80),
                bound(8, true, 0x80, -1), child(1, -1, 0x20), child(2, 0x20, 0x40));
        parent.fail(Status.of(StatusCode.OVERLOADED));
        MultiWriterRecoveryTest.refresh(f);
        Assert.assertTrue("Idle writer must make room for history probe", idle.stopping);
        Assert.assertTrue(f.writers.stream().anyMatch(lower -> lower.config.getPartitionId() == null
                && "owner-0".equals(lower.config.getProducerId())));
        f.active(1).ack(2, 0);
        f.active(8).ack(3, 0);
        f.runUntilIdle();
        Assert.assertTrue(recovery.isDone());
        Assert.assertTrue(independent.isDone());
        recovery.join();
        independent.join();
        shutdown(writer, f);
    }

    @Test
    public void rejectedTopicCallResolvesAcceptedMessagesAndShutdownWithinDeadline() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        f.topicCallExecutor.reject = true;
        CompletableFuture<MultiWriteResult> pending = writer.send(message("a", 1));
        f.runUntilIdle();
        Assert.assertTrue(pending.isCompletedExceptionally());
        CompletableFuture<Void> closed = writer.shutdown();
        f.passTime(f.settings.getCloseTimeout());
        Assert.assertTrue(closed.isCompletedExceptionally());
        Assert.assertTrue(f.writers.stream().allMatch(lower -> lower.stopping));
    }

    @Test
    public void rejectedSchedulerFailsInitializationWithoutHanging() {
        MultiWriterTestSupport f = support();
        Mockito.doThrow(new RejectedExecutionException("Injected scheduler failure")).when(f.scheduler)
                .scheduleAtFixedRate(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.anyLong(),
                        Mockito.any(TimeUnit.class));
        MultiWriterImpl writer = f.create();
        CompletableFuture<InitResult> init = writer.init();
        f.runUntilIdle();
        Assert.assertTrue(init.isCompletedExceptionally());
        CompletableFuture<Void> closed = writer.shutdown();
        f.runUntilIdle();
        Assert.assertTrue(closed.isCompletedExceptionally());
    }

    @Test
    public void rejectedCompletionExecutorStillResolvesMessageAndControlFutures() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> pending = writer.send(message("a", 1));
        f.runUntilIdle();
        f.completion.reject = true;
        CompletableFuture<Void> flush = writer.flush();
        f.active(0).ack(1, 0);
        f.runUntilIdle();
        Assert.assertTrue(pending.isDone());
        Assert.assertTrue(flush.isDone());
        pending.join();
        flush.join();
        shutdown(writer, f);
    }

    @Test
    public void twoMessageCallbacksCanJoinShutdownWithOwnedRuntime() throws Exception {
        callbacksJoinShutdown(false);
    }

    @Test
    public void twoFlushCallbacksCanJoinShutdownWithOwnedRuntime() throws Exception {
        callbacksJoinShutdown(true);
    }

    private static void callbacksJoinShutdown(boolean controls) throws Exception {
        MultiWriterSettings settings = MultiWriterTestSupport.settings().setCloseTimeout(Duration.ofSeconds(2)).build();
        MultiWriterRuntime runtime = new MultiWriterRuntime(settings);
        LinkedBlockingQueue<Ack> sends = new LinkedBlockingQueue<>();
        MultiWriterImpl writer = new MultiWriterImpl(settings, new CodecRegistry().getCodec(settings.getCodec()),
                config -> new CallbackWriter(sends),
                () -> CompletableFuture.completedFuture(Collections.singletonList(MultiWriterTestSupport.partition(0))),
                runtime);
        try {
            writer.init().get(5, TimeUnit.SECONDS);
            CompletableFuture<MultiWriteResult> first = writer.send(message("a", 1));
            CompletableFuture<MultiWriteResult> second = writer.send(message("a", 2));
            Ack firstAck = sends.poll(5, TimeUnit.SECONDS);
            Ack secondAck = sends.poll(5, TimeUnit.SECONDS);
            Assert.assertNotNull(firstAck);
            Assert.assertNotNull(secondAck);
            CountDownLatch entered = new CountDownLatch(2);
            Runnable join = () -> {
                entered.countDown();
                try {
                    if (!entered.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Both callbacks must execute concurrently");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(error);
                }
                writer.shutdown().join();
            };
            CompletableFuture<Void> firstCallback = controls ? writer.flush().thenRun(join) : first.thenRun(join);
            CompletableFuture<Void> secondCallback = controls ? writer.flush().thenRun(join) : second.thenRun(join);
            firstAck.confirm();
            secondAck.confirm();
            CompletableFuture.allOf(firstCallback, secondCallback).get(5, TimeUnit.SECONDS);
            writer.shutdown().get(5, TimeUnit.SECONDS);
        } finally {
            writer.shutdown().get(5, TimeUnit.SECONDS);
            Assert.assertTrue(((ExecutorService) runtime.coordinator).awaitTermination(5, TimeUnit.SECONDS));
            Assert.assertTrue(((ExecutorService) runtime.topicCallExecutor).awaitTermination(5, TimeUnit.SECONDS));
            Assert.assertTrue(((ExecutorService) runtime.completion).awaitTermination(5, TimeUnit.SECONDS));
            Assert.assertTrue(((ExecutorService) runtime.controlCompletion).awaitTermination(5, TimeUnit.SECONDS));
            Assert.assertTrue(runtime.scheduler.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static MultiWriterTestSupport support() {
        return new MultiWriterTestSupport(MultiWriterTestSupport.settings().build());
    }

    private static MultiWriterImpl initialized(MultiWriterTestSupport support) {
        MultiWriterImpl writer = support.create();
        CompletableFuture<InitResult> init = writer.init();
        support.runUntilIdle();
        Assert.assertTrue(init.isDone());
        init.join();
        return writer;
    }

    private static MultiMessage message(String key, int value) {
        return MultiMessage.of(new byte[] { (byte) value }, key);
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

    private static PartitionInfo child(long id, int from, int to) {
        return new PartitionInfo(YdbTopic.DescribeTopicResult.PartitionInfo.newBuilder().setPartitionId(id)
                .setActive(true).setKeyRange(range(from, to)).addParentPartitionIds(0).build());
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

    private static void shutdown(MultiWriterImpl writer, MultiWriterTestSupport support) {
        CompletableFuture<Void> closed = writer.shutdown();
        support.runUntilIdle();
        Assert.assertTrue(closed.isDone());
        closed.join();
    }

    private static final class CallbackWriter implements AsyncWriter {
        private final LinkedBlockingQueue<Ack> sends;

        CallbackWriter(LinkedBlockingQueue<Ack> sends) {
            this.sends = sends;
        }

        @Override
        public CompletableFuture<InitResult> init() {
            return CompletableFuture.completedFuture(new InitResult(0));
        }

        @Override
        public CompletableFuture<WriteAck> send(Message message) {
            return send(message, null);
        }

        @Override
        public CompletableFuture<WriteAck> send(Message message, SendSettings settings) {
            Ack ack = new Ack(message.getSeqNo());
            sends.add(ack);
            return ack.future;
        }

        @Override
        public CompletableFuture<Void> shutdown() {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class Ack {
        private final long sequence;
        private final CompletableFuture<WriteAck> future = new CompletableFuture<>();

        Ack(long sequence) {
            this.sequence = sequence;
        }

        void confirm() {
            future.complete(new WriteAck(sequence, WriteAck.State.WRITTEN, new WriteAck.Details(sequence), null));
        }
    }
}
