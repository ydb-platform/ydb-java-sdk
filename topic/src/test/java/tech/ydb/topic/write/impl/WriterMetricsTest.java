package tech.ydb.topic.write.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.InMemoryMeter;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;

import static tech.ydb.topic.utils.InMemoryMeter.attributes;

public class WriterMetricsTest {
    private static final String PREFIX = "ydb.topic.writer.";
    private static final Map<String, String> COMMON = attributes("topic", "/test/topic", "writer.name", "writer");
    private final InMemoryMeter meter = new InMemoryMeter();

    @Test
    public void sendAndAcknowledgeMetrics() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(WriterSettings.newBuilder().setTopicPath("/test/topic")
                    .setCodec(Codec.RAW).setMaxSendBufferMemorySize(100).withMeter(meter, "writer").build());
            try {
                writer.init();
                stream.sendInitResponse(0);
                meter.assertLongGauge(PREFIX + "buffer.used.bytes", 0, COMMON);
                meter.assertLongGauge(PREFIX + "buffer.limit.bytes", 100, COMMON);
                CompletableFuture<Void> written = CompletableFuture.allOf(
                        writer.send(Message.of(new byte[]{1, 2})), writer.send(Message.of(new byte[]{3, 4, 5})));
                Assert.assertFalse(written.isDone());
                meter.assertCounter(PREFIX + "sending.messages", 2, COMMON);
                meter.assertCounter(PREFIX + "sending.bytes", 5, COMMON);
                meter.assertNoCounterSamples(PREFIX + "written.messages");
                meter.assertLongGauge(PREFIX + "buffer.used.bytes", 5, COMMON);
                meter.assertPositiveDoubleGauge(PREFIX + "sending.oldest_age", COMMON);
                stream.sendAckResponse(2, 10);
                written.get(1, TimeUnit.SECONDS);
                meter.assertCounter(PREFIX + "written.messages", 2, COMMON);
                meter.assertLongGauge(PREFIX + "buffer.used.bytes", 0, COMMON);
                meter.assertLongGauge(PREFIX + "buffer.limit.bytes", 100, COMMON);
                meter.assertDoubleGauge(PREFIX + "sending.oldest_age", 0, COMMON);
                meter.assertHistogram(PREFIX + "message.ack.duration", "s", 2, COMMON);
                meter.assertHistogram(PREFIX + "message.processing.duration", "s", 2, attributes(
                        "topic", "/test/topic", "writer.name", "writer", "codec", "raw", "outcome", "ok"));
                meter.assertNoHistogramSamples(PREFIX + "buffer.wait.duration");
                meter.assertNoHistogramSamples(PREFIX + "compression.duration");
                meter.assertUnit(PREFIX + "sending.bytes", "By");
            } finally {
                CompletableFuture<Void> closed = writer.shutdown();
                stream.close(Status.SUCCESS);
                closed.get(1, TimeUnit.SECONDS);
            }
        }
        meter.assertNoCounterSamples(PREFIX + "session.errors");
        meter.assertNoGauges();
    }

    @Test
    public void compressedSyncSendMetrics() throws Exception {
        WriteStreamMock stream = Mockito.spy(new WriteStreamMock());
        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            stream.close(Status.SUCCESS);
            return null;
        }).when(stream).close();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream))
                .setCompressionExecutor(Runnable::run).build()) {
            SyncWriter writer = client.createSyncWriter(WriterSettings.newBuilder().setTopicPath("/test/topic")
                    .setCodec(Codec.GZIP).setMaxSendBufferMemorySize(100).withMeter(meter, "writer").build());
            try {
                writer.init();
                stream.sendInitResponse(0);
                writer.send(Message.of(new byte[]{1, 2}), 1, TimeUnit.SECONDS);
                writer.send(Message.of(new byte[]{3, 4, 5}), 1, TimeUnit.SECONDS);
                meter.assertCounter(PREFIX + "sending.messages", 2, COMMON);
                meter.assertCounter(PREFIX + "sending.bytes", 5, COMMON);
                meter.assertNoCounterSamples(PREFIX + "written.messages");
                // The two GZIP bodies occupy 22 and 23 bytes.
                meter.assertLongGauge(PREFIX + "buffer.used.bytes", 45, COMMON);
                stream.sendAckResponse(2, 10);
                meter.assertCounter(PREFIX + "written.messages", 2, COMMON);
                meter.assertLongGauge(PREFIX + "buffer.used.bytes", 0, COMMON);
                for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
                    meter.assertHistogram(PREFIX + name, "s", 2, attributes(
                            "topic", "/test/topic", "writer.name", "writer", "codec", "gzip", "outcome", "ok"));
                }
                for (String name : Arrays.asList("buffer.wait.duration", "message.ack.duration")) {
                    meter.assertHistogram(PREFIX + name, "s", 2, COMMON);
                }
            } finally {
                writer.shutdown(1, TimeUnit.SECONDS);
            }
        }
        meter.assertNoCounterSamples(PREFIX + "session.errors");
        meter.assertNoGauges();
    }

    @Test
    public void sessionErrorsFollowRetriesAndStop() throws Exception {
        WriteStreamMock first = new WriteStreamMock();
        WriteStreamMock second = new WriteStreamMock();
        WriteStreamMock third = new WriteStreamMock();
        RetryConfig retries = status -> (number, elapsed) -> number < 2 ? 0 : -1;
        Map<String, String> retry = attributes("topic", "/test/topic", "writer.name", "writer",
                "retry_decision", "retry", "status_code", "TRANSPORT_UNAVAILABLE", "error.type", "transport_error");
        Map<String, String> stop = attributes("topic", "/test/topic", "writer.name", "writer",
                "retry_decision", "stop", "status_code", "TRANSPORT_UNAVAILABLE", "error.type", "transport_error");
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(first, second, third))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(WriterSettings.newBuilder().setTopicPath("/test/topic")
                    .setRetryConfig(retries).withMeter(meter, "writer").build());
            try {
                writer.init();
                first.sendInitResponse(0);
                first.close(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
                second.close(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
                meter.assertCounter(PREFIX + "session.errors", 2, retry);
                meter.assertCounter(PREFIX + "session.errors", 0, stop);
                third.close(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
            } finally {
                CompletableFuture<Void> closed = writer.shutdown();
                first.close(Status.SUCCESS);
                second.close(Status.SUCCESS);
                third.close(Status.SUCCESS);
                closed.get(1, TimeUnit.SECONDS);
            }
        }
        meter.assertCounter(PREFIX + "session.errors", 2, retry);
        meter.assertCounter(PREFIX + "session.errors", 1, stop);
        meter.assertCounterSeries(PREFIX + "session.errors", 2);
        meter.assertNoGauges();
    }

    private static TopicRpc mockRpc(WriteStreamMock first, WriteStreamMock... rest) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.writeSession(Mockito.anyString())).thenReturn(first, rest);
        return rpc;
    }
}
