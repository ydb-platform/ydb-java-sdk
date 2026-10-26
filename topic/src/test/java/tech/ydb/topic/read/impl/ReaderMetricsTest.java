package tech.ydb.topic.read.impl;

import java.io.IOException;
import java.io.InputStream;
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
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.SingleChannelTransport;
import tech.ydb.proto.topic.v1.TopicServiceGrpc;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.read.DecompressionException;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.utils.InMemoryMeter;

import static tech.ydb.topic.utils.InMemoryMeter.attributes;

public class ReaderMetricsTest {
    private static final String PREFIX = "ydb.topic.reader.";
    private static final byte[] MSG1 = new byte[] { 1, 2 };
    private static final byte[] MSG2 = new byte[] { 3, 4, 5 };
    private static final Map<String, String> COMMON = attributes("endpoint", "reader.example:2135",
            "database", "/reader-db", "consumer", "consumer", "reader.name", "reader");
    private static final Map<String, String> TOPIC = attributes(
            "endpoint", "reader.example:2135", "database", "/reader-db",
            "consumer", "consumer", "reader.name", "reader", "topic", "/topic");
    private static final Map<String, String> OTHER = attributes(
            "endpoint", "reader.example:2135", "database", "/reader-db",
            "consumer", "consumer", "reader.name", "reader", "topic", "/other-topic");
    private final InMemoryMeter meter = new InMemoryMeter();

