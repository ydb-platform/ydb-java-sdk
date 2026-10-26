package tech.ydb.topic.write.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.SingleChannelTransport;
import tech.ydb.proto.topic.v1.TopicServiceGrpc;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.InMemoryMeter;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;
import tech.ydb.topic.write.WriteAck;

import static tech.ydb.topic.utils.InMemoryMeter.attributes;

public class WriterMetricsTest {
    private static final String PREFIX = "ydb.topic.writer.";
    private static final Map<String, String> COMMON = attributes("endpoint", "writer.example:2136",
            "database", "/writer-db", "topic", "/test/topic", "writer.name", "writer");
    private final InMemoryMeter meter = new InMemoryMeter();

    @Test
    public void sendAndAcknowledgeMetrics() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        try (GrpcTransport transport = mockTransport(stream);
                TopicClient client = TopicClient.newClient(transport)
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
                        "endpoint", "writer.example:2136", "database", "/writer-db",
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
        try (GrpcTransport transport = mockTransport(stream);
                TopicClient client = TopicClient.newClient(transport)
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
                            "endpoint", "writer.example:2136", "database", "/writer-db",
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
        Map<String, String> retry = attributes("endpoint", "writer.example:2136", "database", "/writer-db",
                "topic", "/test/topic", "writer.name", "writer",
                "retry_decision", "retry", "status_code", "TRANSPORT_UNAVAILABLE", "error.type", "transport_error");
        Map<String, String> stop = attributes("endpoint", "writer.example:2136", "database", "/writer-db",
                "topic", "/test/topic", "writer.name", "writer",
                "retry_decision", "stop", "status_code", "TRANSPORT_UNAVAILABLE", "error.type", "transport_error");
        try (GrpcTransport transport = mockTransport(first, second, third);
                TopicClient client = TopicClient.newClient(transport)
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

    @Test
    public void compressionFailureMetrics() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        IOException problem = new IOException("encode failure");
        Codec codec = Mockito.mock(Codec.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(codec.getId()).thenReturn(Codec.GZIP);
        Mockito.doThrow(problem).when(codec).encode(Mockito.any(OutputStream.class));
        try (GrpcTransport transport = mockTransport(stream);
                TopicClient client = TopicClient.newClient(transport).registerCodec(codec)
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(WriterSettings.newBuilder().setTopicPath("/test/topic")
                    .setCodec(Codec.GZIP).withMeter(meter, "writer").build());
            try {
                writer.init();
                stream.sendInitResponse(0);
                CompletableFuture<WriteAck> first = writer.send(Message.of(new byte[]{1, 2}));
                CompletableFuture<WriteAck> second = writer.send(Message.of(new byte[]{3, 4, 5}));
                Assert.assertSame(problem, Assert.assertThrows(ExecutionException.class,
                        () -> first.get(1, TimeUnit.SECONDS)).getCause());
                Assert.assertSame(problem, Assert.assertThrows(ExecutionException.class,
                        () -> second.get(1, TimeUnit.SECONDS)).getCause());
                meter.assertCounter(PREFIX + "sending.messages", 2, COMMON);
                meter.assertCounter(PREFIX + "sending.bytes", 5, COMMON);
                meter.assertNoCounterSamples(PREFIX + "written.messages");
                meter.assertNoHistogramSamples(PREFIX + "message.ack.duration");
                meter.assertLongGauge(PREFIX + "buffer.used.bytes", 0, COMMON);
                for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
                    meter.assertHistogram(PREFIX + name, "s", 2, attributes(
                            "endpoint", "writer.example:2136", "database", "/writer-db", "topic", "/test/topic",
                            "writer.name", "writer", "codec", "gzip", "outcome", "error",
                            "error.type", "java.io.IOException"));
                    meter.assertHistogram(PREFIX + name, "s", 0, attributes(
                            "endpoint", "writer.example:2136", "database", "/writer-db", "topic", "/test/topic",
                            "writer.name", "writer", "codec", "gzip", "outcome", "ok"));
                }
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
    public void reusedSettingsKeepConnectionAttributes() throws Exception {
        WriteStreamMock first = new WriteStreamMock();
        WriteStreamMock second = new WriteStreamMock();
        WriterSettings settings = WriterSettings.newBuilder().setTopicPath("/test/topic").setCodec(Codec.RAW)
                .withMeter(meter, "writer").build();
        Map<String, String> otherCommon = attributes("endpoint", "other-writer.example:2137",
                "database", "/other-writer-db", "topic", "/test/topic", "writer.name", "writer");
        try (GrpcTransport firstTransport = mockTransport(first);
                GrpcTransport secondTransport = Mockito.spy(new SingleChannelTransport(
                        GrpcTransport.forHost("other-writer.example", 2137, "/other-writer-db")
                                .withScheduler(Mockito.mock(ScheduledExecutorService.class))));
                TopicClient firstClient = TopicClient.newClient(firstTransport)
                        .setCompressionExecutor(Runnable::run).build();
                TopicClient secondClient = TopicClient.newClient(secondTransport)
                        .setCompressionExecutor(Runnable::run).build()) {
            Mockito.doReturn(second).when(secondTransport)
                    .readWriteStreamCall(Mockito.eq(TopicServiceGrpc.getStreamWriteMethod()), Mockito.any());
            AsyncWriter firstWriter = firstClient.createAsyncWriter(settings);
            AsyncWriter secondWriter = secondClient.createAsyncWriter(settings);
            try {
                firstWriter.init();
                secondWriter.init();
                first.sendInitResponse(0);
                second.sendInitResponse(0);
                CompletableFuture<WriteAck> firstAck = firstWriter.send(Message.of(new byte[]{1, 2}));
                CompletableFuture<WriteAck> secondAck = secondWriter.send(Message.of(new byte[]{3, 4, 5}));
                meter.assertCounter(PREFIX + "sending.bytes", 2, COMMON);
                meter.assertCounter(PREFIX + "sending.bytes", 3, otherCommon);
                meter.assertLongGauge(PREFIX + "buffer.used.bytes", 2, COMMON);
                meter.assertLongGauge(PREFIX + "buffer.used.bytes", 3, otherCommon);
                meter.assertCounterSeries(PREFIX + "sending.bytes", 2);
                first.sendAckResponse(1, 10);
                second.sendAckResponse(1, 20);
                firstAck.get(1, TimeUnit.SECONDS);
                secondAck.get(1, TimeUnit.SECONDS);
                Assert.assertSame(meter, settings.getMeter());
            } finally {
                CompletableFuture<Void> firstClosed = firstWriter.shutdown();
                CompletableFuture<Void> secondClosed = secondWriter.shutdown();
                first.close(Status.SUCCESS);
                second.close(Status.SUCCESS);
                firstClosed.get(1, TimeUnit.SECONDS);
                secondClosed.get(1, TimeUnit.SECONDS);
            }
        }
        meter.assertNoGauges();
    }

    private static GrpcTransport mockTransport(WriteStreamMock first, WriteStreamMock... rest) {
        GrpcTransport transport = Mockito.spy(new SingleChannelTransport(
                GrpcTransport.forEndpoint("grpc://writer.example:2136", "/writer-db")
                        .withScheduler(Mockito.mock(ScheduledExecutorService.class))));
        Mockito.doReturn(first, (Object[]) rest).when(transport)
                .readWriteStreamCall(Mockito.eq(TopicServiceGrpc.getStreamWriteMethod()), Mockito.any());
        return transport;
    }
}
