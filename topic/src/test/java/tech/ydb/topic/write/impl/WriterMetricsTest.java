package tech.ydb.topic.write.impl;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.RecordingMeter;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;

public class WriterMetricsTest {
    private static final String PREFIX = "ydb.topic.writer.";
    private final RecordingMeter meter = new RecordingMeter();

    @Test
    public void sendAndAcknowledgeMetrics() throws Exception {
        try (WriterFixture f = new WriterFixture(meter)) {
            Assert.assertEquals(0, meter.collect(PREFIX + "buffer.used.bytes"));
            Assert.assertEquals(100, meter.collect(PREFIX + "buffer.limit.bytes"));
            CompletableFuture<Void> written = CompletableFuture.allOf(
                    f.writer.send(Message.of(new byte[]{1, 2})),
                    f.writer.send(Message.of(new byte[]{3, 4, 5})));
            Assert.assertFalse(written.isDone());
            Assert.assertEquals(2, meter.value(PREFIX + "sending.messages"));
            Assert.assertEquals(5, meter.value(PREFIX + "sending.bytes"));
            Assert.assertEquals(0, meter.value(PREFIX + "written.messages"));
            Assert.assertEquals(5, meter.collect(PREFIX + "buffer.used.bytes"));
            Assert.assertTrue(meter.collectDouble(PREFIX + "sending.oldest_age") > 0);
            f.stream.sendAckResponse(2, 10);
            written.get(1, TimeUnit.SECONDS);
            Assert.assertEquals(2, meter.value(PREFIX + "written.messages"));
            Assert.assertEquals(0, meter.collect(PREFIX + "buffer.used.bytes"));
            Assert.assertEquals(100, meter.collect(PREFIX + "buffer.limit.bytes"));
            Assert.assertEquals(0, meter.collectDouble(PREFIX + "sending.oldest_age"), 0);
            for (String name : Arrays.asList("message.ack.duration", "message.processing.duration")) {
                Assert.assertEquals(2, meter.histograms.get(PREFIX + name).size());
                Assert.assertEquals("s", meter.units.get(PREFIX + name));
            }
            Assert.assertTrue(meter.histograms.get(PREFIX + "buffer.wait.duration").isEmpty());
            Assert.assertTrue(meter.histograms.get(PREFIX + "compression.duration").isEmpty());
            meter.assertAttributes(PREFIX + "sending.messages", "topic", "/test/topic", "writer.name", "writer");
            meter.assertAttributes(PREFIX + "message.processing.duration",
                    "topic", "/test/topic", "writer.name", "writer", "codec", "raw", "outcome", "ok");
            Assert.assertEquals("By", meter.units.get(PREFIX + "sending.bytes"));
        }
        Assert.assertEquals(0, meter.value(PREFIX + "session.errors"));
        Assert.assertTrue(meter.gauges.isEmpty());
        Assert.assertTrue(meter.doubleGauges.isEmpty());
    }

    private static TopicRpc mockRpc(WriteStreamMock stream) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.writeSession(Mockito.any(String.class))).thenReturn(stream);
        return rpc;
    }

    private static final class WriterFixture implements AutoCloseable {
        private final WriteStreamMock stream = new WriteStreamMock();
        private final TopicClient client = TopicClientImpl.newClient(mockRpc(stream))
                .setCompressionExecutor(Runnable::run).build();
        private final AsyncWriter writer;

        WriterFixture(RecordingMeter meter) {
            writer = client.createAsyncWriter(WriterSettings.newBuilder().setTopicPath("/test/topic")
                    .setCodec(Codec.RAW).setMaxSendBufferMemorySize(100).withMeter(meter, "writer").build());
            writer.init();
            stream.sendInitResponse(0);
        }

        @Override
        public void close() {
            try {
                writer.shutdown();
                stream.close(Status.SUCCESS);
            } finally {
                client.close();
            }
        }
    }
}
