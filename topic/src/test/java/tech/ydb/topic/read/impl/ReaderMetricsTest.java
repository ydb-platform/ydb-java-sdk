package tech.ydb.topic.read.impl;

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
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.utils.TopicMeterMock;

public class ReaderMetricsTest {
    private static final String DELIVERED = "ydb.topic.reader.delivered.messages";
    private static final String RECEIVED = "ydb.topic.reader.received.messages";
    private static final String BYTES = "ydb.topic.reader.received.bytes";
    private static final String ERRORS = "ydb.topic.reader.session.errors";
    private static final String PARTITIONS = "ydb.topic.reader.partition_session.count";
    private static final String CREDIT = "ydb.topic.reader.credit_balance_bytes";

    @Test
    public void readerReceivesMessagesAndShutsDown() throws InterruptedException {
        TopicMeterMock meter = new TopicMeterMock();
        ReadStreamMock stream = new ReadStreamMock();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream)).build()) {
            SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder()
                    .addTopic("/topic").setConsumerName("consumer")
                    .setMaxMemoryUsageBytes(100).withMeter(meter, "reader").build());
            try {
                meter.assertNoGauges();
                reader.init();
                stream.assertLastMessage().isInitRequest("consumer", "/topic").hasReaderName("reader");
                meter.assertNoGauges();
                stream.responseInit("session");
                Assert.assertEquals(0, meter.collect(PARTITIONS));
                Assert.assertEquals(100, meter.collect(CREDIT));
                stream.responseStartPartition("/topic", 42, 0);
                Assert.assertEquals(1, meter.collect(PARTITIONS));

                byte[] msg1 = new byte[]{1};
                byte[] msg2 = new byte[]{2};
                stream.responseData(25).partition(1, 0).batch(Codec.RAW, msg1, msg2).and().send();
                Assert.assertEquals(2, meter.value(RECEIVED));
                Assert.assertEquals(25, meter.value(BYTES));
                Assert.assertEquals(75, meter.collect(CREDIT));
                Assert.assertEquals(0, meter.value(DELIVERED));
                Assert.assertEquals(0, meter.value(ERRORS));

                stream.responseData(30).partition(1, 2).batch(Codec.RAW, msg1, msg2, msg1).and().send();
                Assert.assertEquals(5, meter.value(RECEIVED));
                Assert.assertEquals(55, meter.value(BYTES));
                Assert.assertEquals(45, meter.collect(CREDIT));
                reader.receive();
                reader.receive();
                Assert.assertEquals(2, meter.value(DELIVERED));
                reader.receive();
                reader.receive();
                reader.receive();
                Assert.assertEquals(5, meter.value(DELIVERED));
                Assert.assertEquals(100, meter.collect(CREDIT));
                Assert.assertEquals(1, meter.collect(PARTITIONS));
                meter.assertAttribute(RECEIVED, "topic", "/topic");
                meter.assertAttribute(DELIVERED, "topic", "/topic");
                meter.assertAttribute(BYTES, "consumer", "consumer");
                meter.assertAttribute(PARTITIONS, "reader.name", "reader");
                meter.assertAttribute(CREDIT, "consumer", "consumer");
            } finally {
                reader.shutdown();
            }
        }
        Assert.assertEquals(0, meter.value(ERRORS));
        meter.assertNoGauges();
    }

    @Test
    public void readerReconnectsThenStopsOnTerminalError() throws InterruptedException {
        TopicMeterMock meter = new TopicMeterMock();
        ReadStreamMock first = new ReadStreamMock();
        ReadStreamMock retry = new ReadStreamMock();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(first, retry)).build()) {
            SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder()
                    .addTopic("/topic").setConsumerName("consumer").setMaxMemoryUsageBytes(100)
                    .setRetryConfig(status -> status.getCode() == StatusCode.TRANSPORT_UNAVAILABLE
                            ? (attempt, elapsed) -> 0 : null)
                    .withMeter(meter, "reader").build());
            try {
                reader.init();
                first.responseInit("first");
                first.responseStartPartition("/topic", 42, 0);
                first.responseData(25).partition(1, 0).batch(Codec.RAW, new byte[]{1}).and().send();
                reader.receive();
                first.closeStream(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
                Assert.assertEquals(1, meter.value(ERRORS));
                meter.assertAttribute(ERRORS, "retry_decision", "retry");
                meter.assertAttribute(ERRORS, "status_code", "TRANSPORT_UNAVAILABLE");
                meter.assertAttribute(ERRORS, "error.type", "transport_error");
                meter.assertNoGauges();
                Assert.assertEquals(1, meter.value(RECEIVED));
                Assert.assertEquals(1, meter.value(DELIVERED));
                Assert.assertEquals(25, meter.value(BYTES));

                retry.responseInit("retry");
                retry.responseStartPartition("/topic", 42, 1);
                Assert.assertEquals(1, meter.collect(PARTITIONS));
                Assert.assertEquals(100, meter.collect(CREDIT));
                retry.responseData(30).partition(1, 1).batch(Codec.RAW, new byte[]{2}).and().send();
                reader.receive();
                Assert.assertEquals(2, meter.value(RECEIVED));
                Assert.assertEquals(2, meter.value(DELIVERED));
                Assert.assertEquals(55, meter.value(BYTES));
                Assert.assertEquals(100, meter.collect(CREDIT));

                retry.closeStream(Status.of(StatusCode.UNAUTHORIZED));
                Assert.assertEquals(2, meter.value(ERRORS));
                meter.assertAttribute(ERRORS, "retry_decision", "stop");
                meter.assertAttribute(ERRORS, "status_code", "UNAUTHORIZED");
                meter.assertAttribute(ERRORS, "error.type", "ydb_error");
                meter.assertAttribute(ERRORS, "consumer", "consumer");
                meter.assertAttribute(ERRORS, "reader.name", "reader");
                meter.assertNoGauges();
            } finally {
                reader.shutdown();
            }
        }
        Assert.assertEquals(2, meter.value(ERRORS));
        meter.assertNoGauges();
    }

    private static TopicRpc mockRpc(ReadStreamMock first, ReadStreamMock... retries) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.anyString())).thenReturn(first, retries);
        return rpc;
    }
}
