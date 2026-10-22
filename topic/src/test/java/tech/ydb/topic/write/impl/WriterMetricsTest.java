package tech.ydb.topic.write.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.RecordingMeter;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;

public class WriterMetricsTest {
    private static final String PREFIX = "ydb.topic.writer.";
    private final RecordingMeter meter = new RecordingMeter();

    @Test
    public void writerCountersCountAcceptedMessagesAndAcknowledgements() throws QueueOverflowException {
        try (WriterFixture f = new WriterFixture(meter)) {
            f.writer.send(Message.of(new byte[]{1, 2}));
            f.writer.send(Message.of(new byte[]{3, 4, 5}));
            f.writer.send(Message.of(new byte[0]));
            Assert.assertEquals(3, meter.value(PREFIX + "sending.messages"));
            Assert.assertEquals(5, meter.value(PREFIX + "sending.bytes"));
            Assert.assertEquals(0, meter.value(PREFIX + "written.messages"));
            f.stream.sendAckResponse(2, 10);
            Assert.assertEquals(2, meter.value(PREFIX + "written.messages"));
            f.stream.sendAckResponse(2, 10);
            Assert.assertEquals(2, meter.value(PREFIX + "written.messages"));
            f.stream.sendAckResponse(3, 11);
            Assert.assertEquals(3, meter.value(PREFIX + "written.messages"));
            Assert.assertEquals(3, meter.histograms.get(PREFIX + "message.ack.duration").size());
        }
    }

    @Test
    public void writerBufferGaugesTrackBudgetAndLimit() throws QueueOverflowException {
        String used = "ydb.topic.writer.buffer.used.bytes";
        String limit = "ydb.topic.writer.buffer.limit.bytes";
        try (WriterFixture f = new WriterFixture(meter)) {
            Assert.assertEquals(0, meter.collect(used));
            Assert.assertEquals(100, meter.collect(limit));
            Assert.assertEquals(0, meter.collectDouble(PREFIX + "sending.oldest_age"), 0);
            f.writer.send(Message.of(new byte[]{1, 2}));
            f.writer.send(Message.of(new byte[]{3, 4, 5}));
            Assert.assertEquals(5, meter.collect(used));
            Assert.assertEquals(100, meter.collect(limit));
            f.stream.sendAckResponse(2, 10);
            Assert.assertEquals(0, meter.collect(used));
            Assert.assertEquals(0, meter.collectDouble(PREFIX + "sending.oldest_age"), 0);
        }
        Assert.assertTrue(meter.gauges.isEmpty());
        Assert.assertTrue(meter.doubleGauges.isEmpty());
    }

    @Test
    public void rejectedAdmissionDoesNotChangeCounters() throws QueueOverflowException {
        try (WriterFixture f = new WriterFixture(meter)) {
            f.writer.send(Message.of(new byte[100]));
            Assert.assertThrows(QueueOverflowException.class, () -> f.writer.send(Message.of(new byte[]{1})));
            Assert.assertEquals(1, meter.value(PREFIX + "sending.messages"));
            Assert.assertEquals(100, meter.value(PREFIX + "sending.bytes"));
            Assert.assertEquals(0, meter.value(PREFIX + "written.messages"));
            Assert.assertEquals(1, meter.histograms.get(PREFIX + "message.processing.duration").size());
        }
    }

    @Test
    public void emptyBodyStillCountsAsAnAcceptedMessage() {
        WriterMetrics metrics = new WriterMetrics(meter, "/topic", "writer");
        metrics.reportSending(0);
        metrics.reportSending(5);
        metrics.reportWritten();
        Assert.assertEquals(2, meter.value(PREFIX + "sending.messages"));
        Assert.assertEquals(5, meter.value(PREFIX + "sending.bytes"));
        Assert.assertEquals(1, meter.value(PREFIX + "written.messages"));
        Assert.assertEquals("By", meter.units.get(PREFIX + "sending.bytes"));
    }

