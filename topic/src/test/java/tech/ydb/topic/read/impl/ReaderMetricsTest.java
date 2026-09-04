package tech.ydb.topic.read.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;

public class ReaderMetricsTest {
    private static final String DELIVERED = "ydb.topic.reader.delivered.messages";

    @Test
    public void deliveredMessageIncrementsCounter() throws InterruptedException {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.any(String.class))).thenReturn(stream);

        TopicClient client = TopicClientImpl.newClient(rpc).withMeter(meter).build();
        SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath("/topic").build())
                .setConsumerName("consumer")
                .build());

        try {
            reader.init();
            stream.responseInit("read-session");
            stream.responseStartPartition("/topic", 42);
            stream.responseData(25).partition(1, -1)
                    .batch(Codec.RAW, new byte[] { 1 })
                    .and().send();

            Assert.assertEquals(0, meter.value(DELIVERED));

            Assert.assertNotNull(reader.receive());
            Assert.assertEquals(1, meter.value(DELIVERED));
        } finally {
            reader.shutdown();
            client.close();
        }
    }

    private static class RecordingMeter implements Meter {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

        @Override
        public LongCounter createCounter(String name, String unit, String description) {
            AtomicLong counter = counters.computeIfAbsent(name, key -> new AtomicLong());
            return (value, attrs) -> counter.addAndGet(value);
        }

        long value(String name) {
            AtomicLong counter = counters.get(name);
            return counter == null ? 0 : counter.get();
        }
    }
}
