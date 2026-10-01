package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.Arrays;
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

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;

public class ReaderMetricsTest {
    private static final String DELIVERED = "ydb.topic.reader.delivered.messages";
    private static final String RECEIVED_MESSAGES = "ydb.topic.reader.received.messages";
    private static final String RECEIVED_BYTES = "ydb.topic.reader.received.bytes";
    private static final String PARTITIONS = "ydb.topic.reader.partition_session.count";
    private static final String CREDIT = "ydb.topic.reader.credit_balance_bytes";

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
    public void gaugesObservePartitionSessionsAndProtocolCredit() throws InterruptedException {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(stream).thenReturn(new ReadStreamMock());
        TopicClient client = TopicClientImpl.newClient(rpc).build();
        SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder()
                .addTopic("/topic").setConsumerName("consumer")
                .setMaxMemoryUsageBytes(100).withMeter(meter, "reader").build());
        try {
            Assert.assertTrue(meter.gauges.isEmpty());
            reader.init();
            Assert.assertEquals(0, meter.collect(PARTITIONS));
            Assert.assertEquals(0, meter.collect(CREDIT));
            stream.responseInit("session");
            stream.responseStartPartition("/topic", 42, 0);
            Assert.assertEquals(1, meter.collect(PARTITIONS));
            Assert.assertEquals(100, meter.collect(CREDIT));
            reader.init();
            Assert.assertEquals(1, meter.collect(PARTITIONS));
            Assert.assertEquals(100, meter.collect(CREDIT));

            stream.responseData(20).partition(1, 0).batch(Codec.RAW, new byte[]{1}).and().send();
            Assert.assertEquals(80, meter.collect(CREDIT));
            Assert.assertNotNull(reader.receive(1, TimeUnit.SECONDS));
            Assert.assertEquals(100, meter.collect(CREDIT));
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
            List<Consumer<LongMeasurement>> callbacks = gauges.get(name);
            Assert.assertEquals(1, callbacks.size());
            callbacks.get(0).accept((observed, attrs) -> value[0] = observed);
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
