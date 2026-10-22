package tech.ydb.topic.read.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.utils.RecordingMeter;

public class ReaderMetricsTest {
    private static final String PREFIX = "ydb.topic.reader.";

    private static final byte[] MSG1 = new byte[] { 1 };
    private static final byte[] MSG2 = new byte[] { 2 };
    private static final byte[] MSG3 = new byte[] { 3 };
    private final RecordingMeter meter = new RecordingMeter();

    @Test
    public void readerCountersIncrementOnReceive() throws InterruptedException {
        try (ReaderFixture f = new ReaderFixture(meter)) {
            f.start();
            f.stream.responseStartPartition("/topic", 42, 0);
            f.stream.responseData(25).partition(1, 0).batch(Codec.RAW, MSG1, MSG2).and().send();
            Assert.assertEquals(2, meter.value(PREFIX + "received.messages"));
            Assert.assertEquals(25, meter.value(PREFIX + "received.bytes"));
            f.stream.responseData(30).partition(1, 2).batch(Codec.RAW, MSG1, MSG2, MSG3).and().send();
            Assert.assertEquals(5, meter.value(PREFIX + "received.messages"));
            Assert.assertEquals(55, meter.value(PREFIX + "received.bytes"));
            Assert.assertEquals(5, meter.collect(PREFIX + "local_buffer.messages"));
            Assert.assertEquals(0, meter.value(PREFIX + "delivered.messages"));
            meter.assertAttributes(PREFIX + "received.messages",
                    "consumer", "consumer", "reader.name", "reader", "topic", "/topic");
            Assert.assertNotNull(f.reader.receive(0, TimeUnit.SECONDS));
            Assert.assertNotNull(f.reader.receive(0, TimeUnit.SECONDS));
            Assert.assertEquals(2, meter.value(PREFIX + "delivered.messages"));
            Assert.assertEquals(3, meter.collect(PREFIX + "local_buffer.messages"));
        }
    }

    @Test
    public void commitMetricsFollowPublicAcknowledgement() throws InterruptedException {
        try (ReaderFixture f = new ReaderFixture(meter)) {
            f.start();
            f.stream.responseStartPartition("/topic", 42, 0);
            f.stream.responseData(5).partition(1, 0).batch(Codec.RAW, MSG1).and().send();
            Message message = f.reader.receive(0, TimeUnit.SECONDS);
            Assert.assertNotNull(message);
            CompletableFuture<Void> committed = message.commit();
            f.stream.assertLastMessage().isCommit(1);
            Assert.assertFalse(committed.isDone());
            Assert.assertEquals(1, meter.value(PREFIX + "commit.queued"));
            Assert.assertEquals(1, meter.collect(PREFIX + "commit_offset.lag.max"));
            f.stream.responseCommitAck().partition(1, 1).send();
            Assert.assertTrue(committed.isDone());
            Assert.assertFalse(committed.isCompletedExceptionally());
            Assert.assertEquals(1, meter.value(PREFIX + "commit.acknowledged"));
            Assert.assertEquals(0, meter.collect(PREFIX + "commit_offset.lag.max"));
        }
    }

    @Test
    public void commitCountersUseAcceptedWidthsAndAcknowledgedFrontier() {
        ReaderMetrics metrics = registeredMetrics();
        metrics.addPartition(1, 10);
        metrics.reportCommitQueued(1, 15, 3, "/topic");
        metrics.reportCommitQueued(1, 12, 2, "/topic");
        Assert.assertEquals(5, meter.value(PREFIX + "commit.queued"));
        Assert.assertEquals(5, meter.collect(PREFIX + "commit_offset.lag.max"));
        metrics.reportCommitAcknowledged(1, 13, "/topic");
        metrics.reportCommitAcknowledged(1, 13, "/topic");
        metrics.reportCommitAcknowledged(1, 12, "/topic");
        Assert.assertEquals(3, meter.value(PREFIX + "commit.acknowledged"));
        metrics.reportCommitAcknowledged(1, 15, "/topic");
        Assert.assertEquals(5, meter.value(PREFIX + "commit.acknowledged"));
        Assert.assertEquals(0, meter.collect(PREFIX + "commit_offset.lag.max"));
        for (String name : Arrays.asList("commit.queued", "commit.acknowledged")) {
            meter.assertAttributes(PREFIX + name, "consumer", "consumer", "reader.name", "reader", "topic", "/topic");
        }
    }

