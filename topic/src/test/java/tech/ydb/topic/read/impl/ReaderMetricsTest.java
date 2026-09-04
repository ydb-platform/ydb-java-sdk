package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;

public class ReaderMetricsTest {
    private static final String DELIVERED = "ydb.topic.reader.delivered.messages";

    private static final String TOPIC = "/topic";
    private static final String CONSUMER = "consumer";
    private static final String READER_NAME = "reader-name";

    @Test
    public void instrumentIsCreated() {
        RecordingMeter meter = new RecordingMeter();

        new ReaderMetrics(meter, CONSUMER, null);

        Assert.assertEquals(Arrays.asList(DELIVERED), new ArrayList<>(meter.instruments.keySet()));
        assertInstrument(meter, DELIVERED,
                "The number of messages delivered by the SDK to application code.");
    }

    @Test
    public void configuredReaderNameIsReported() {
        RecordingMeter meter = new RecordingMeter();
        ReaderMetrics metrics = new ReaderMetrics(meter, CONSUMER, READER_NAME);

        metrics.reportDelivered(1, TOPIC);

        assertLabels(meter, DELIVERED, READER_NAME);
    }

    @Test
    public void missingReaderNameIsNotReported() {
        for (String readerName : Arrays.asList(null, "")) {
            RecordingMeter meter = new RecordingMeter();
            ReaderMetrics metrics = new ReaderMetrics(meter, CONSUMER, readerName);

            metrics.reportDelivered(1, TOPIC);

            assertLabels(meter, DELIVERED, null);
        }
    }

    @Test
    public void missingConsumerIsNotReported() {
        RecordingMeter meter = new RecordingMeter();

        new ReaderMetrics(meter, null, null).reportDelivered(1, TOPIC);

        Measurement measurement = meter.measurements(DELIVERED).get(0);
        Assert.assertEquals(1, measurement.attrs.length);
        Assert.assertEquals(TOPIC, attrValue(measurement.attrs, "topic"));
        Assert.assertNull(attrValue(measurement.attrs, "consumer"));
    }

    @Test
    public void asyncReaderRecordsDeliveredBeforeCallback() {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        int[] callbacks = { 0 };

        TopicClient client = TopicClientImpl.newClient(mockRpc(stream)).withMeter(meter).build();
        AsyncReader reader = client.createAsyncReader(readerSettings(), ReadEventHandlersSettings.newBuilder()
                .setExecutor(Runnable::run)
                .setEventHandler(event -> {
                    callbacks[0]++;
                    Assert.assertEquals(2, meter.total(DELIVERED));
                })
                .build());

        try {
            reader.init();
            stream.responseInit("read-session");
            stream.responseStartPartition(TOPIC, 42);
            stream.responseData(50).partition(1, -1)
                    .batch(Codec.RAW, new byte[] { 1 }, new byte[] { 2 })
                    .and().send();

            Assert.assertEquals(1, callbacks[0]);
            assertLabels(meter, DELIVERED);
        } finally {
            reader.shutdown().join();
            client.close();
        }
    }

    @Test
    public void syncReaderRecordsDeliveredOnlyWhenReceiveReturnsMessage() throws InterruptedException {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        TopicClient client = TopicClientImpl.newClient(mockRpc(stream)).withMeter(meter).build();
        SyncReader reader = client.createSyncReader(readerSettings());

        try {
            reader.init();
            stream.responseInit("read-session");
            stream.responseStartPartition(TOPIC, 42);
            stream.responseData(25).partition(1, -1)
                    .batch(Codec.RAW, new byte[] { 1 })
                    .and().send();

            Assert.assertEquals(0, meter.total(DELIVERED));

            Message message = reader.receive();

            Assert.assertNotNull(message);
            Assert.assertEquals(1, meter.total(DELIVERED));
            assertLabels(meter, DELIVERED);
        } finally {
            reader.shutdown();
            client.close();
        }
    }

    private static ReaderSettings readerSettings() {
        return ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TOPIC).build())
                .setConsumerName(CONSUMER)
                .build();
    }

    private static TopicRpc mockRpc(ReadStreamMock stream) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.any(String.class))).thenReturn(stream);
        return rpc;
    }

    private static void assertInstrument(RecordingMeter meter, String name, String description) {
        Instrument instrument = meter.instruments.get(name);
        Assert.assertNotNull(instrument);
        Assert.assertEquals("{message}", instrument.unit);
        Assert.assertEquals(description, instrument.description);
    }

    private static void assertLabels(RecordingMeter meter, String name) {
        assertLabels(meter, name, null);
    }

    private static void assertLabels(RecordingMeter meter, String name, String readerName) {
        List<Measurement> measurements = meter.measurements(name);
        Assert.assertFalse(name + " has no measurements", measurements.isEmpty());
        for (Measurement measurement : measurements) {
            Assert.assertEquals(readerName == null ? 2 : 3, measurement.attrs.length);
            Assert.assertEquals(TOPIC, attrValue(measurement.attrs, "topic"));
            Assert.assertEquals(CONSUMER, attrValue(measurement.attrs, "consumer"));
            Assert.assertEquals(readerName, attrValue(measurement.attrs, "reader.name"));
        }
    }

    private static String attrValue(Attr[] attrs, String key) {
        for (Attr attr : attrs) {
            if (key.equals(attr.getKey())) {
                return attr.getValue();
            }
        }
        return null;
    }

    private static class RecordingMeter implements Meter {
        private final Map<String, Instrument> instruments = new LinkedHashMap<>();
        private final List<Measurement> measurements = new ArrayList<>();

        @Override
        public LongCounter createCounter(String name, String unit, String description) {
            instruments.put(name, new Instrument(unit, description));
            return (value, attrs) -> measurements.add(new Measurement(name, value, attrs));
        }

        long total(String name) {
            return measurements(name).stream().mapToLong(measurement -> measurement.value).sum();
        }

        List<Measurement> measurements(String name) {
            List<Measurement> result = new ArrayList<>();
            for (Measurement measurement : measurements) {
                if (name.equals(measurement.name)) {
                    result.add(measurement);
                }
            }
            return result;
        }
    }

    private static class Instrument {
        private final String unit;
        private final String description;

        Instrument(String unit, String description) {
            this.unit = unit;
            this.description = description;
        }
    }

    private static class Measurement {
        private final String name;
        private final long value;
        private final Attr[] attrs;

        Measurement(String name, long value, Attr[] attrs) {
            this.name = name;
            this.value = value;
            this.attrs = attrs;
        }
    }
}
