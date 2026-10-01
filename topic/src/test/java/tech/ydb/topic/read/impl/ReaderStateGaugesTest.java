package tech.ydb.topic.read.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;

public class ReaderStateGaugesTest {
    private static final String PARTITIONS = "ydb.topic.reader.partition_session.count";
    private static final String CREDIT = "ydb.topic.reader.credit_balance_bytes";

    @Test
    public void gaugesObservePartitionSessionsAndProtocolCredit() throws InterruptedException {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(stream);
        ReaderSettings settings = ReaderSettings.newBuilder().addTopic("/topic").setConsumerName("consumer")
                .setMaxMemoryUsageBytes(100).withMeter(meter, "reader").build();
        SyncReader reader = new SyncReaderImpl(rpc, settings, new CodecRegistry());
        try {
            reader.init();
            stream.responseInit("session");
            stream.responseStartPartition("/topic", 42, 0);
            Assert.assertEquals(1, meter.collect(PARTITIONS));
            Assert.assertEquals(100, meter.collect(CREDIT));

            stream.responseData(20).partition(1, 0).batch(Codec.RAW, new byte[]{1}).and().send();
            Assert.assertEquals(80, meter.collect(CREDIT));
            Assert.assertNotNull(reader.receive(1, TimeUnit.SECONDS));
            Assert.assertEquals(100, meter.collect(CREDIT));
        } finally {
            reader.shutdown();
        }
    }

    private static class RecordingMeter implements Meter {
        private final Map<String, Consumer<LongMeasurement>> gauges = new HashMap<>();

        @Override
        public void createLongGauge(String name, String unit, String description, Consumer<LongMeasurement> callback) {
            gauges.put(name, callback);
        }

        long collect(String name) {
            long[] value = new long[1];
            gauges.get(name).accept((observed, attrs) -> value[0] = observed);
            return value[0];
        }
    }
}