    @Test
    public void preparationDurationsUseSeconds() {
        WriterMetrics metrics = new WriterMetrics(meter, "/topic", "writer");
        metrics.reportCompression(250_000_000, Codec.GZIP, null);
        metrics.reportProcessing(1_500_000_000, Codec.GZIP, null);
        Assert.assertEquals(Collections.singletonList(0.25), meter.histograms.get(PREFIX + "compression.duration"));
        Assert.assertEquals(Collections.singletonList(1.5), meter.histograms.get(PREFIX + "message.processing.duration"));
        for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
            Assert.assertEquals("s", meter.units.get(PREFIX + name));
            meter.assertAttributes(PREFIX + name,
                    "topic", "/topic", "writer.name", "writer", "codec", "gzip", "outcome", "ok");
        }
    }

    @Test
    public void preparationFailuresKeepCodecAndError() {
        WriterMetrics metrics = new WriterMetrics(meter, "/topic", "writer");
        metrics.reportCompression(500_000_000, 10001, new IOException());
        metrics.reportProcessing(2_000_000_000, 10001, new IOException());
        Assert.assertEquals(Collections.singletonList(0.5), meter.histograms.get(PREFIX + "compression.duration"));
        Assert.assertEquals(Collections.singletonList(2.0), meter.histograms.get(PREFIX + "message.processing.duration"));
        for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
            meter.assertAttributes(PREFIX + name, "topic", "/topic", "writer.name", "writer",
                    "codec", "10001", "outcome", "error", "error.type", "java.io.IOException");
        }
    }

    @Test
    public void ackAndBufferWaitUseMonotonicSeconds() {
        WriterMetrics metrics = new WriterMetrics(meter, "/topic", "writer");
        long timestamp = System.nanoTime() - 2_000_000_000L;
        metrics.reportMessageAckDuration(timestamp);
        metrics.reportBufferWaitDuration(timestamp);
        long end = System.nanoTime();
        for (String name : Arrays.asList("message.ack.duration", "buffer.wait.duration")) {
            List<Double> samples = meter.histograms.get(PREFIX + name);
            Assert.assertEquals(1, samples.size());
            assertElapsedSeconds(name, samples.get(0), timestamp, end);
        }
    }

    @Test
    public void oldestAgeUsesMonotonicSeconds() {
        WriterMetrics metrics = new WriterMetrics(meter, "/topic", "writer");
        long timestamp = System.nanoTime() - 2_000_000_000L;
        metrics.register(() -> 5, () -> 10, () -> timestamp);
        double age = meter.collectDouble(PREFIX + "sending.oldest_age");
        assertElapsedSeconds("sending.oldest_age", age, timestamp, System.nanoTime());
        metrics.unregister();
        Assert.assertTrue(meter.doubleGauges.isEmpty());
    }

    @Test
    public void sessionErrorsCountRetryAndStopOnce() {
        WriterMetrics metrics = new WriterMetrics(meter, "/topic", "writer");
        metrics.reportSessionError(Status.of(StatusCode.TRANSPORT_UNAVAILABLE), true);
        Assert.assertEquals(1, meter.value(PREFIX + "session.errors"));
        meter.assertAttributes(PREFIX + "session.errors", "topic", "/topic", "writer.name", "writer",
                "retry_decision", "retry", "status_code", "TRANSPORT_UNAVAILABLE", "error.type", "transport_error");
        metrics.reportSessionError(Status.of(StatusCode.BAD_REQUEST), false);
        Assert.assertEquals(2, meter.value(PREFIX + "session.errors"));
        meter.assertAttributes(PREFIX + "session.errors", "topic", "/topic", "writer.name", "writer",
                "retry_decision", "stop", "status_code", "BAD_REQUEST", "error.type", "ydb_error");
    }

    private void assertElapsedSeconds(String name, double value, long timestamp, long end) {
        Assert.assertTrue(value >= 2);
        Assert.assertTrue(value <= (end - timestamp) / 1_000_000_000d);
        Assert.assertEquals("s", meter.units.get(PREFIX + name));
        meter.assertAttributes(PREFIX + name, "topic", "/topic", "writer.name", "writer");
    }

    private static TopicRpc mockRpc(WriteStreamMock stream) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.writeSession(Mockito.any(String.class))).thenReturn(stream);
        return rpc;
    }

    private static final class WriterFixture implements AutoCloseable {
        private final WriteStreamMock stream = new WriteStreamMock();
        private final TopicClient client = TopicClientImpl.newClient(mockRpc(stream))
                .setCompressionExecutor(Runnable::run).build();
        private final AsyncWriter writer;

        WriterFixture(RecordingMeter meter) {
            writer = client.createAsyncWriter(WriterSettings.newBuilder().setTopicPath("/test/topic")
                    .setCodec(Codec.RAW).setMaxSendBufferMemorySize(100).withMeter(meter, "writer").build());
            writer.init();
            stream.sendInitResponse(0);
        }

        @Override
        public void close() {
            try {
                writer.shutdown();
                stream.close(Status.SUCCESS);
            } finally {
                client.close();
            }
        }
    }
}
