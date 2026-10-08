package tech.ydb.topic.write.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.DoubleMeasurement;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;

public class WriterMetricsTest {
    private static final Message MSG1 = Message.of(new byte[] { 0x00, 0x01, 0x02 });

    @Test
    public void writerCountersCountAcceptedMessagesAndAcknowledgements() throws QueueOverflowException {
        Map<String, Long> counters = new HashMap<>();
        Meter meter = new Meter() {
            @Override
            public LongCounter createCounter(String name, String unit, String description) {
                counters.put(name, 0L);
                return (value, attrs) -> counters.put(name, counters.get(name) + value);
            }
        };
        WriteStreamMock stream = new WriteStreamMock();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test/topic").setCodec(Codec.RAW)
                .withMeter(meter, "writer").build();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(settings);
            try {
                writer.init();
                stream.sendInitResponse(0);
                writer.send(Message.of(new byte[]{1, 2}));
                writer.send(Message.of(new byte[]{3, 4, 5}));
                stream.sendAckResponse(2, 10);
                Assert.assertEquals(Long.valueOf(2), counters.get("ydb.topic.writer.sending.messages"));
                Assert.assertEquals(Long.valueOf(5), counters.get("ydb.topic.writer.sending.bytes"));
                Assert.assertEquals(Long.valueOf(2), counters.get("ydb.topic.writer.written.messages"));
            } finally {
                writer.shutdown();
                stream.close(Status.SUCCESS);
            }
        }
    }

    @Test
    public void writerBufferGaugesTrackBudgetAndLimit() throws QueueOverflowException {
        Map<String, LongSupplier> gauges = new HashMap<>();
        Meter meter = new Meter() {
            @Override
            public MetricRegistration registerLongGauge(String name, String unit, String description,
                    Consumer<LongMeasurement> callback) {
                gauges.put(name, () -> {
                    long[] value = new long[1];
                    callback.accept((observed, attrs) -> value[0] = observed);
                    return value[0];
                });
                return () -> gauges.remove(name);
            }
        };
        String used = "ydb.topic.writer.buffer.used.bytes";
        String limit = "ydb.topic.writer.buffer.limit.bytes";
        WriteStreamMock stream = new WriteStreamMock();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test/topic").setCodec(Codec.RAW).setMaxSendBufferMemorySize(100)
                .withMeter(meter, "writer").build();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(settings);
            try {
                writer.init();
                stream.sendInitResponse(0);
                Assert.assertEquals(0, gauges.get(used).getAsLong());
                Assert.assertEquals(100, gauges.get(limit).getAsLong());
                writer.send(Message.of(new byte[]{1, 2}));
                writer.send(Message.of(new byte[]{3, 4, 5}));
                Assert.assertEquals(5, gauges.get(used).getAsLong());
                Assert.assertEquals(100, gauges.get(limit).getAsLong());
                stream.sendAckResponse(2, 10);
                Assert.assertEquals(0, gauges.get(used).getAsLong());
            } finally {
                writer.shutdown();
                stream.close(Status.SUCCESS);
            }
        }
        Assert.assertTrue(gauges.isEmpty());
    }

    @Test
    public void writerAckDurationRecordsAcknowledgements() throws QueueOverflowException {
        List<Double> durations = new ArrayList<>();
        Meter meter = new Meter() {
            @Override
            public DoubleHistogram createHistogram(String name, String unit, String description) {
                return (value, attrs) -> durations.add(value);
            }
        };
        WriteStreamMock stream = new WriteStreamMock();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test/topic").setCodec(Codec.RAW).withMeter(meter, "writer").build();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(settings);
            try {
                writer.init();
                stream.sendInitResponse(0);
                writer.send(MSG1);
                writer.send(MSG1);
                Assert.assertTrue(durations.isEmpty());
                stream.sendAckResponse(2, 10);
                Assert.assertEquals(2, durations.size());
                Assert.assertTrue(durations.get(0) >= 0);
                Assert.assertTrue(durations.get(1) >= 0);
            } finally {
                writer.shutdown();
                stream.close(Status.SUCCESS);
            }
        }
    }

    @Test
    public void writerOldestAgeClearsAfterAcknowledgement() throws QueueOverflowException {
        Map<String, DoubleSupplier> gauges = new HashMap<>();
        Meter meter = new Meter() {
            @Override
            public MetricRegistration registerDoubleGauge(String name, String unit, String description,
                    Consumer<DoubleMeasurement> callback) {
                gauges.put(name, () -> {
                    double[] value = new double[1];
                    callback.accept((observed, attrs) -> value[0] = observed);
                    return value[0];
                });
                return () -> gauges.remove(name);
            }
        };
        String metric = "ydb.topic.writer.sending.oldest_age";
        WriteStreamMock stream = new WriteStreamMock();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test/topic").setCodec(Codec.RAW)
                .withMeter(meter, "writer").build();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(settings);
            try {
                DoubleSupplier age = gauges.get(metric);
                writer.init();
                stream.sendInitResponse(0);
                Assert.assertEquals(0, age.getAsDouble(), 0);
                long acceptedBefore = System.nanoTime();
                writer.send(MSG1);
                long acceptedAfter = System.nanoTime();
                writer.send(MSG1);
                long collectedBefore = System.nanoTime();
                double observed = age.getAsDouble();
                long collectedAfter = System.nanoTime();
                Assert.assertTrue(observed >= (collectedBefore - acceptedAfter) / 1_000_000_000d);
                Assert.assertTrue(observed <= (collectedAfter - acceptedBefore) / 1_000_000_000d);
                stream.sendAckResponse(2, 10);
                Assert.assertEquals(0, age.getAsDouble(), 0);
            } finally {
                writer.shutdown();
                stream.close(Status.SUCCESS);
            }
        }
        Assert.assertTrue(gauges.isEmpty());
    }

    private static TopicRpc mockRpc(WriteStreamMock stream) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.writeSession(Mockito.any(String.class))).thenReturn(stream);
        return rpc;
    }

}
