package tech.ydb.topic.read.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;

public class ReaderStateGaugesTest {
    private static final String PARTITIONS = "ydb.topic.reader.partition_session.count";
    private static final String CREDIT = "ydb.topic.reader.credit_balance_bytes";

    @Test
    public void gaugesObservePartitionSessionsAndProtocolCredit() throws InterruptedException {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        SyncReader reader = new SyncReaderImpl(mockRpc(stream), settings(meter).build(), new CodecRegistry());
        try {
            meter.assertValue(PARTITIONS, 0);
            meter.assertValue(CREDIT, 0);
            reader.init();
            meter.assertValue(CREDIT, 0);
            stream.responseInit("session");
            stream.assertLastMessage().isReadRequest(100);
            meter.assertValue(CREDIT, 100);
            stream.responseStartPartition("/topic", 42, 0);
            stream.responseStartPartition("/topic", 43, 0);
            meter.assertValue(PARTITIONS, 2);
            stream.responseStopPartition(2, false);
            meter.assertValue(PARTITIONS, 1);

            stream.responseData(5).partition(1, 0).batch(Codec.RAW, new byte[]{1}, new byte[]{2}).and().send();
            meter.assertValue(CREDIT, 95);
            Assert.assertNotNull(reader.receive(1, TimeUnit.SECONDS));
            Assert.assertNotNull(reader.receive(1, TimeUnit.SECONDS));
            meter.assertValue(CREDIT, 95); // Released bytes below the request threshold grant no new credit.
            stream.responseData(15).partition(1, 2).batch(Codec.RAW, new byte[]{3}).and().send();
            meter.assertValue(CREDIT, 80);
            Assert.assertNotNull(reader.receive(1, TimeUnit.SECONDS));
            stream.assertLastMessage().isReadRequest(20);
            meter.assertValue(CREDIT, 100);
            stream.responseStopPartition(1, false);
            meter.assertValue(PARTITIONS, 0);
            Assert.assertEquals("{session}", meter.units.get(PARTITIONS));
            Assert.assertEquals("By", meter.units.get(CREDIT));
            for (String name : Arrays.asList(PARTITIONS, CREDIT)) {
                meter.assertAttribute(name, "consumer", "consumer");
                meter.assertAttribute(name, "reader.name", "reader");
                Assert.assertEquals(2, meter.attributes.get(name).length);
            }
        } finally {
            reader.shutdown();
        }
        meter.assertAbsent(PARTITIONS);
        meter.assertAbsent(CREDIT);
    }

    @Test
    public void gaugesFollowReconnectedSessionWithoutRegisteringAgain() {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock first = new ReadStreamMock();
        ReadStreamMock second = new ReadStreamMock();
        SyncReader reader = new SyncReaderImpl(mockRpc(first, second),
                settings(meter).setRetryConfig(status -> (number, elapsed) -> 0).build(), new CodecRegistry());
        try {
            reader.init();
            first.responseInit("first");
            first.responseStartPartition("/topic", 42, 0);
            first.responseData(5).partition(1, 0).batch(Codec.RAW, new byte[]{1}).and().send();
            meter.assertValue(PARTITIONS, 1);
            meter.assertValue(CREDIT, 95);
            first.closeStream(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
            second.assertLastMessage().isInitRequest("consumer", "/topic");
            meter.assertValue(PARTITIONS, 0);
            meter.assertValue(CREDIT, 0);
            second.responseInit("second");
            second.responseStartPartition("/topic", 42, 0);
            meter.assertValue(PARTITIONS, 1);
            meter.assertValue(CREDIT, 100);
            Assert.assertEquals(2, meter.registrations);
        } finally {
            reader.shutdown();
        }
        meter.assertAbsent(PARTITIONS);
        meter.assertAbsent(CREDIT);
    }

    @Test
    public void asyncGaugesIncludePartitionAwaitingGracefulStopConfirmation() throws Exception {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        AtomicReference<StopPartitionSessionEvent> stop = new AtomicReference<>();
        ReadEventHandler handler = new ReadEventHandler() {
            @Override
            public void onMessages(DataReceivedEvent event) { }

            @Override
            public void onStopPartitionSession(StopPartitionSessionEvent event) {
                stop.set(event);
            }
        };
        AsyncReader reader = new AsyncReaderImpl(mockRpc(stream), settings(meter).build(),
                ReadEventHandlersSettings.newBuilder().setEventHandler(handler).setExecutor(Runnable::run).build(),
                new CodecRegistry());
        try {
            meter.assertValue(PARTITIONS, 0);
            meter.assertValue(CREDIT, 0);
            reader.init();
            stream.responseInit("session");
            stream.responseStartPartition("/topic", 42, 0);
            meter.assertValue(PARTITIONS, 1);
            meter.assertValue(CREDIT, 100);
            stream.responseStopPartition(1, true);
            Assert.assertNotNull(stop.get());
            meter.assertValue(PARTITIONS, 1);
            stop.get().confirm();
            meter.assertValue(PARTITIONS, 0);
        } finally {
            reader.shutdown().get(1, TimeUnit.SECONDS);
        }
        meter.assertAbsent(PARTITIONS);
        meter.assertAbsent(CREDIT);
    }

    private static ReaderSettings.Builder settings(Meter meter) {
        return ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath("/topic").build())
                .setConsumerName("consumer").setMaxMemoryUsageBytes(100).withMeter(meter, "reader");
    }

    private static TopicRpc mockRpc(ReadStreamMock first, ReadStreamMock... rest) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(first, rest);
        return rpc;
    }

    private static class RecordingMeter implements Meter {
        private final Map<String, Consumer<LongMeasurement>> gauges = new HashMap<>();
        private final Map<String, String> units = new HashMap<>();
        private final Map<String, Attr[]> attributes = new HashMap<>();
        private int registrations;

        @Override
        public void createLongGauge(String name, String unit, String description, Consumer<LongMeasurement> callback) {
            registrations++;
            gauges.put(name, callback);
            units.put(name, unit);
        }

        private Long collect(String name) {
            Long[] value = new Long[1];
            gauges.get(name).accept((observed, attrs) -> {
                Assert.assertNull("Multiple measurements for one reader", value[0]);
                value[0] = observed;
                attributes.put(name, attrs);
            });
            return value[0];
        }

        void assertValue(String name, long value) {
            Assert.assertEquals(Long.valueOf(value), collect(name));
        }

        void assertAbsent(String name) {
            Assert.assertNull(collect(name));
        }

        void assertAttribute(String name, String key, String value) {
            Assert.assertTrue(Arrays.stream(attributes.get(name))
                    .anyMatch(attr -> key.equals(attr.getKey()) && value.equals(attr.getValue())));
        }
    }
}
