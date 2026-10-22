package tech.ydb.topic.read.impl;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.utils.RecordingMeter;

public class ReaderMetricsTest {
    private static final String PREFIX = "ydb.topic.reader.";
    private static final byte[] MSG1 = new byte[] { 1, 2 };
    private static final byte[] MSG2 = new byte[] { 3, 4, 5 };
    private final RecordingMeter meter = new RecordingMeter();

    @Test
    public void readAndCommitMetrics() throws Exception {
        try (ReaderFixture f = new ReaderFixture(meter)) {
            f.start();
            f.stream.responseStartPartition("/topic", 42, 0);
            Assert.assertEquals(1, meter.collect(PREFIX + "partition_session.count"));
            Assert.assertEquals(100, meter.collect(PREFIX + "credit_balance_bytes"));
            f.stream.responseData(20).partition(1, 0).batch(Codec.GZIP, MSG1, MSG2).and().send();
            Assert.assertEquals(2, meter.value(PREFIX + "received.messages"));
            Assert.assertEquals(20, meter.value(PREFIX + "received.bytes"));
            Assert.assertEquals(0, meter.value(PREFIX + "delivered.messages"));
            Assert.assertEquals(2, meter.collect(PREFIX + "local_buffer.messages"));
            Assert.assertTrue(meter.collectDouble(PREFIX + "local_buffer.message_age.max") > 0);
            Message first = f.reader.receive(0, TimeUnit.SECONDS);
            Message second = f.reader.receive(0, TimeUnit.SECONDS);
            Assert.assertNotNull(first);
            Assert.assertNotNull(second);
            Assert.assertArrayEquals(MSG1, first.getData());
            Assert.assertArrayEquals(MSG2, second.getData());
            Assert.assertEquals(2, meter.value(PREFIX + "delivered.messages"));
            Assert.assertEquals(0, meter.collect(PREFIX + "local_buffer.messages"));
            Assert.assertEquals(0, meter.collectDouble(PREFIX + "local_buffer.message_age.max"), 0);
            Assert.assertEquals(100, meter.collect(PREFIX + "credit_balance_bytes"));
            CompletableFuture<Void> committed = CompletableFuture.allOf(first.commit(), second.commit());
            Assert.assertFalse(committed.isDone());
            Assert.assertEquals(2, meter.value(PREFIX + "commit.queued"));
            Assert.assertEquals(0, meter.value(PREFIX + "commit.acknowledged"));
            Assert.assertEquals(2, meter.collect(PREFIX + "commit_offset.lag.max"));
            f.stream.responseCommitAck().partition(1, 2).send();
            committed.get(1, TimeUnit.SECONDS);
            Assert.assertEquals(2, meter.value(PREFIX + "commit.acknowledged"));
            Assert.assertEquals(0, meter.collect(PREFIX + "commit_offset.lag.max"));
            for (String name : Arrays.asList("compression.duration", "message.processing.duration")) {
                Assert.assertEquals(2, meter.histograms.get(PREFIX + name).size());
                Assert.assertEquals("s", meter.units.get(PREFIX + name));
                meter.assertAttributes(PREFIX + name, "consumer", "consumer", "reader.name", "reader",
                        "topic", "/topic", "codec", "gzip", "outcome", "ok");
            }
            meter.assertAttributes(PREFIX + "received.messages",
                    "consumer", "consumer", "reader.name", "reader", "topic", "/topic");
            Assert.assertEquals("By", meter.units.get(PREFIX + "received.bytes"));
            f.stream.responseStopPartition(1, true);
            Assert.assertEquals(0, meter.collect(PREFIX + "partition_session.count"));
        }
        Assert.assertEquals(0, meter.value(PREFIX + "session.errors"));
        Assert.assertTrue(meter.gauges.isEmpty());
        Assert.assertTrue(meter.doubleGauges.isEmpty());
    }

    private static final class ReaderFixture implements AutoCloseable {
        private final ReadStreamMock stream = new ReadStreamMock(true);
        private final TopicClient client;
        private final SyncReader reader;

        ReaderFixture(RecordingMeter meter) {
            TopicRpc rpc = Mockito.mock(TopicRpc.class);
            Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
            Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(stream);
            client = TopicClientImpl.newClient(rpc).build();
            reader = client.createSyncReader(ReaderSettings.newBuilder().addTopic("/topic")
                    .setConsumerName("consumer").setMaxMemoryUsageBytes(100)
                    .setDecompressionExecutor(Runnable::run).withMeter(meter, "reader").build());
        }

        void start() {
            reader.init();
            stream.assertLastMessage().isInitRequest("consumer", "/topic").hasReaderName("reader");
            stream.responseInit("read-session");
        }

        @Override
        public void close() {
            try {
                reader.shutdown();
            } finally {
                client.close();
            }
        }
    }
}
