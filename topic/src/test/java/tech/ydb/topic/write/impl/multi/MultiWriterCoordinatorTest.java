package tech.ydb.topic.write.impl.multi;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.settings.MultiWriterSettings.RoutingMode;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.MultiMessage;
import tech.ydb.topic.write.MultiWriteResult;
import tech.ydb.topic.write.MultiWriteResult.ConfirmationSource;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.impl.multi.MultiWriterTestSupport.ControlledWriter;
import tech.ydb.topic.write.impl.multi.MultiWriterTestSupport.Sent;

public class MultiWriterCoordinatorTest {
    @Test
    public void bootstrapReadsMaximumHistoryClosesProbesAndRechecksTopology() throws Exception {
        MultiWriterTestSupport f = support();
        f.partitions = Arrays.asList(MultiWriterTestSupport.partition(0), MultiWriterTestSupport.partition(1));
        f.histories.put("owner-0", 17L);
        f.histories.put("owner-1", 41L);
        MultiWriterImpl writer = f.create();
        CompletableFuture<InitResult> init = writer.init();
        f.runUntilIdle();
        Assert.assertTrue(init.isDone());
        Assert.assertEquals(41, init.join().getSeqNo());
        Assert.assertTrue(f.describes >= 2);
        Assert.assertEquals(2, f.writers.size());
        Assert.assertTrue(f.writers.stream().allMatch(partition -> partition.stopping));
        CompletableFuture<MultiWriteResult> sent = writer.send(message(1));
        f.runUntilIdle();
        ControlledWriter active = f.writers.get(f.writers.size() - 1);
        Assert.assertEquals(Long.valueOf(42), active.sent.get(0).message.getSeqNo());
        active.ack(42, 0);
        f.runUntilIdle();
        Assert.assertEquals(42, result(sent).getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void bootstrapDiscardsHistoryWhenTopologyChangesDuringProbe() {
        MultiWriterTestSupport f = support();
        f.onCreate = probeWriter -> probeWriter.automaticShutdown = false;
        MultiWriterImpl writer = f.create();
        CompletableFuture<InitResult> init = writer.init();
        f.histories.put("owner-0", 41L);
        f.runUntilIdle();

        Assert.assertEquals(1, f.describes);
        Assert.assertTrue(f.writers.get(0).stopping);
        f.partitions = Arrays.asList(MultiWriterTestSupport.partition(1), MultiWriterTestSupport.partition(2));
        f.histories.put("owner-1", 7L);
        f.onCreate = probeWriter -> { };
        f.writers.get(0).stopped.complete(null);
        f.runUntilIdle();
        Assert.assertEquals(2, f.describes);
        Assert.assertFalse(init.isDone());

        f.passTime(Duration.ofMillis(100));
        Assert.assertEquals(4, f.describes);
        Assert.assertTrue(init.isDone());
        Assert.assertEquals(7, init.join().getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void bootstrapBoundsConcurrentHistoryProbes() {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setMaxWriters(2).setMaxConcurrentInits(2).build());
        f.partitions = Arrays.asList(MultiWriterTestSupport.partition(0), MultiWriterTestSupport.partition(1),
                MultiWriterTestSupport.partition(2));
        f.onCreate = probeWriter -> probeWriter.automaticInit = false;
        MultiWriterImpl writer = f.create();
        CompletableFuture<InitResult> init = writer.init();
        f.runUntilIdle();
        Assert.assertEquals(2, f.writers.size());
        Assert.assertFalse(init.isDone());
        f.writers.get(0).initialized.complete(new InitResult(8));
        f.runUntilIdle();
        Assert.assertEquals(3, f.writers.size());
        f.writers.get(1).initialized.complete(new InitResult(4));
        f.writers.get(2).initialized.complete(new InitResult(6));
        f.runUntilIdle();
        Assert.assertTrue(init.isDone());
        Assert.assertEquals(8, init.join().getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void failedBootstrapProbeWaitsForOtherProbesToCloseBeforeRetry() {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setMaxWriters(2).setMaxConcurrentInits(2).build());
        f.partitions = Arrays.asList(MultiWriterTestSupport.partition(0), MultiWriterTestSupport.partition(1),
                MultiWriterTestSupport.partition(2));
        f.onCreate = writer -> {
            writer.automaticInit = false;
            writer.automaticShutdown = false;
        };
        MultiWriterImpl writer = f.create();
        CompletableFuture<InitResult> init = writer.init();
        f.runUntilIdle();

        ControlledWriter failed = f.writers.get(0);
        ControlledWriter other = f.writers.get(1);
        failed.initialized.completeExceptionally(new IllegalStateException("Transient probe failure"));
        other.initialized.complete(new InitResult(41));
        f.runUntilIdle();
        failed.finishShutdown();
        f.passTime(Duration.ofMillis(100));
        Assert.assertEquals(1, f.describes);
        Assert.assertEquals(2, f.writers.size());
        Assert.assertFalse(init.isDone());

        other.finishShutdown();
        f.runUntilIdle();
        f.onCreate = lower -> { };
        f.histories.put("owner-2", 7L);
        f.passTime(Duration.ofMillis(100));
        Assert.assertEquals(3, f.describes);
        Assert.assertTrue(init.isDone());
        Assert.assertEquals(7, init.join().getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void requiresSuccessfulInitializationBeforeAdmission() {
        MultiWriterTestSupport f = support();
        f.nextDescribe = new CompletableFuture<>();
        MultiWriterImpl writer = f.create();
        Assert.assertThrows(IllegalStateException.class, () -> writer.send(message(1)));
        writer.init();
        f.runUntilIdle();
        Assert.assertThrows(IllegalStateException.class, () -> writer.send(message(1)));
        writer.shutdown();
        f.runUntilIdle();
    }

    @Test
    public void pipelinesMessagesWithoutOverflowingLowerQueue() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setWriterBufferMessages(2).build());
        MultiWriterImpl writer = initialized(f);
        List<CompletableFuture<MultiWriteResult>> results = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            results.add(writer.send(message(index)));
        }
        f.runUntilIdle();
        ControlledWriter lower = f.active(0);
        Assert.assertEquals(2, lower.sent.size());
        Assert.assertFalse(results.get(0).isDone());
        Assert.assertFalse(results.get(1).isDone());
        lower.ack(1, 0);
        f.runUntilIdle();
        Assert.assertEquals(3, lower.sent.size());
        lower.ack(2, 1);
        f.runUntilIdle();
        Assert.assertEquals(4, lower.sent.size());
        lower.ack(4, 3);
        f.runUntilIdle();
        Assert.assertEquals(5, lower.sent.size());
        lower.ack(5, 4);
        f.runUntilIdle();
        for (int index = 0; index < results.size(); index++) {
            Assert.assertEquals(index + 1, result(results.get(index)).getSeqNo());
        }
        shutdown(writer, f);
    }

    @Test
    public void rejectedAdmissionDoesNotConsumeSequenceAndSamePayloadIsNewMessage() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setBufferMessages(2).build());
        MultiWriterImpl writer = initialized(f);
        MultiMessage input = message(1);
        CompletableFuture<MultiWriteResult> first = writer.send(input);
        CompletableFuture<MultiWriteResult> second = writer.send(input);
        Assert.assertThrows(QueueOverflowException.class, () -> writer.send(input));
        f.runUntilIdle();
        ControlledWriter lower = f.active(0);
        lower.ack(1, 0);
        f.runUntilIdle();
        CompletableFuture<MultiWriteResult> third = writer.send(input);
        f.runUntilIdle();
        Assert.assertEquals(Long.valueOf(3), lower.sent.get(2).message.getSeqNo());
        lower.ack(3, 2);
        f.runUntilIdle();
        Assert.assertEquals(1, result(first).getSeqNo());
        Assert.assertEquals(2, result(second).getSeqNo());
        Assert.assertEquals(3, result(third).getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void submittedMessageKeepsPayloadTimestampAndMetadata() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        byte[] data = { 1, 2 };
        byte[] value = { 3, 4 };
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        MultiMessage input = MultiMessage.newBuilder().setData(data).setKey("key").setCreateTimestamp(timestamp)
                .addMetadataItem(new MetadataItem("tag", value)).build();
        writer.send(input);
        data[0] = 9;
        value[0] = 9;
        f.runUntilIdle();
        ControlledWriter lower = f.active(0);
        Sent sent = lower.sent.get(0);
        Assert.assertArrayEquals(new byte[] { 1, 2 }, sent.message.getData());
        Assert.assertArrayEquals(new byte[] { 3, 4 }, sent.message.getMetadataItems().get(0).getValue());
        Assert.assertEquals(timestamp, sent.message.getCreateTimestamp());
        lower.ack(1, 0);
        f.runUntilIdle();
        shutdown(writer, f);
    }

    @Test
    public void cumulativeAckUsesPendingIdentityAndOnlyExactOffset() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> first = writer.send(message(1));
        CompletableFuture<MultiWriteResult> second = writer.send(message(2));
        f.runUntilIdle();
        f.active(0).ack(2, 91);
        f.runUntilIdle();
        Assert.assertEquals(1, result(first).getSeqNo());
        Assert.assertEquals(ConfirmationSource.ACK, result(first).getSource());
        Assert.assertNull(result(first).getOffset());
        Assert.assertEquals(2, result(second).getSeqNo());
        Assert.assertEquals(Long.valueOf(91), result(second).getOffset());
        f.active(0).ack(2, 91);
        f.runUntilIdle();
        shutdown(writer, f);
    }

    @Test
    public void handlesInlineLowerAckAfterRegisteringAttempt() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        f.onCreate = lower -> lower.inlineAck = true;
        CompletableFuture<MultiWriteResult> first = writer.send(message(1));
        f.runUntilIdle();
        Assert.assertEquals(1, result(first).getSeqNo());
        CompletableFuture<Void> flush = writer.flush();
        f.runUntilIdle();
        Assert.assertTrue(flush.isDone());
        flush.join();
        shutdown(writer, f);
    }

