package tech.ydb.topic.read.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromClient;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.read.DeferredCommitter;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;

public class ReaderMetricsTest {
    private static final String DELIVERED = "ydb.topic.reader.delivered.messages";
    private static final String RECEIVED_MESSAGES = "ydb.topic.reader.received.messages";
    private static final String RECEIVED_BYTES = "ydb.topic.reader.received.bytes";
    private static final String COMMIT_QUEUED = "ydb.topic.reader.commit.queued";
    private static final String COMMIT_ACKNOWLEDGED = "ydb.topic.reader.commit.acknowledged";

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
            stream.responseData(25).partition(1, 0)
                    .batch(Codec.RAW, new byte[]{1})
                    .batch(Codec.RAW, new byte[]{2})
                    .and().send();

            Assert.assertEquals(2, meter.value(RECEIVED_MESSAGES));
            Assert.assertEquals(25, meter.value(RECEIVED_BYTES));
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

    @Test
    public void commitCountersCountAcceptedAndAcknowledgedRanges() throws InterruptedException {
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
            stream.responseInit("read-session");
            stream.responseStartPartition("/topic", 42, 100);
            stream.responseData(25).partition(1, 100)
                    .batch(Codec.RAW, new byte[]{1}, new byte[]{2}, new byte[]{3}, new byte[]{4}, new byte[]{5})
                    .and().send();
            Message first = reader.receive(1, TimeUnit.SECONDS);
            Message second = reader.receive(1, TimeUnit.SECONDS);
            Message third = reader.receive(1, TimeUnit.SECONDS);
            Message fourth = reader.receive(1, TimeUnit.SECONDS);
            Message fifth = reader.receive(1, TimeUnit.SECONDS);
            Assert.assertNotNull(first);
            Assert.assertNotNull(second);
            Assert.assertNotNull(third);
            Assert.assertNotNull(fourth);
            Assert.assertNotNull(fifth);
            Assert.assertEquals(0, meter.value(COMMIT_QUEUED));
            Assert.assertEquals(0, meter.value(COMMIT_ACKNOWLEDGED));

            CompletableFuture<Void> committed = first.commit();
            Assert.assertFalse(committed.isDone());
            Assert.assertEquals(1, meter.value(COMMIT_QUEUED));
            Assert.assertSame(committed, first.commit());
            Assert.assertEquals(2, meter.value(COMMIT_QUEUED));
            DeferredCommitter deferred = DeferredCommitter.newInstance();
            deferred.add(second);
            deferred.add(fourth);
            deferred.commit();
            stream.assertLastMessage().isCommit(1)
                    .hasPartitionOffset(1, OffsetsRange.of(101, 102), OffsetsRange.of(103, 104));
            Assert.assertEquals(4, meter.value(COMMIT_QUEUED));
            meter.assertAttribute(COMMIT_QUEUED, "topic", "/topic");
            meter.assertAttribute(COMMIT_QUEUED, "consumer", "consumer");
            meter.assertAttribute(COMMIT_QUEUED, "reader.name", "reader");
            stream.responseCommitAck().partition(999, 104).send();
            Assert.assertEquals(0, meter.value(COMMIT_ACKNOWLEDGED));
            stream.responseCommitAck().partition(1, 102).send();
            Assert.assertTrue(committed.isDone());
            Assert.assertFalse(committed.isCompletedExceptionally());
            Assert.assertEquals(3, meter.value(COMMIT_ACKNOWLEDGED));
            stream.responseCommitAck().partition(1, 102).send();
            stream.responseCommitAck().partition(1, 101).send();
            Assert.assertEquals(3, meter.value(COMMIT_ACKNOWLEDGED));
            stream.responseCommitAck().partition(1, 104).send();
            Assert.assertEquals(4, meter.value(COMMIT_ACKNOWLEDGED));
            Assert.assertEquals(4, meter.value(COMMIT_QUEUED));
            meter.assertAttribute(COMMIT_ACKNOWLEDGED, "topic", "/topic");
            meter.assertAttribute(COMMIT_ACKNOWLEDGED, "consumer", "consumer");
            meter.assertAttribute(COMMIT_ACKNOWLEDGED, "reader.name", "reader");

            CompletableFuture<Void> pending = fifth.commit();
            Assert.assertEquals(5, meter.value(COMMIT_QUEUED));
            stream.responseStopPartition(1, false);
            stream.responseCommitAck().partition(1, 105).send();
            Assert.assertTrue(pending.isCompletedExceptionally());
            Assert.assertTrue(fifth.commit().isCompletedExceptionally());
            Assert.assertEquals(5, meter.value(COMMIT_QUEUED));
            Assert.assertEquals(4, meter.value(COMMIT_ACKNOWLEDGED));
        } finally {
            reader.shutdown();
            client.close();
        }
    }

    @Test
    public void commitAcknowledgementDuringSendIsCounted() throws InterruptedException {
        RecordingMeter meter = new RecordingMeter();
        ReadStreamMock stream = new ReadStreamMock() {
            @Override
            public void sendNext(FromClient message) {
                super.sendNext(message);
                if (message.hasCommitOffsetRequest()) {
                    Assert.assertEquals(1, meter.value(COMMIT_QUEUED));
                    responseCommitAck().partition(1, 1).send();
                }
            }
        };
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
            stream.responseInit("read-session");
            stream.responseStartPartition("/topic", 42, 0);
            stream.responseData(1).partition(1, 0).batch(Codec.RAW, new byte[]{1}).and().send();
            Message message = reader.receive(1, TimeUnit.SECONDS);
            Assert.assertNotNull(message);
            CompletableFuture<Void> committed = message.commit();
            Assert.assertTrue(committed.isDone());
            Assert.assertFalse(committed.isCompletedExceptionally());
            Assert.assertEquals(1, meter.value(COMMIT_ACKNOWLEDGED));
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
