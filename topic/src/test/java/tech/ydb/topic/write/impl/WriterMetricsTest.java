package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.TopicMeterMock;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

public class WriterMetricsTest {
    private static final String SENDING = "ydb.topic.writer.sending.messages";
    private static final String BYTES = "ydb.topic.writer.sending.bytes";
    private static final String WRITTEN = "ydb.topic.writer.written.messages";
    private static final String ERRORS = "ydb.topic.writer.session.errors";
    private static final String USED = "ydb.topic.writer.buffer.used.bytes";
    private static final String LIMIT = "ydb.topic.writer.buffer.limit.bytes";

    @Test
    public void writerSendsMessagesAndShutsDown() throws QueueOverflowException {
        TopicMeterMock meter = new TopicMeterMock();
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
                Assert.assertEquals(0, meter.value(SENDING));
                Assert.assertEquals(0, meter.value(WRITTEN));
                Assert.assertEquals(0, meter.value(ERRORS));
                Assert.assertEquals(0, meter.collect(USED));
                Assert.assertEquals(100, meter.collect(LIMIT));

                CompletableFuture<WriteAck> first = writer.send(Message.of(new byte[]{1, 2}));
                CompletableFuture<WriteAck> second = writer.send(Message.of(new byte[]{3, 4, 5}));
                Assert.assertFalse(first.isDone());
                Assert.assertFalse(second.isDone());
                Assert.assertEquals(2, meter.value(SENDING));
                Assert.assertEquals(5, meter.value(BYTES));
                Assert.assertEquals(0, meter.value(WRITTEN));
                Assert.assertEquals(5, meter.collect(USED));
                Assert.assertEquals(100, meter.collect(LIMIT));

                stream.sendAckResponse(2, 10);
                Assert.assertTrue(first.isDone());
                Assert.assertTrue(second.isDone());
                Assert.assertEquals(2, meter.value(SENDING));
                Assert.assertEquals(5, meter.value(BYTES));
                Assert.assertEquals(2, meter.value(WRITTEN));
                Assert.assertEquals(0, meter.value(ERRORS));
                Assert.assertEquals(0, meter.collect(USED));
                meter.assertAttribute(SENDING, "topic", "/test/topic");
                meter.assertAttribute(BYTES, "writer.name", "writer");
                meter.assertAttribute(WRITTEN, "writer.name", "writer");
                meter.assertAttribute(USED, "topic", "/test/topic");
                meter.assertAttribute(LIMIT, "writer.name", "writer");
            } finally {
                writer.shutdown();
                stream.close(Status.SUCCESS);
            }
        }
        Assert.assertEquals(0, meter.value(ERRORS));
        meter.assertNoGauges();
    }

    @Test
    public void writerReconnectsThenStopsOnTerminalError() throws QueueOverflowException {
        TopicMeterMock meter = new TopicMeterMock();
        WriteStreamMock first = new WriteStreamMock();
        WriteStreamMock retry = new WriteStreamMock();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test/topic").setCodec(Codec.RAW).setMaxSendBufferMemorySize(100)
                .setRetryConfig(status -> status.getCode() == StatusCode.TRANSPORT_UNAVAILABLE
                        ? (attempt, elapsed) -> 0 : null)
                .withMeter(meter, "writer").build();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(first, retry))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(settings);
            try {
                writer.init();
                first.sendInitResponse(0);
                CompletableFuture<WriteAck> written = writer.send(Message.of(new byte[]{1, 2}));
                CompletableFuture<WriteAck> pending = writer.send(Message.of(new byte[]{3, 4, 5}));
                first.close(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
                Assert.assertEquals(1, meter.value(ERRORS));
                meter.assertAttribute(ERRORS, "retry_decision", "retry");
                meter.assertAttribute(ERRORS, "status_code", "TRANSPORT_UNAVAILABLE");
                meter.assertAttribute(ERRORS, "error.type", "transport_error");
                Assert.assertEquals(2, meter.value(SENDING));
                Assert.assertEquals(5, meter.value(BYTES));
                Assert.assertEquals(0, meter.value(WRITTEN));
                Assert.assertEquals(5, meter.collect(USED));
                Assert.assertEquals(100, meter.collect(LIMIT));

                retry.sendInitResponse(0);
                retry.sendAckResponse(1, 10);
                Assert.assertTrue(written.isDone());
                Assert.assertFalse(pending.isDone());
                Assert.assertEquals(2, meter.value(SENDING));
                Assert.assertEquals(5, meter.value(BYTES));
                Assert.assertEquals(1, meter.value(WRITTEN));
                Assert.assertEquals(3, meter.collect(USED));

                retry.close(Status.of(StatusCode.UNAUTHORIZED));
                Assert.assertTrue(pending.isCompletedExceptionally());
                Assert.assertEquals(2, meter.value(ERRORS));
                Assert.assertEquals(2, meter.value(SENDING));
                Assert.assertEquals(5, meter.value(BYTES));
                Assert.assertEquals(1, meter.value(WRITTEN));
                meter.assertAttribute(ERRORS, "retry_decision", "stop");
                meter.assertAttribute(ERRORS, "status_code", "UNAUTHORIZED");
                meter.assertAttribute(ERRORS, "error.type", "ydb_error");
                meter.assertAttribute(ERRORS, "topic", "/test/topic");
                meter.assertAttribute(ERRORS, "writer.name", "writer");
                meter.assertNoGauges();
            } finally {
                writer.shutdown();
            }
        }
        Assert.assertEquals(2, meter.value(ERRORS));
        meter.assertNoGauges();
    }

    private static TopicRpc mockRpc(WriteStreamMock first, WriteStreamMock... retries) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.writeSession(Mockito.anyString())).thenReturn(first, retries);
        return rpc;
    }
}
