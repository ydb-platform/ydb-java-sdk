package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleMeasurement;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;

public class ReaderMetricsTest {
    private static final String DELIVERED = "ydb.topic.reader.delivered.messages";
    private static final String RECEIVED_MESSAGES = "ydb.topic.reader.received.messages";
    private static final String RECEIVED_BYTES = "ydb.topic.reader.received.bytes";
    private static final String PARTITIONS = "ydb.topic.reader.partition_session.count";
    private static final String CREDIT = "ydb.topic.reader.credit_balance_bytes";
    private static final String BUFFER = "ydb.topic.reader.local_buffer.messages";
    private static final String AGE = "ydb.topic.reader.local_buffer.message_age.max";
    private static final String COMMIT_LAG = "ydb.topic.reader.commit_offset.lag.max";

    @Test
    public void readerCountersIncrementOnReceive() throws InterruptedException {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.any(String.class))).thenReturn(stream);

        TopicClient client = TopicClientImpl.newClient(rpc).build();
        SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath("/topic").build())
                .setConsumerName("consumer")
                .withMeter(meter, "reader")
                .build());

        try {
            reader.init();
            stream.assertLastMessage().isInitRequest("consumer", "/topic").hasReaderName("reader");
            stream.responseInit("read-session");
            stream.responseStartPartition("/topic", 42, 0);
            stream.responseData(25).partition(1, 0)
                    .batch(Codec.RAW, new byte[]{1})
                    .batch(Codec.RAW, new byte[]{2})
                    .and().send();

            Assert.assertEquals(2, meter.value(RECEIVED_MESSAGES));
            Assert.assertEquals(25, meter.value(RECEIVED_BYTES));
            Assert.assertEquals(0, meter.value(DELIVERED));
            meter.assertAttribute(RECEIVED_MESSAGES, "topic", "/topic");
            meter.assertAttribute(RECEIVED_MESSAGES, "reader.name", "reader");

            reader.receive();
            reader.receive();
            Assert.assertEquals(2, meter.value(DELIVERED));
        } finally {
            reader.shutdown();
            client.close();
        }
    }

    @Test
    public void bufferGaugesIncludePartialZeroByteBatchesAndDropStoppedPartitions() throws InterruptedException {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(stream);
        TopicClient client = TopicClientImpl.newClient(rpc).build();
        SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder()
                .addTopic("/topic").setConsumerName("consumer").setMaxBatchSize(1)
                .withMeter(meter, "reader").build());
        try {
            reader.init();
            Assert.assertTrue(meter.doubleGauges.isEmpty());
            stream.responseInit("session");
            Assert.assertEquals(0, meter.collect(BUFFER));
            Assert.assertEquals(0, meter.collectDouble(AGE), 0);
            stream.responseStartPartition("/topic", 42, 0);
            stream.responseStartPartition("/topic", 43, 0);
            stream.responseData(0).partition(1, 0).batch(Codec.RAW, new byte[0], new byte[0]).and().send();
            stream.responseData(5).partition(2, 100).batch(Codec.RAW, new byte[]{1}).and().send();
            Assert.assertEquals(3, meter.collect(BUFFER));
            Assert.assertTrue(meter.collectDouble(AGE) >= 0);
            meter.assertAttribute(BUFFER, "reader.name", "reader");
            meter.assertAttribute(AGE, "consumer", "consumer");
            Assert.assertEquals(0, reader.receive(1, TimeUnit.SECONDS).getOffset());
            Assert.assertEquals(2, meter.collect(BUFFER));
            Assert.assertEquals(1, reader.receive(1, TimeUnit.SECONDS).getOffset());
            Assert.assertEquals(1, meter.collect(BUFFER));
            stream.responseStopPartition(2, false);
            Assert.assertEquals(0, meter.collect(BUFFER));
            Assert.assertEquals(0, meter.collectDouble(AGE), 0);
            stream.responseData(10).partition(2, 101).batch(Codec.RAW, new byte[]{2}).and().send();
            Assert.assertEquals(0, meter.collect(BUFFER));
        } finally {
            reader.shutdown();
            client.close();
        }
        Assert.assertTrue(meter.gauges.isEmpty());
        Assert.assertTrue(meter.doubleGauges.isEmpty());
    }

    @Test
    public void commitLagTracksSingleAndBulkRequestsAndMaximumAcrossPartitions() throws InterruptedException {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(stream);
        TopicClient client = TopicClientImpl.newClient(rpc).build();
        SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder()
                .addTopic("/topic").setConsumerName("consumer").withMeter(meter, "reader").build());
        try {
            reader.init();
            stream.responseInit("session");
            stream.responseStartPartition("/topic", 42, 10);
            stream.responseStartPartition("/topic", 43, 100);
            Assert.assertEquals(0, meter.collect(COMMIT_LAG));
            stream.responseData(5).partition(1, 30).batch(Codec.RAW, new byte[]{1}).and().send();
            Message first = reader.receive(1, TimeUnit.SECONDS);
            first.commit();
            Assert.assertEquals(21, meter.collect(COMMIT_LAG));
            stream.responseCommitAck().partition(1, 20).send();
            Assert.assertEquals(11, meter.collect(COMMIT_LAG));
            first.getCommitter().commitRanges(Collections.singletonList(OffsetsRange.of(70, 80)));
            first.getCommitter().commitRanges(Collections.singletonList(OffsetsRange.of(40, 42)));
            Assert.assertEquals(60, meter.collect(COMMIT_LAG));
            stream.responseData(5).partition(2, 105).batch(Codec.RAW, new byte[]{2}).and().send();
            Message second = reader.receive(1, TimeUnit.SECONDS);
            second.commit();
            Assert.assertEquals(60, meter.collect(COMMIT_LAG));
            stream.responseCommitAck().partition(1, 80).send();
            Assert.assertEquals(6, meter.collect(COMMIT_LAG));
            stream.responseCommitAck().partition(2, 110).send();
            Assert.assertEquals(0, meter.collect(COMMIT_LAG));
            first.getCommitter().commitRanges(Collections.singletonList(OffsetsRange.of(120, 130)));
            Assert.assertEquals(50, meter.collect(COMMIT_LAG));
            stream.responseStopPartition(1, true);
            Assert.assertEquals(0, meter.collect(COMMIT_LAG));
            first.getCommitter().commitRanges(Collections.singletonList(OffsetsRange.of(150, 160)));
            Assert.assertEquals(0, meter.collect(COMMIT_LAG));
        } finally {
            reader.shutdown();
            client.close();
        }
    }

    @Test
    public void asyncBufferIncludesWaitingDecompressionAndEndsAtCallbackDelivery() {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        List<Runnable> decompression = new ArrayList<>();
        int[] delivered = {0};
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(stream);
        TopicClient client = TopicClientImpl.newClient(rpc).build();
        AsyncReader reader = client.createAsyncReader(ReaderSettings.newBuilder()
                .addTopic("/topic").setConsumerName("consumer").setDecompressionExecutor(decompression::add)
                .withMeter(meter, "reader").build(), ReadEventHandlersSettings.newBuilder()
                        .setExecutor(Runnable::run).setEventHandler(event -> {
                            delivered[0] += event.getMessages().size();
                            Assert.assertEquals(0, meter.collect(BUFFER));
                            Assert.assertEquals(0, meter.collectDouble(AGE), 0);
                        }).build());
        try {
            reader.init();
            stream.responseInit("session");
            stream.responseStartPartition("/topic", 42, 0);
            stream.responseData(30).partition(1, 0).batch(Codec.GZIP, new byte[]{1}).and().send();
            Assert.assertEquals(1, decompression.size());
            Assert.assertEquals(1, meter.collect(BUFFER));
            Assert.assertEquals(0, delivered[0]);
            decompression.remove(0).run();
            Assert.assertEquals(1, delivered[0]);
            Assert.assertEquals(0, meter.collect(BUFFER));
        } finally {
            reader.shutdown();
            client.close();
        }
        Assert.assertTrue(meter.doubleGauges.isEmpty());
    }

    @Test
    public void gaugesObservePartitionSessionsAndProtocolCredit() throws InterruptedException {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(stream);
        TopicClient client = TopicClientImpl.newClient(rpc).build();
        SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder()
                .addTopic("/topic").setConsumerName("consumer")
                .setMaxMemoryUsageBytes(100).withMeter(meter, "reader").build());
        try {
            Assert.assertTrue(meter.gauges.isEmpty());
            reader.init();
            Assert.assertTrue(meter.gauges.isEmpty());
            stream.responseInit("session");
            Assert.assertEquals(0, meter.collect(PARTITIONS));
            Assert.assertEquals(100, meter.collect(CREDIT));
            stream.responseStartPartition("/topic", 42, 0);
            Assert.assertEquals(1, meter.collect(PARTITIONS));
            Assert.assertEquals(100, meter.collect(CREDIT));
            stream.responseData(20).partition(1, 0).batch(Codec.RAW, new byte[]{1}).and().send();
            Assert.assertEquals(80, meter.collect(CREDIT));
            Assert.assertNotNull(reader.receive(1, TimeUnit.SECONDS));
            Assert.assertEquals(100, meter.collect(CREDIT));
            stream.closeStream(Status.of(StatusCode.OVERLOADED));
            Assert.assertTrue(meter.gauges.isEmpty());
            Assert.assertTrue(meter.doubleGauges.isEmpty());
        } finally {
            reader.shutdown();
            client.close();
        }
        Assert.assertTrue(meter.gauges.isEmpty());
    }

    private static class RecordingMeter implements Meter {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        private final Map<String, Attr[]> attributes = new ConcurrentHashMap<>();
        private final Map<String, List<Consumer<LongMeasurement>>> gauges = new ConcurrentHashMap<>();
        private final Map<String, Consumer<DoubleMeasurement>> doubleGauges = new ConcurrentHashMap<>();

        @Override
        public MetricRegistration registerDoubleGauge(
                String name, String unit, String description, Consumer<DoubleMeasurement> callback) {
            doubleGauges.put(name, callback);
            return () -> doubleGauges.remove(name, callback);
        }

        double collectDouble(String name) {
            double[] value = new double[1];
            Assert.assertNotNull(doubleGauges.get(name));
            doubleGauges.get(name).accept((observed, attrs) -> {
                value[0] = observed;
                attributes.put(name, attrs);
            });
            return value[0];
        }

        @Override
        public MetricRegistration registerLongGauge(
                String name, String unit, String description, Consumer<LongMeasurement> callback) {
            List<Consumer<LongMeasurement>> callbacks = gauges.computeIfAbsent(name, key -> new ArrayList<>());
            callbacks.add(callback);
            return () -> {
                callbacks.remove(callback);
                if (callbacks.isEmpty()) {
                    gauges.remove(name, callbacks);
                }
            };
        }

        long collect(String name) {
            Long[] value = new Long[1];
            Assert.assertEquals(1, gauges.get(name).size());
            gauges.get(name).get(0).accept((observed, attrs) -> {
                value[0] = observed;
                attributes.put(name, attrs);
            });
            return value[0];
        }

        @Override
        public LongCounter createCounter(String name, String unit, String description) {
            AtomicLong counter = counters.computeIfAbsent(name, key -> new AtomicLong());
            return (value, attrs) -> {
                counter.addAndGet(value);
                attributes.put(name, attrs);
            };
        }

        long value(String name) {
            AtomicLong counter = counters.get(name);
            return counter == null ? 0 : counter.get();
        }

        void assertAttribute(String name, String key, String value) {
            Assert.assertTrue(Arrays.stream(attributes.get(name))
                    .anyMatch(attr -> attr.getKey().equals(key) && attr.getValue().equals(value)));
        }
    }
}
