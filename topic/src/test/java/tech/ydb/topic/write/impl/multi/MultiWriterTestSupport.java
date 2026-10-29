package tech.ydb.topic.write.impl.multi;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.Assert;
import org.mockito.Mockito;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.settings.MultiWriterSettings;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/** Controlled executors, time, topology and public AsyncWriter dependencies for coordinator tests. */
final class MultiWriterTestSupport {
    final ManualExecutor coordinator = new ManualExecutor();
    final ManualExecutor topicCallExecutor = new ManualExecutor();
    final ManualExecutor completion = new ManualExecutor();
    final List<Timer> timers = new ArrayList<>();
    final List<ControlledWriter> writers = new ArrayList<>();
    final Map<String, Long> histories = new HashMap<>();
    final ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
    final MultiWriterSettings settings;
    List<PartitionInfo> partitions = Collections.singletonList(partition(0));
    Consumer<ControlledWriter> onCreate = writer -> { };
    CompletableFuture<List<PartitionInfo>> nextDescribe;
    int describes;
    long now;
    private final Codec codec;

    MultiWriterTestSupport(MultiWriterSettings settings) {
        this.settings = settings;
        this.codec = new CodecRegistry().getCodec(settings.getCodec());
        Mockito.when(scheduler.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any(TimeUnit.class)))
                .thenAnswer(call -> {
                    Runnable action = call.getArgument(0);
                    long delay = call.getArgument(1);
                    TimeUnit unit = call.getArgument(2);
                    Timer timer = new Timer(action, now + unit.toNanos(delay), 0);
                    timers.add(timer);
                    return timer;
                });
        Mockito.when(scheduler.scheduleAtFixedRate(Mockito.any(Runnable.class), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.any(TimeUnit.class))).thenAnswer(call -> {
                    Runnable action = call.getArgument(0);
                    long delay = call.getArgument(1);
                    long period = call.getArgument(2);
                    TimeUnit unit = call.getArgument(3);
                    Timer timer = new Timer(action, now + unit.toNanos(delay), unit.toNanos(period));
                    timers.add(timer);
                    return timer;
                });
    }

    MultiWriterImpl create() {
        MultiWriterRuntime runtime =
                new MultiWriterRuntime(coordinator, topicCallExecutor, completion, scheduler, () -> now);
        return new MultiWriterImpl(settings, codec, this::createWriter, this::describe, runtime);
    }

    TransactionalMultiWriterImpl createTransactional(YdbTransaction transaction) {
        MultiWriterRuntime runtime =
                new MultiWriterRuntime(coordinator, topicCallExecutor, completion, scheduler, () -> now);
        return new TransactionalMultiWriterImpl(settings, codec, this::createWriter, this::describe,
                runtime, transaction);
    }

    ControlledWriter createWriter(WriterSettings config) {
        ControlledWriter writer = new ControlledWriter(config);
        onCreate.accept(writer);
        writers.add(writer);
        return writer;
    }

    CompletableFuture<List<PartitionInfo>> describe() {
        describes++;
        if (nextDescribe != null) {
            CompletableFuture<List<PartitionInfo>> result = nextDescribe;
            nextDescribe = null;
            return result;
        }
        return CompletableFuture.completedFuture(new ArrayList<>(partitions));
    }

    void runUntilIdle() {
        for (int step = 0; step < 10000; step++) {
            boolean progressed = coordinator.runOne();
            progressed |= topicCallExecutor.runOne();
            progressed |= completion.runOne();
            List<Timer> due = new ArrayList<>();
            for (Timer timer : timers) {
                if (!timer.isDone() && timer.deadline <= now) {
                    due.add(timer);
                }
            }
            due.sort(Comparator.comparingLong(timer -> timer.deadline));
            for (Timer timer : due) {
                timer.run();
                progressed = true;
            }
            if (!progressed) {
                return;
            }
        }
        Assert.fail("Coordinator failed to become idle without advancing time");
    }

    void passTime(Duration time) {
        now += time.toNanos();
        runUntilIdle();
    }

    ControlledWriter active(long partition) {
        for (int index = writers.size() - 1; index >= 0; index--) {
            ControlledWriter writer = writers.get(index);
            if (!writer.stopping && Long.valueOf(partition).equals(writer.config.getPartitionId())) {
                return writer;
            }
        }
        throw new AssertionError("No active writer for " + partition);
    }

    static MultiWriterSettings.Builder settings() {
        return MultiWriterSettings.newBuilder().setTopicPath("/test/multi").setProducerPrefix("owner")
                .setMaxMessageBytes(1024).setBufferBytes(65536).setWriterBufferBytes(8192);
    }

    static PartitionInfo partition(long id) {
        return new PartitionInfo(YdbTopic.DescribeTopicResult.PartitionInfo.newBuilder()
                .setPartitionId(id).setActive(true).build());
    }

    final class ControlledWriter implements AsyncWriter {
        final WriterSettings config;
        final CompletableFuture<InitResult> initialized = new CompletableFuture<>();
        final CompletableFuture<Void> stopped = new CompletableFuture<>();
        final List<Sent> sent = new ArrayList<>();
        boolean automaticInit = true;
        boolean automaticShutdown = true;
        boolean inlineAck;
        boolean stopping;
        int outstanding;
        long reservedBytes;
        long previousSequence;
        QueueOverflowException overflow;

        ControlledWriter(WriterSettings config) {
            this.config = config;
        }

        @Override
        public CompletableFuture<InitResult> init() {
            if (automaticInit) {
                initialized.complete(new InitResult(histories.getOrDefault(config.getProducerId(), 0L)));
            }
            return initialized;
        }

        @Override
        public CompletableFuture<WriteAck> send(Message message) throws QueueOverflowException {
            return send(message, null);
        }

        @Override
        public CompletableFuture<WriteAck> send(Message message, SendSettings sendSettings)
                throws QueueOverflowException {
            Assert.assertFalse("No submit after shutdown", stopping);
            Assert.assertTrue("Explicit SeqNo required", message.getSeqNo() != null);
            Assert.assertTrue("Strictly increasing submit order", message.getSeqNo() > previousSequence);
            previousSequence = message.getSeqNo();
            if (overflow != null) {
                throw overflow;
            }
            long reservation = codec.getMaxEncodedSize(message.getData().length);
            Assert.assertTrue("Lower message capacity must be reserved", outstanding < config
                    .getMaxSendBufferMessagesCount());
            Assert.assertTrue("Lower byte capacity must be reserved", reservedBytes + reservation <= config
                    .getMaxSendBufferMemorySize());
            Sent item = new Sent(message, sendSettings, reservation);
            sent.add(item);
            outstanding++;
            reservedBytes += reservation;
            if (inlineAck) {
                ack(message.getSeqNo(), message.getSeqNo());
            }
            return item.ack;
        }

        void ack(long sequence, long offset) {
            ack(new WriteAck(sequence, WriteAck.State.WRITTEN, new WriteAck.Details(offset), null));
        }

        void ack(WriteAck value) {
            for (Sent item : sent) {
                if (!item.ack.isDone() && item.message.getSeqNo() <= value.getSeqNo()) {
                    outstanding--;
                    reservedBytes -= item.reservation;
                    item.ack.complete(value);
                }
            }
        }

        void diagnostic(Status status) {
            config.getErrorsHandler().accept(status, null);
        }

        void fail(Status status) {
            for (Sent item : sent) {
                if (!item.ack.isDone()) {
                    outstanding--;
                    reservedBytes -= item.reservation;
                    item.ack.completeExceptionally(new IllegalStateException("Stopped with " + status));
                }
            }
            diagnostic(status);
        }

        @Override
        public CompletableFuture<Void> shutdown() {
            stopping = true;
            if (automaticShutdown) {
                finishShutdown();
            }
            return stopped;
        }

        void finishShutdown() {
            stopped.complete(null);
            for (Sent item : sent) {
                if (!item.ack.isDone()) {
                    outstanding--;
                    reservedBytes -= item.reservation;
                    item.ack.completeExceptionally(new IllegalStateException("Closed"));
                }
            }
        }
    }

    static final class Sent {
        final Message message;
        final SendSettings settings;
        final long reservation;
        final CompletableFuture<WriteAck> ack = new CompletableFuture<>();

        Sent(Message message, SendSettings settings, long reservation) {
            this.message = message;
            this.settings = settings;
            this.reservation = reservation;
        }
    }

    static final class ManualExecutor implements Executor {
        final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        boolean reject;

        @Override
        public void execute(Runnable command) {
            if (reject) {
                throw new RejectedExecutionException("Injected test rejection");
            }
            tasks.add(command);
        }

        boolean runOne() {
            Runnable task = tasks.poll();
            if (task == null) {
                return false;
            }
            task.run();
            return true;
        }
    }

    final class Timer extends FutureTask<Void> implements ScheduledFuture<Void> {
        private long deadline;
        private final long period;

        Timer(Runnable action, long deadline, long period) {
            super(action, null);
            this.deadline = deadline;
            this.period = period;
        }

        @Override
        public void run() {
            if (period == 0) {
                super.run();
            } else if (super.runAndReset()) {
                deadline += period;
            }
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(deadline - now, TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
        }
    }
}