    @Test
    public void commitLagUsesMaximumAndDropsClosedPartitions() {
        ReaderMetrics metrics = registeredMetrics();
        metrics.addPartition(1, 0);
        metrics.addPartition(2, 0);
        metrics.reportCommitQueued(1, 20, 2, "/topic");
        metrics.reportCommitQueued(1, 10, 1, "/topic");
        metrics.reportCommitQueued(2, 7, 7, "/topic");
        Assert.assertEquals(20, meter.collect(PREFIX + "commit_offset.lag.max"));
        metrics.updateCommittedOffset(1, 25);
        Assert.assertEquals(7, meter.collect(PREFIX + "commit_offset.lag.max"));
        metrics.removePartition(2);
        Assert.assertEquals(0, meter.collect(PREFIX + "commit_offset.lag.max"));
        Assert.assertEquals(1, meter.collect(PREFIX + "partition_session.count"));
    }

    @Test
    public void bufferCountsUseCurrentSourcesAndRemoveClosedOnes() {
        ReaderMetrics metrics = registeredMetrics();
        ReaderMetrics.Source first = source(2, 0);
        ReaderMetrics.Source second = source(3, 0);
        metrics.addSource(first);
        metrics.addSource(second);
        Assert.assertEquals(5, meter.collect(PREFIX + "local_buffer.messages"));
        Mockito.when(first.getMessagesCount()).thenReturn(4L);
        Assert.assertEquals(7, meter.collect(PREFIX + "local_buffer.messages"));
        metrics.removeSource(first);
        Assert.assertEquals(3, meter.collect(PREFIX + "local_buffer.messages"));
        metrics.removeSource(second);
        Assert.assertEquals(0, meter.collect(PREFIX + "local_buffer.messages"));
        meter.assertAttributes(PREFIX + "local_buffer.messages", "consumer", "consumer", "reader.name", "reader");
    }

    @Test
    public void bufferAgeUsesOldestNonZeroSourceInSeconds() {
        ReaderMetrics metrics = registeredMetrics();
        Assert.assertEquals(0, meter.collectDouble(PREFIX + "local_buffer.message_age.max"), 0);
        long oldest = System.nanoTime() - TimeUnit.DAYS.toNanos(1);
        long newer = oldest + TimeUnit.HOURS.toNanos(12);
        ReaderMetrics.Source first = source(1, oldest);
        metrics.addSource(first);
        metrics.addSource(source(2, newer));
        metrics.addSource(source(3, 0));
        assertBufferAge(oldest);
        metrics.removeSource(first);
        assertBufferAge(newer);
        metrics.unregister();
        Assert.assertTrue(meter.doubleGauges.isEmpty());
    }

    @Test
    public void gaugesObservePartitionSessionsAndProtocolCredit() throws InterruptedException {
        try (ReaderFixture f = new ReaderFixture(meter)) {
            Assert.assertTrue(meter.gauges.isEmpty());
            f.reader.init();
            Assert.assertTrue(meter.gauges.isEmpty());
            f.stream.responseInit("session");
            assertReaderGauges(0, 100);
            f.stream.responseStartPartition("/topic", 42, 0);
            assertReaderGauges(1, 100);
            f.stream.responseData(20).partition(1, 0).batch(Codec.RAW, MSG1, MSG2).and().send();
            assertReaderGauges(1, 80);
            Assert.assertNotNull(f.reader.receive(0, TimeUnit.SECONDS));
            assertReaderGauges(1, 80);
            Assert.assertNotNull(f.reader.receive(0, TimeUnit.SECONDS));
            assertReaderGauges(1, 100);
            f.stream.closeStream(Status.of(StatusCode.OVERLOADED));
            Assert.assertTrue(meter.gauges.isEmpty());
        }
        Assert.assertTrue(meter.gauges.isEmpty());
    }

    @Test
    public void creditCanBeNegativeWhenServerExceedsWindow() throws InterruptedException {
        try (ReaderFixture f = new ReaderFixture(meter)) {
            f.start();
            f.stream.responseStartPartition("/topic", 42, 0);
            f.stream.responseData(120).partition(1, 0).batch(Codec.RAW, MSG1).and().send();
            assertReaderGauges(1, -20);
            Assert.assertNotNull(f.reader.receive(0, TimeUnit.SECONDS));
            assertReaderGauges(1, 100);
        }
    }

    @Test
    public void nonPositiveDeltasDoNotChangeCounters() {
        ReaderMetrics metrics = new ReaderMetrics(meter, "consumer", "reader");
        metrics.reportDelivered(2, "/topic");
        metrics.reportReceivedMessages(3, "/topic");
        metrics.reportReceivedBytes(5);
        for (long ignored : new long[]{0, -1}) {
            metrics.reportDelivered(ignored, "/topic");
            metrics.reportReceivedMessages(ignored, "/topic");
            metrics.reportReceivedBytes(ignored);
        }
        Assert.assertEquals(2, meter.value(PREFIX + "delivered.messages"));
        Assert.assertEquals(3, meter.value(PREFIX + "received.messages"));
        Assert.assertEquals(5, meter.value(PREFIX + "received.bytes"));
        Assert.assertEquals("By", meter.units.get(PREFIX + "received.bytes"));
    }

