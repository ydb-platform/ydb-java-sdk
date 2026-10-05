package tech.ydb.topic.read.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

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
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;

public class ReaderMetricsTest {
    private static final String DELIVERED = "ydb.topic.reader.delivered.messages";
    private static final String RECEIVED_MESSAGES = "ydb.topic.reader.received.messages";
    private static final String RECEIVED_BYTES = "ydb.topic.reader.received.bytes";

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
            byte[] msg1 = new byte[] { 1 };
            byte[] msg2 = new byte[] { 2 };
            stream.responseData(25).partition(1, 0)
                    .batch(Codec.RAW, msg1, msg2)
                    .and().send();
            Assert.assertEquals(2, meter.value(RECEIVED_MESSAGES));
            Assert.assertEquals(25, meter.value(RECEIVED_BYTES));

            stream.responseData(30).partition(1, 2)
                    .batch(Codec.RAW, msg1, msg2, msg1)
                    .and().send();
            Assert.assertEquals(5, meter.value(RECEIVED_MESSAGES));
            Assert.assertEquals(55, meter.value(RECEIVED_BYTES));

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

    private static class RecordingMeter implements Meter {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        private final Map<String, Attr[]> attributes = new ConcurrentHashMap<>();

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