    @Test
    public void readAndCommitMetrics() throws Exception {
        ReadStreamMock stream = new ReadStreamMock(true);
        try (GrpcTransport transport = mockTransport(stream);
                TopicClient client = TopicClient.newClient(transport).build()) {
            SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder()
                    .addTopic("/topic").addTopic("/other-topic").setConsumerName("consumer")
                    .setMaxMemoryUsageBytes(100).setDecompressionExecutor(Runnable::run)
                    .withMeter(meter, "reader").build());
            try {
                reader.init();
                stream.assertLastMessage().isInitRequest("consumer", "/topic", "/other-topic").hasReaderName("reader");
                stream.responseInit("read-session");
                stream.responseStartPartition("/topic", 42, 0);
                stream.responseStartPartition("/other-topic", 43, 0);
                meter.assertLongGauge(PREFIX + "partition_session.count", 2, COMMON);
                meter.assertLongGauge(PREFIX + "credit_balance_bytes", 100, COMMON);
                stream.responseData(12).partition(1, 0).batch(Codec.GZIP, MSG1, MSG2).and().send();
                meter.assertCounter(PREFIX + "received.messages", 2, TOPIC);
                meter.assertLongGauge(PREFIX + "credit_balance_bytes", 88, COMMON);
                stream.responseData(8).partition(2, 0).batch(Codec.GZIP, MSG1).and().send();
                meter.assertCounter(PREFIX + "received.messages", 2, TOPIC);
                meter.assertCounter(PREFIX + "received.messages", 1, OTHER);
                meter.assertCounter(PREFIX + "received.bytes", 20, COMMON);
                meter.assertLongGauge(PREFIX + "credit_balance_bytes", 80, COMMON);
                meter.assertCounter(PREFIX + "delivered.messages", 0, TOPIC);
                meter.assertCounter(PREFIX + "delivered.messages", 0, OTHER);
                meter.assertLongGauge(PREFIX + "local_buffer.messages", 3, COMMON);
                meter.assertPositiveDoubleGauge(PREFIX + "local_buffer.message_age.max", COMMON);
                Message first = reader.receive(0, TimeUnit.SECONDS);
                Message second = reader.receive(0, TimeUnit.SECONDS);
                Message third = reader.receive(0, TimeUnit.SECONDS);
                Assert.assertNotNull(first);
                Assert.assertNotNull(second);
                Assert.assertNotNull(third);
                Assert.assertArrayEquals(MSG1, first.getData());
                Assert.assertArrayEquals(MSG2, second.getData());
                Assert.assertArrayEquals(MSG1, third.getData());
                meter.assertCounter(PREFIX + "delivered.messages", 2, TOPIC);
                meter.assertCounter(PREFIX + "delivered.messages", 1, OTHER);
                meter.assertLongGauge(PREFIX + "local_buffer.messages", 0, COMMON);
                meter.assertDoubleGauge(PREFIX + "local_buffer.message_age.max", 0, COMMON);
                // The final 8 released bytes stay below the 10-byte credit-request threshold.
                meter.assertLongGauge(PREFIX + "credit_balance_bytes", 92, COMMON);
                CompletableFuture<Void> committed = CompletableFuture.allOf(
                        first.commit(), second.commit(), third.commit());
                Assert.assertFalse(committed.isDone());
                meter.assertCounter(PREFIX + "commit.queued", 2, TOPIC);
                meter.assertCounter(PREFIX + "commit.queued", 1, OTHER);
                meter.assertNoCounterSamples(PREFIX + "commit.acknowledged");
                meter.assertLongGauge(PREFIX + "commit_offset.lag.max", 2, COMMON);
                stream.responseCommitAck().partition(1, 2).partition(2, 1).send();
                committed.get(1, TimeUnit.SECONDS);
                meter.assertCounter(PREFIX + "commit.acknowledged", 2, TOPIC);
                meter.assertCounter(PREFIX + "commit.acknowledged", 1, OTHER);
                meter.assertLongGauge(PREFIX + "commit_offset.lag.max", 0, COMMON);
                for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
                    meter.assertHistogram(PREFIX + name, "s", 2, attributes(
                            "endpoint", "reader.example:2135", "database", "/reader-db", "consumer", "consumer",
                            "reader.name", "reader", "topic", "/topic", "codec", "gzip", "outcome", "ok"));
                    meter.assertHistogram(PREFIX + name, "s", 1, attributes(
                            "endpoint", "reader.example:2135", "database", "/reader-db", "consumer", "consumer",
                            "reader.name", "reader", "topic", "/other-topic", "codec", "gzip", "outcome", "ok"));
                }
                meter.assertUnit(PREFIX + "received.bytes", "By");
                stream.responseStopPartition(1, true);
                stream.responseStopPartition(2, true);
                meter.assertLongGauge(PREFIX + "partition_session.count", 0, COMMON);
            } finally {
                reader.shutdown();
            }
        }
        meter.assertNoCounterSamples(PREFIX + "session.errors");
        meter.assertNoGauges();
    }

    @Test
    public void readWithoutConsumerMetrics() throws Exception {
        ReadStreamMock stream = new ReadStreamMock(true);
        Map<String, String> common = attributes("endpoint", "reader.example:2135", "database", "/reader-db",
                "reader.name", "without-consumer");
        Map<String, String> topic = attributes("endpoint", "reader.example:2135", "database", "/reader-db",
                "reader.name", "without-consumer", "topic", "/topic");
        try (GrpcTransport transport = mockTransport(stream);
                TopicClient client = TopicClient.newClient(transport).build()) {
            SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder().addTopic("/topic")
                    .withoutConsumer().setDecompressionExecutor(Runnable::run)
                    .withMeter(meter, "without-consumer").build());
            try {
                reader.init();
                stream.assertLastMessage().isInitRequest(null, "/topic").hasReaderName("without-consumer");
                stream.responseInit("without-consumer-session");
                stream.responseStartPartition("/topic", 42, 0);
                stream.responseData(8).partition(1, 0).batch(Codec.GZIP, MSG1).and().send();
                meter.assertCounter(PREFIX + "received.messages", 1, topic);
                meter.assertCounter(PREFIX + "received.bytes", 8, common);
                Message message = reader.receive(0, TimeUnit.SECONDS);
                Assert.assertNotNull(message);
                Assert.assertArrayEquals(MSG1, message.getData());
                meter.assertCounter(PREFIX + "delivered.messages", 1, topic);
                meter.assertLongGauge(PREFIX + "partition_session.count", 1, common);
                meter.assertLongGauge(PREFIX + "local_buffer.messages", 0, common);
                for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
                    meter.assertHistogram(PREFIX + name, "s", 1, attributes(
                            "endpoint", "reader.example:2135", "database", "/reader-db", "reader.name", "without-consumer",
                            "topic", "/topic", "codec", "gzip", "outcome", "ok"));
                }
            } finally {
                reader.shutdown();
            }
        }
        meter.assertNoCounterSamples(PREFIX + "session.errors");
        meter.assertNoGauges();
    }

    @Test
    public void sessionErrorsFollowRetriesAndStop() throws Exception {
        ReadStreamMock first = new ReadStreamMock(true);
        ReadStreamMock second = new ReadStreamMock(true);
        ReadStreamMock third = new ReadStreamMock(true);
        RetryConfig retries = status -> (number, elapsed) -> number < 2 ? 0 : -1;
        Map<String, String> retry = attributes("endpoint", "reader.example:2135", "database", "/reader-db",
                "consumer", "consumer", "reader.name", "reader",
                "retry_decision", "retry", "status_code", "TRANSPORT_UNAVAILABLE", "error.type", "transport_error");
        Map<String, String> stop = attributes("endpoint", "reader.example:2135", "database", "/reader-db",
                "consumer", "consumer", "reader.name", "reader",
                "retry_decision", "stop", "status_code", "TRANSPORT_UNAVAILABLE", "error.type", "transport_error");
        try (GrpcTransport transport = mockTransport(first, second, third);
                TopicClient client = TopicClient.newClient(transport).build()) {
            SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder().addTopic("/topic")
                    .setConsumerName("consumer").setRetryConfig(retries).withMeter(meter, "reader").build());
            try {
                reader.init();
                first.responseInit("first-session");
                first.closeStream(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
                second.closeStream(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
                third.assertLastMessage().isInitRequest("consumer", "/topic");
                meter.assertCounter(PREFIX + "session.errors", 2, retry);
                meter.assertCounter(PREFIX + "session.errors", 0, stop);
                third.closeStream(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
            } finally {
                reader.shutdown();
            }
        }
        meter.assertCounter(PREFIX + "session.errors", 2, retry);
        meter.assertCounter(PREFIX + "session.errors", 1, stop);
        meter.assertCounterSeries(PREFIX + "session.errors", 2);
        meter.assertNoGauges();
    }

    @Test
    public void decompressionFailureMetrics() throws Exception {
        ReadStreamMock stream = new ReadStreamMock(true);
        IOException problem = new IOException("decode failure");
        Codec codec = Mockito.mock(Codec.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(codec.getId()).thenReturn(Codec.GZIP);
        Mockito.doThrow(problem).when(codec).decode(Mockito.any(InputStream.class));
        try (GrpcTransport transport = mockTransport(stream);
                TopicClient client = TopicClient.newClient(transport).registerCodec(codec).build()) {
            SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder().addTopic("/topic")
                    .setConsumerName("consumer").setDecompressionExecutor(Runnable::run)
                    .withMeter(meter, "reader").build());
            try {
                reader.init();
                stream.responseInit("decode-failure-session");
                stream.responseStartPartition("/topic", 42, 0);
                stream.responseData(20).partition(1, 0).batch(Codec.GZIP, MSG1, MSG2).and().send();
                for (int i = 0; i < 2; i++) {
                    Message message = reader.receive(0, TimeUnit.SECONDS);
                    Assert.assertNotNull(message);
                    DecompressionException error = Assert.assertThrows(DecompressionException.class, message::getData);
                    Assert.assertSame(problem, error.getCause());
                }
                meter.assertCounter(PREFIX + "received.messages", 2, TOPIC);
                meter.assertCounter(PREFIX + "delivered.messages", 2, TOPIC);
                meter.assertNoCounterSamples(PREFIX + "commit.acknowledged");
                for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
                    meter.assertHistogram(PREFIX + name, "s", 2, attributes(
                            "endpoint", "reader.example:2135", "database", "/reader-db", "consumer", "consumer",
                            "reader.name", "reader", "topic", "/topic", "codec", "gzip", "outcome", "error",
                            "error.type", "java.io.IOException"));
                    meter.assertHistogram(PREFIX + name, "s", 0, attributes(
                            "endpoint", "reader.example:2135", "database", "/reader-db", "consumer", "consumer",
                            "reader.name", "reader", "topic", "/topic", "codec", "gzip", "outcome", "ok"));
                }
            } finally {
                reader.shutdown();
            }
        }
        meter.assertNoCounterSamples(PREFIX + "session.errors");
        meter.assertNoGauges();
    }

    @Test
    public void reusedSettingsKeepConnectionAttributes() throws Exception {
        ReadStreamMock first = new ReadStreamMock(true);
        ReadStreamMock second = new ReadStreamMock(true);
        ReaderSettings settings = ReaderSettings.newBuilder().addTopic("/topic").setConsumerName("consumer")
                .setDecompressionExecutor(Runnable::run).withMeter(meter, "reader").build();
        CompletableFuture<Message> delivered = new CompletableFuture<>();
        Map<String, String> otherCommon = attributes("endpoint", "other-reader.example:2137",
                "database", "/other-reader-db", "consumer", "consumer", "reader.name", "reader");
        try (GrpcTransport firstTransport = mockTransport(first);
                GrpcTransport secondTransport = Mockito.spy(new SingleChannelTransport(
                        GrpcTransport.forHost("other-reader.example", 2137, "/other-reader-db")
                                .withScheduler(Mockito.mock(ScheduledExecutorService.class))));
                TopicClient firstClient = TopicClient.newClient(firstTransport).build();
                TopicClient secondClient = TopicClient.newClient(secondTransport).build()) {
            Mockito.doReturn(second).when(secondTransport)
                    .readWriteStreamCall(Mockito.eq(TopicServiceGrpc.getStreamReadMethod()), Mockito.any());
            SyncReader sync = firstClient.createSyncReader(settings);
            AsyncReader async = secondClient.createAsyncReader(settings, ReadEventHandlersSettings.newBuilder()
                    .setExecutor(Runnable::run).setEventHandler(event -> delivered.complete(event.getMessages().get(0)))
                    .build());
            try {
                sync.init();
                async.init();
                first.responseInit("first");
                second.responseInit("second");
                first.responseStartPartition("/topic", 42, 0);
                second.responseStartPartition("/topic", 43, 0);
                first.responseData(12).partition(1, 0).batch(Codec.GZIP, MSG1).and().send();
                second.responseData(8).partition(1, 0).batch(Codec.GZIP, MSG2).and().send();
                Assert.assertArrayEquals(MSG1, sync.receive(0, TimeUnit.SECONDS).getData());
                Assert.assertArrayEquals(MSG2, delivered.get(1, TimeUnit.SECONDS).getData());
                meter.assertCounter(PREFIX + "received.bytes", 12, COMMON);
                meter.assertCounter(PREFIX + "received.bytes", 8, otherCommon);
                meter.assertLongGauge(PREFIX + "partition_session.count", 1, COMMON);
                meter.assertLongGauge(PREFIX + "partition_session.count", 1, otherCommon);
                meter.assertCounterSeries(PREFIX + "received.bytes", 2);
                Assert.assertSame(meter, settings.getMeter());
            } finally {
                sync.shutdown();
                async.shutdown().get(1, TimeUnit.SECONDS);
            }
        }
        meter.assertNoGauges();
    }

    private static GrpcTransport mockTransport(ReadStreamMock first, ReadStreamMock... rest) {
        GrpcTransport transport = Mockito.spy(new SingleChannelTransport(
                GrpcTransport.forEndpoint("grpcs://reader.example:2135", "/reader-db")
                        .withScheduler(Mockito.mock(ScheduledExecutorService.class))));
        Mockito.doReturn(first, (Object[]) rest).when(transport)
                .readWriteStreamCall(Mockito.eq(TopicServiceGrpc.getStreamReadMethod()), Mockito.any());
        return transport;
    }
}