    @Test
    public void flushHasAdmissionWatermarkAndCancellationDoesNotRetractMessages() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> first = writer.send(message(1));
        CompletableFuture<Void> boundary = writer.flush();
        CompletableFuture<Void> cancelled = writer.flush();
        cancelled.cancel(false);
        CompletableFuture<MultiWriteResult> later = writer.send(message(2));
        f.runUntilIdle();
        f.active(0).ack(1, 0);
        f.runUntilIdle();
        Assert.assertTrue(first.isDone());
        Assert.assertTrue(boundary.isDone());
        boundary.join();
        Assert.assertFalse(later.isDone());
        f.active(0).ack(2, 1);
        f.runUntilIdle();
        Assert.assertEquals(2, result(later).getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void slowPartitionDoesNotHoldIndependentPartition() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setRoutingMode(RoutingMode.EXPLICIT).setWriterBufferMessages(1).build());
        f.partitions = Arrays.asList(MultiWriterTestSupport.partition(0), MultiWriterTestSupport.partition(1));
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> slow = writer.send(explicit(0, 1));
        writer.send(explicit(0, 2));
        CompletableFuture<MultiWriteResult> fast = writer.send(explicit(1, 3));
        f.runUntilIdle();
        Assert.assertEquals(1, f.active(0).sent.size());
        Assert.assertEquals(1, f.active(1).sent.size());
        f.active(1).ack(3, 0);
        f.runUntilIdle();
        Assert.assertEquals(3, result(fast).getSeqNo());
        Assert.assertFalse(slow.isDone());
        f.active(0).ack(1, 0);
        f.runUntilIdle();
        f.active(0).ack(2, 1);
        f.runUntilIdle();
        shutdown(writer, f);
    }

    @Test
    public void readyPartitionsNeverSubmitBeforeSequenceAssignment() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setRoutingMode(RoutingMode.EXPLICIT).build());
        f.partitions = Arrays.asList(MultiWriterTestSupport.partition(0), MultiWriterTestSupport.partition(1));
        MultiWriterImpl writer = initialized(f);
        writer.send(explicit(0, 1));
        writer.send(explicit(1, 2));
        f.runUntilIdle();
        ControlledWriter first = f.active(0);
        ControlledWriter second = f.active(1);
        first.ack(1, 0);
        second.ack(2, 0);
        f.runUntilIdle();
        CompletableFuture<MultiWriteResult> third = writer.send(explicit(0, 3));
        CompletableFuture<MultiWriteResult> fourth = writer.send(explicit(1, 4));
        f.runUntilIdle();
        Assert.assertEquals(Long.valueOf(3), first.sent.get(1).message.getSeqNo());
        Assert.assertEquals(Long.valueOf(4), second.sent.get(1).message.getSeqNo());
        first.ack(3, 1);
        second.ack(4, 1);
        f.runUntilIdle();
        Assert.assertEquals(3, result(third).getSeqNo());
        Assert.assertEquals(4, result(fourth).getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void terminalFailurePropagatesThroughFlushEvenWhenFuturePrecedesStatus() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        CompletableFuture<Void> flush = writer.flush();
        f.runUntilIdle();
        f.active(0).fail(Status.of(StatusCode.SCHEME_ERROR));
        f.runUntilIdle();
        Assert.assertTrue(pending.isCompletedExceptionally());
        Assert.assertTrue(flush.isCompletedExceptionally());
        Assert.assertThrows(IllegalStateException.class, () -> writer.send(message(2)));
        writer.shutdown();
        f.runUntilIdle();
    }

    @Test
    public void transientDiagnosticDoesNotResubmitPendingAtUpperLayer() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        ControlledWriter lower = f.active(0);
        int before = f.writers.size();
        lower.diagnostic(Status.of(StatusCode.UNAVAILABLE));
        f.runUntilIdle();
        Assert.assertFalse(pending.isDone());
        Assert.assertEquals(before, f.writers.size());
        Assert.assertEquals(1, lower.sent.size());
        lower.ack(1, 0);
        f.runUntilIdle();
        Assert.assertEquals(1, result(pending).getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void unexpectedLowerOverflowFailsWithoutRenumberingOrHotRetry() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        int probes = f.writers.size();
        f.onCreate = lower -> lower.overflow = new QueueOverflowException("unexpected overflow");
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        Assert.assertTrue(pending.isCompletedExceptionally());
        Assert.assertEquals(probes + 1, f.writers.size());
        Assert.assertEquals(1, f.writers.get(probes).previousSequence);
        writer.shutdown();
        f.runUntilIdle();
    }

    @Test
    public void deliveryDeadlineFailsUnknownOutcomeWithoutParallelResend() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setDeliveryTimeout(Duration.ofSeconds(1)).build());
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        int before = f.writers.size();
        f.passTime(Duration.ofSeconds(1));
        Assert.assertTrue(pending.isCompletedExceptionally());
        Assert.assertEquals(before, f.writers.size());
        Assert.assertEquals(1, f.writers.get(before - 1).sent.size());
        writer.shutdown();
        f.runUntilIdle();
    }

    @Test
    public void shutdownStopsAdmissionAndWaitsForAcceptedMessage() throws Exception {
        MultiWriterTestSupport f = support();
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        ControlledWriter lower = f.active(0);
        CompletableFuture<Void> stopped = writer.shutdown();
        Assert.assertThrows(IllegalStateException.class, () -> writer.send(message(2)));
        f.runUntilIdle();
        Assert.assertFalse(stopped.isDone());
        lower.ack(1, 0);
        f.runUntilIdle();
        Assert.assertTrue(pending.isDone());
        Assert.assertTrue(stopped.isDone());
        stopped.join();
        Assert.assertTrue(lower.stopping);
        Assert.assertTrue(writer.shutdown().isDone());
    }

    @Test
    public void hungLowerShutdownHasFiniteFailedResult() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setCloseTimeout(Duration.ofSeconds(1)).build());
        MultiWriterImpl writer = initialized(f);
        writer.send(message(1));
        f.runUntilIdle();
        ControlledWriter lower = f.active(0);
        lower.ack(1, 0);
        f.runUntilIdle();
        lower.automaticShutdown = false;
        CompletableFuture<Void> stopped = writer.shutdown();
        f.runUntilIdle();
        Assert.assertFalse(stopped.isDone());
        f.passTime(Duration.ofSeconds(1));
        Assert.assertTrue(stopped.isCompletedExceptionally());
        Assert.assertFalse(lower.stopped.isDone());
        lower.finishShutdown();
        f.runUntilIdle();
        Assert.assertTrue(stopped.isCompletedExceptionally());
    }

    @Test
    public void initializationDeadlineClosesProbesAndIgnoresLateInit() {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setInitTimeout(Duration.ofSeconds(1)).build());
        f.onCreate = lower -> lower.automaticInit = false;
        MultiWriterImpl writer = f.create();
        CompletableFuture<InitResult> init = writer.init();
        f.runUntilIdle();
        Assert.assertEquals(1, f.writers.size());
        f.passTime(Duration.ofSeconds(1));
        Assert.assertTrue(init.isCompletedExceptionally());
        Assert.assertTrue(f.writers.get(0).stopping);
        f.writers.get(0).initialized.complete(new InitResult(7));
        f.runUntilIdle();
        Assert.assertEquals(1, f.writers.size());
        Assert.assertTrue(init.isCompletedExceptionally());
        writer.shutdown();
        f.runUntilIdle();
    }

    @Test
    public void invalidExplicitSequenceDoesNotStrandLaterAdmittedMessages() throws Exception {
        MultiWriterTestSupport f = support();
        f.histories.put("owner-0", 41L);
        MultiWriterImpl writer = initialized(f);
        CompletableFuture<MultiWriteResult> invalid = writer.send(MultiMessage.newBuilder()
                .setData(new byte[] { 1 }).setKey("key").setSeqNo(40).build());
        CompletableFuture<MultiWriteResult> later = writer.send(message(2));
        f.runUntilIdle();
        Assert.assertTrue(invalid.isCompletedExceptionally());
        Assert.assertTrue("Already admitted message must not remain pending after terminal failure",
                later.isCompletedExceptionally());
    }

    @Test
    public void interleavedPartitionAcksCompleteEachMessageOnceWithFixedSeeds() throws Exception {
        for (long seed : new long[] { 7, 991 }) {
            MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                    .setRoutingMode(RoutingMode.EXPLICIT).setWriterBufferMessages(2).build());
            f.partitions = Arrays.asList(MultiWriterTestSupport.partition(0), MultiWriterTestSupport.partition(1),
                    MultiWriterTestSupport.partition(2), MultiWriterTestSupport.partition(3));
            MultiWriterImpl writer = initialized(f);
            AtomicInteger completions = new AtomicInteger();
            List<CompletableFuture<MultiWriteResult>> results = new ArrayList<>();
            for (int index = 0; index < 80; index++) {
                CompletableFuture<MultiWriteResult> future = writer.send(explicit(index % 4, index));
                future.thenRun(completions::incrementAndGet);
                results.add(future);
            }
            f.runUntilIdle();
            Random random = new Random(seed);
            for (int attempt = 0; attempt < 1000 && completions.get() < 80; attempt++) {
                ControlledWriter lower = f.active(random.nextInt(4));
                for (Sent sent : lower.sent) {
                    if (!sent.ack.isDone()) {
                        lower.ack(sent.message.getSeqNo(), sent.message.getSeqNo());
                        break;
                    }
                }
                f.runUntilIdle();
            }
            Assert.assertEquals("seed=" + seed, 80, completions.get());
            for (int index = 0; index < results.size(); index++) {
                Assert.assertEquals(index + 1, result(results.get(index)).getSeqNo());
            }
            shutdown(writer, f);
        }
    }

    @Test
    public void sharedByteBudgetRejectsBeforeAdmissionAndIsReusedAfterCompletion() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setMaxMessageBytes(512).setBufferBytes(2000).setBufferMessages(10).build());
        MultiWriterImpl writer = initialized(f);
        MultiMessage input = MultiMessage.of(new byte[512], "a");
        writer.send(input);
        Assert.assertThrows(QueueOverflowException.class, () -> writer.send(input));
        f.runUntilIdle();
        ControlledWriter lower = f.active(0);
        Assert.assertEquals(1, lower.sent.size());
        lower.ack(1, 0);
        f.runUntilIdle();
        CompletableFuture<MultiWriteResult> second = writer.send(input);
        f.runUntilIdle();
        Assert.assertEquals(Long.valueOf(2), lower.sent.get(1).message.getSeqNo());
        lower.ack(2, 1);
        f.runUntilIdle();
        Assert.assertEquals(2, result(second).getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void smallMessageCannotOvertakeEarlierMessageWaitingForLowerBytes() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setMaxMessageBytes(800).setWriterBufferBytes(1000).build());
        MultiWriterImpl writer = initialized(f);
        writer.send(MultiMessage.of(new byte[700], "a"));
        writer.send(MultiMessage.of(new byte[800], "a"));
        CompletableFuture<MultiWriteResult> small = writer.send(MultiMessage.of(new byte[10], "a"));
        f.runUntilIdle();
        ControlledWriter lower = f.active(0);
        Assert.assertEquals("Third message must wait behind the second despite fitting free bytes", 1,
                lower.sent.size());
        lower.ack(1, 0);
        f.runUntilIdle();
        Assert.assertEquals(3, lower.sent.size());
        Assert.assertEquals(Long.valueOf(2), lower.sent.get(1).message.getSeqNo());
        Assert.assertEquals(Long.valueOf(3), lower.sent.get(2).message.getSeqNo());
        lower.ack(3, 2);
        f.runUntilIdle();
        Assert.assertEquals(3, result(small).getSeqNo());
        shutdown(writer, f);
    }

    @Test
    public void idleReopenRejectsHigherProducerSequence() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setIdleTimeout(Duration.ofMillis(100)).build());
        MultiWriterImpl writer = initialized(f);
        writer.send(message(1));
        f.runUntilIdle();
        ControlledWriter original = f.active(0);
        original.ack(1, 0);
        f.runUntilIdle();
        f.passTime(Duration.ofMillis(100));
        Assert.assertTrue(original.stopping);
        f.histories.put("owner-0", 5L);
        CompletableFuture<MultiWriteResult> next = writer.send(message(2));
        f.runUntilIdle();
        Assert.assertTrue("Unexpected history cannot falsely confirm a new local message",
                next.isCompletedExceptionally());
        Assert.assertEquals(0, f.writers.get(f.writers.size() - 1).sent.size());
        CompletableFuture<Void> closed = writer.shutdown();
        f.runUntilIdle();
        Assert.assertTrue(closed.isCompletedExceptionally());
    }

    @Test
    public void exhaustedBootstrapSequenceNeverWrapsOrOpensAdmission() {
        MultiWriterTestSupport f = support();
        f.histories.put("owner-0", Long.MAX_VALUE);
        MultiWriterImpl writer = f.create();
        CompletableFuture<InitResult> init = writer.init();
        f.runUntilIdle();
        Assert.assertTrue(init.isCompletedExceptionally());
        Assert.assertTrue(f.writers.stream().allMatch(lower -> lower.stopping));
        Assert.assertThrows(IllegalStateException.class, () -> writer.send(message(1)));
        CompletableFuture<Void> closed = writer.shutdown();
        f.runUntilIdle();
        Assert.assertTrue(closed.isCompletedExceptionally());
    }

    private static MultiWriterTestSupport support() {
        return new MultiWriterTestSupport(MultiWriterTestSupport.settings().build());
    }

    private static MultiWriterImpl initialized(MultiWriterTestSupport support) {
        MultiWriterImpl writer = support.create();
        CompletableFuture<InitResult> init = writer.init();
        support.runUntilIdle();
        Assert.assertTrue("Initialization did not complete", init.isDone());
        init.join();
        return writer;
    }

    private static MultiMessage message(int value) {
        return MultiMessage.of(new byte[] { (byte) value }, "key");
    }

    private static MultiMessage explicit(long partition, int value) {
        return MultiMessage.newBuilder().setData(new byte[] { (byte) value }).setPartitionId(partition).build();
    }

    private static MultiWriteResult result(CompletableFuture<MultiWriteResult> future) {
        Assert.assertTrue("Message did not complete", future.isDone());
        return future.join();
    }

    private static void shutdown(MultiWriterImpl writer, MultiWriterTestSupport support) {
        CompletableFuture<Void> stopped = writer.shutdown();
        support.runUntilIdle();
        Assert.assertTrue("Shutdown did not complete", stopped.isDone());
        stopped.join();
    }
}