    @Test
    public void decodedDurationsUseSeconds() {
        ReaderMetrics metrics = new ReaderMetrics(meter, "consumer", "reader");
        metrics.reportDecoded(250_000_000, 1_500_000_000, Codec.GZIP, "/topic", null);
        Assert.assertEquals(Collections.singletonList(0.25), meter.histograms.get(PREFIX + "compression.duration"));
        Assert.assertEquals(Collections.singletonList(1.5), meter.histograms.get(PREFIX + "message.processing.duration"));
        for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
            Assert.assertEquals("s", meter.units.get(PREFIX + name));
            meter.assertAttributes(PREFIX + name, "consumer", "consumer", "reader.name", "reader",
                    "topic", "/topic", "codec", "gzip", "outcome", "ok");
        }
    }

    @Test
    public void decodedFailuresKeepCodecAndErrorWithoutConsumer() {
        ReaderMetrics metrics = new ReaderMetrics(meter, null, "reader");
        metrics.reportDecoded(500_000_000, 2_000_000_000, 10001, "/topic", new IOException());
        Assert.assertEquals(Collections.singletonList(0.5), meter.histograms.get(PREFIX + "compression.duration"));
        Assert.assertEquals(Collections.singletonList(2.0), meter.histograms.get(PREFIX + "message.processing.duration"));
        for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
            meter.assertAttributes(PREFIX + name, "reader.name", "reader", "topic", "/topic",
                    "codec", "10001", "outcome", "error", "error.type", "java.io.IOException");
        }
    }

    @Test
    public void sessionErrorsCountRetryAndStopOnce() {
        ReaderMetrics metrics = new ReaderMetrics(meter, "consumer", "reader");
        metrics.reportSessionError(Status.of(StatusCode.TRANSPORT_UNAVAILABLE), true);
        Assert.assertEquals(1, meter.value(PREFIX + "session.errors"));
        meter.assertAttributes(PREFIX + "session.errors", "consumer", "consumer", "reader.name", "reader",
                "retry_decision", "retry", "status_code", "TRANSPORT_UNAVAILABLE", "error.type", "transport_error");
        metrics.reportSessionError(Status.of(StatusCode.BAD_REQUEST), false);
        Assert.assertEquals(2, meter.value(PREFIX + "session.errors"));
        meter.assertAttributes(PREFIX + "session.errors", "consumer", "consumer", "reader.name", "reader",
                "retry_decision", "stop", "status_code", "BAD_REQUEST", "error.type", "ydb_error");
    }

    private ReaderMetrics registeredMetrics() {
        ReaderMetrics metrics = new ReaderMetrics(meter, "consumer", "reader");
        metrics.register(() -> 100);
        return metrics;
    }

    private static ReaderMetrics.Source source(long count, long timestamp) {
        ReaderMetrics.Source source = Mockito.mock(ReaderMetrics.Source.class);
        Mockito.when(source.getMessagesCount()).thenReturn(count);
        Mockito.when(source.getOldestReceivedTimestamp()).thenReturn(timestamp);
        return source;
    }

    private void assertBufferAge(long timestamp) {
        long before = System.nanoTime();
        double age = meter.collectDouble(PREFIX + "local_buffer.message_age.max");
        long after = System.nanoTime();
        Assert.assertEquals((before - timestamp) / 1_000_000_000d, age, (after - before) / 1_000_000_000d);
        Assert.assertEquals("s", meter.units.get(PREFIX + "local_buffer.message_age.max"));
    }

    private void assertReaderGauges(long partitions, long credit) {
        Assert.assertEquals(partitions, meter.collect(PREFIX + "partition_session.count"));
        Assert.assertEquals(credit, meter.collect(PREFIX + "credit_balance_bytes"));
    }

    private static final class ReaderFixture implements AutoCloseable {
        private final ReadStreamMock stream = new ReadStreamMock(true);
        private final TopicClient client;
        private final SyncReader reader;

        ReaderFixture(RecordingMeter meter) {
            TopicRpc rpc = Mockito.mock(TopicRpc.class);
            Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
            Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(stream);
            client = TopicClientImpl.newClient(rpc).build();
            reader = client.createSyncReader(ReaderSettings.newBuilder().addTopic("/topic")
                    .setConsumerName("consumer").setMaxMemoryUsageBytes(100).withMeter(meter, "reader").build());
        }

        void start() {
            reader.init();
            stream.assertLastMessage().isInitRequest("consumer", "/topic").hasReaderName("reader");
            stream.responseInit("read-session");
        }

        @Override
        public void close() {
            try {
                reader.shutdown();
            } finally {
                client.close();
            }
        }
    }
}
