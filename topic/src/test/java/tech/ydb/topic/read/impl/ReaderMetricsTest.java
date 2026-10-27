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
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.DecompressionException;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.utils.InMemoryMeter;

import static tech.ydb.topic.utils.InMemoryMeter.attributes;

public class ReaderMetricsTest {
    private static final String PREFIX = "ydb.topic.reader.";
    private static final byte[] MSG1 = new byte[] { 1, 2 };
    private static final byte[] MSG2 = new byte[] { 3, 4, 5 };
    private static final Map<String, String> COMMON = attributes("consumer", "consumer", "reader.name", "reader");
    private static final Map<String, String> TOPIC = attributes(
            "consumer", "consumer", "reader.name", "reader", "topic", "/topic");
    private static final Map<String, String> OTHER = attributes(
            "consumer", "consumer", "reader.name", "reader", "topic", "/other-topic");
    private final InMemoryMeter meter = new InMemoryMeter();

    @Test
    public void readAndCommitMetrics() throws Exception {
        ReadStreamMock stream = new ReadStreamMock(true);
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream)).build()) {
            SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder()
                    .addTopic("/topic").addTopic("/other-topic").setConsumerName("consumer")
                    .setMaxMemoryUsageBytes(100).setDecompressionExecutor(Runnable::run)
                    .withMeter(meter, "reader").build());
            try {
                reader.init();
                stream.assertLastMessage().isInitRequest("consumer", "/topic", "/other-topic").hasReaderName("reader");
                stream.responseInit("read-session");
                meter.assertLongGaugeSeries(PREFIX + "partition_session.count", 0);
                meter.assertLongGaugeSeries(PREFIX + "commit_offset.lag.max", 0);
                meter.assertLongGauge(PREFIX + "local_buffer.messages", 0, COMMON);
                meter.assertDoubleGauge(PREFIX + "local_buffer.message_age.max", 0, COMMON);
                stream.responseStartPartition("/topic", 42, 0);
                stream.responseStartPartition("/other-topic", 43, 0);
                stream.responseStartPartition("/topic", 44, 0);
                meter.assertLongGauge(PREFIX + "partition_session.count", 2, TOPIC);
                meter.assertLongGauge(PREFIX + "partition_session.count", 1, OTHER);
                meter.assertLongGauge(PREFIX + "credit_balance_bytes", 100, COMMON);
                stream.responseData(12).partition(1, 0).batch(Codec.GZIP, MSG1, MSG2).and().send();
                meter.assertCounter(PREFIX + "received.messages", 2, TOPIC);
                meter.assertLongGauge(PREFIX + "credit_balance_bytes", 88, COMMON);
                stream.responseData(8).partition(2, 0).batch(Codec.GZIP, MSG1).and().send();
                meter.assertCounter(PREFIX + "received.messages", 2, TOPIC);
                meter.assertCounter(PREFIX + "received.messages", 1, OTHER);
                meter.assertCounter(PREFIX + "received.bytes", 20, COMMON);
                meter.assertCounterSeries(PREFIX + "received.bytes", 1);
                meter.assertLongGauge(PREFIX + "credit_balance_bytes", 80, COMMON);
                meter.assertCounter(PREFIX + "delivered.messages", 0, TOPIC);
                meter.assertCounter(PREFIX + "delivered.messages", 0, OTHER);
                meter.assertLongGauge(PREFIX + "local_buffer.messages", 3, COMMON);
                meter.assertPositiveDoubleGauge(PREFIX + "local_buffer.message_age.max", COMMON);
                Message first = reader.receive(0, TimeUnit.SECONDS);
                meter.assertLongGauge(PREFIX + "local_buffer.messages", 2, COMMON);
                Message second = reader.receive(0, TimeUnit.SECONDS);
                meter.assertLongGauge(PREFIX + "local_buffer.messages", 1, COMMON);
                meter.assertPositiveDoubleGauge(PREFIX + "local_buffer.message_age.max", COMMON);
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
                first.getCommitter().commitRanges(Arrays.asList(
                        first.getRangeToCommit(), second.getRangeToCommit()));
                meter.assertLongGauge(PREFIX + "commit_offset.lag.max", 2, TOPIC);
                meter.assertLongGauge(PREFIX + "commit_offset.lag.max", 0, OTHER);
                CompletableFuture<Void> committed = CompletableFuture.allOf(
                        first.commit(), second.commit(), third.commit());
                Assert.assertFalse(committed.isDone());
                meter.assertCounter(PREFIX + "commit.queued", 4, TOPIC);
                meter.assertCounter(PREFIX + "commit.queued", 1, OTHER);
                meter.assertNoCounterSamples(PREFIX + "commit.acknowledged");
                meter.assertLongGauge(PREFIX + "commit_offset.lag.max", 2, TOPIC);
                meter.assertLongGauge(PREFIX + "commit_offset.lag.max", 1, OTHER);
                stream.responseCommitAck().partition(1, 2).send();
                Assert.assertFalse(committed.isDone());
                meter.assertLongGauge(PREFIX + "commit_offset.lag.max", 0, TOPIC);
                meter.assertLongGauge(PREFIX + "commit_offset.lag.max", 1, OTHER);
                stream.responseCommitAck().partition(2, 1).send();
                committed.get(1, TimeUnit.SECONDS);
                meter.assertCounter(PREFIX + "commit.acknowledged", 2, TOPIC);
                meter.assertCounter(PREFIX + "commit.acknowledged", 1, OTHER);
                meter.assertLongGauge(PREFIX + "commit_offset.lag.max", 0, TOPIC);
                meter.assertLongGauge(PREFIX + "commit_offset.lag.max", 0, OTHER);
                for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
                    meter.assertHistogram(PREFIX + name, "s", 2, attributes("consumer", "consumer",
                            "reader.name", "reader", "topic", "/topic", "codec", "gzip", "outcome", "ok"));
                    meter.assertHistogram(PREFIX + name, "s", 1, attributes("consumer", "consumer",
                            "reader.name", "reader", "topic", "/other-topic", "codec", "gzip", "outcome", "ok"));
                }
                meter.assertUnit(PREFIX + "received.bytes", "By");
                stream.responseStopPartition(1, true);
                meter.assertLongGauge(PREFIX + "partition_session.count", 1, TOPIC);
                meter.assertLongGauge(PREFIX + "partition_session.count", 1, OTHER);
                stream.responseStopPartition(2, true);
                stream.responseStopPartition(3, true);
                meter.assertLongGaugeSeries(PREFIX + "partition_session.count", 0);
                meter.assertLongGaugeSeries(PREFIX + "commit_offset.lag.max", 0);
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
        Map<String, String> common = attributes("reader.name", "without-consumer");
        Map<String, String> topic = attributes("reader.name", "without-consumer", "topic", "/topic");
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream)).build()) {
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
                meter.assertLongGauge(PREFIX + "partition_session.count", 1, topic);
                meter.assertLongGauge(PREFIX + "local_buffer.messages", 0, common);
                for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
                    meter.assertHistogram(PREFIX + name, "s", 1, attributes("reader.name", "without-consumer",
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
        Map<String, String> retry = attributes("consumer", "consumer", "reader.name", "reader",
                "retry_decision", "retry", "status_code", "TRANSPORT_UNAVAILABLE", "error.type", "transport_error");
        Map<String, String> stop = attributes("consumer", "consumer", "reader.name", "reader",
                "retry_decision", "stop", "status_code", "TRANSPORT_UNAVAILABLE", "error.type", "transport_error");
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(first, second, third)).build()) {
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
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream)).registerCodec(codec).build()) {
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
                    meter.assertHistogram(PREFIX + name, "s", 2, attributes("consumer", "consumer",
                            "reader.name", "reader", "topic", "/topic", "codec", "gzip", "outcome", "error",
                            "error.type", "java.io.IOException"));
                    meter.assertHistogram(PREFIX + name, "s", 0, attributes("consumer", "consumer",
                            "reader.name", "reader", "topic", "/topic", "codec", "gzip", "outcome", "ok"));
                }
            } finally {
                reader.shutdown();
            }
        }
        meter.assertNoCounterSamples(PREFIX + "session.errors");
        meter.assertNoGauges();
    }

    private static TopicRpc mockRpc(ReadStreamMock first, ReadStreamMock... rest) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(first, rest);
        return rpc;
    }
}
