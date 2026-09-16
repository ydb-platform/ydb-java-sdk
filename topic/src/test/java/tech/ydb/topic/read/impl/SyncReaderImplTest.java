package tech.ydb.topic.read.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.DeferredCommitter;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.utils.ErrorsHandler;
import tech.ydb.topic.utils.HideLoggers;
import tech.ydb.topic.utils.HideLoggersRule;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class SyncReaderImplTest {
    private static final CodecRegistry REGISTRY = new CodecRegistry();
    private static final RetryConfig IMMEDIATE_RETRY = status -> (number, elapsed) -> 0;

    private static final byte[] MSG1 = new byte[] { 0x00 };
    private static final byte[] MSG2 = new byte[] { };
    private static final byte[] MSG3 = new byte[] { 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02 };
    private static final byte[] MSG4 = new byte[] {
        0x01, 0x23, 0x34, 0x45, 0x67,
        (byte) 0x89, (byte) 0xAB, (byte) 0xCD,(byte) 0xEF };
    private static final byte[] MSG5 = "utf8 encoded message".getBytes();

    @Rule
    public final HideLoggersRule hideLogger = new HideLoggersRule();

    private static TopicRpc mockRpc(ReadStreamMock first, ReadStreamMock... rest) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.any(String.class))).thenReturn(first, rest);
        return rpc;
    }

    @Test
    public void initAndShutdownTest() throws InterruptedException {
        ReadStreamMock mock = new ReadStreamMock();

        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath("/test-topic").build())
                .setConsumerName("consumer")
                .setReaderName("test-reader-name")
                .build();

        SyncReader reader = new SyncReaderImpl(mockRpc(mock), settings, REGISTRY);
        mock.assertSentMessagesCount(0);

        // before init there is nothing to read
        Assert.assertNull(reader.receive(0, TimeUnit.MILLISECONDS));

        reader.init();
        mock.assertSentMessagesCount(1);
        mock.assertLastMessage().isInitRequest("consumer", "/test-topic");

        reader.init(); // double init is allowed
        mock.assertSentMessagesCount(1);

        mock.responseInit("read-session-1");

        Assert.assertEquals("read-session-1", reader.getSessionId());
        mock.assertSentMessagesCount(2);
        mock.assertLastMessage().isReadRequest(100 * 1024 * 1024);

        Assert.assertNull(reader.receive(0, TimeUnit.MILLISECONDS));

        reader.shutdown();
        mock.assertIsClosed();

        reader.shutdown(); // double shutdow is allowed

        Exception ex = Assert.assertThrows(RuntimeException.class, () -> reader.receive(0, TimeUnit.MILLISECONDS));
        Assert.assertEquals("Reader was stopped", ex.getMessage());

        mock.closeStream(Status.SUCCESS);
    }

    @Test
    public void shutdownBeforeInitTest() throws InterruptedException {
        ReadStreamMock mock = new ReadStreamMock();

        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath("/test-topic").build())
                .setConsumerName("consumer")
                .build();

        SyncReader reader = new SyncReaderImpl(mockRpc(mock), settings, REGISTRY);

        reader.init();
        mock.assertSentMessagesCount(1);
        mock.assertLastMessage().isInitRequest("consumer", "/test-topic");

        reader.shutdown(); // shutdown before successful init
        mock.assertIsClosed();
        mock.closeStream(Status.SUCCESS);

        Exception ex = Assert.assertThrows(RuntimeException.class, () -> reader.receive(0, TimeUnit.MILLISECONDS));
        Assert.assertEquals("Reader was stopped", ex.getMessage());
    }

    @Test
    public void readClosedPartitionTest() throws InterruptedException {
        ReadStreamMock mock = new ReadStreamMock();

        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath("/test-topic").build())
                .setMaxMemoryUsageBytes(200000)
                .setConsumerName("consumer")
                .build();

        SyncReader reader = new SyncReaderImpl(mockRpc(mock), settings, REGISTRY);
        reader.init();

        mock.responseInit("read-session-1");
        mock.assertSentMessagesCount(2);
        mock.assertLastMessage().isReadRequest(200000);

        // partition start is auto confirmed
        mock.responseStartPartition("/test-topic", 123, 0);
        mock.assertSentMessagesCount(3);
        mock.assertLastMessage().isStartPartition(1); // partition session id != partition id

        // start second partition
        mock.responseStartPartition("/test-topic", 345, 0);
        mock.assertSentMessagesCount(4);
        mock.assertLastMessage().isStartPartition(2);

        mock.responseData(10000).partition(1, 0).batch(Codec.RAW, MSG1, MSG2, MSG3, MSG4, MSG5).and().send();
        mock.responseData(10000).partition(2, 0).batch(Codec.RAW, MSG5, MSG4, MSG3, MSG2, MSG1).and().send();

        Assert.assertArrayEquals(reader.receive().getData(), MSG1);
        Assert.assertArrayEquals(reader.receive().getData(), MSG2);

        // partition stop is auto confirmed
        mock.responseStopPartition(1, true);
        mock.assertSentMessagesCount(5);
        mock.assertLastMessage().isStopPartition(1);

        // continue to read message from second partition
        Assert.assertArrayEquals(reader.receive().getData(), MSG5);
        Assert.assertArrayEquals(reader.receive().getData(), MSG4);
        Assert.assertArrayEquals(reader.receive().getData(), MSG3);
        Assert.assertArrayEquals(reader.receive().getData(), MSG2);
        Assert.assertArrayEquals(reader.receive().getData(), MSG1);

        // request next data from server
        mock.assertSentMessagesCount(6);
        mock.assertLastMessage().isReadRequest(20000);
    }

    @Test
    @HideLoggers({ BufferManager.class, ReaderImpl.class })
    public void invalidBatchesTest() throws InterruptedException {
        ReadStreamMock mock = new ReadStreamMock();

        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath("/test-topic").build())
                .setMaxMemoryUsageBytes(2000)
                .setConsumerName("consumer")
                .build();

        SyncReader reader = new SyncReaderImpl(mockRpc(mock), settings, REGISTRY);
        reader.init();

        mock.responseInit("read-session-1");
        mock.assertSentMessagesCount(2);
        mock.assertLastMessage().isReadRequest(2000);

        // partition start is auto confirmed
        mock.responseStartPartition("/test-topic", 123, 0);
        mock.assertSentMessagesCount(3);
        mock.assertLastMessage().isStartPartition(1); // partition session id != partition id

        // batch without messages is just auto released
        mock.responseData(1000).partition(1, 0).batch(Codec.RAW).and().send();
        mock.assertSentMessagesCount(4);
        mock.assertLastMessage().isReadRequest(1000);

        reader.shutdown();
        // batch after shutdown is just skipped
        mock.responseData(1200).partition(1, 1000).batch(Codec.RAW, MSG1, MSG2, MSG3, MSG4, MSG5).and().send();
        mock.assertSentMessagesCount(5);
        mock.assertLastMessage().isReadRequest(1200);

        mock.closeStream(Status.of(StatusCode.INTERNAL_ERROR));
    }

    @Test
    public void retrySkipsReadMessagesTest() throws InterruptedException {
        ErrorsHandler errorsHandler = new ErrorsHandler();

        ReadStreamMock m1 = new ReadStreamMock();
        ReadStreamMock m2 = new ReadStreamMock();

        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath("/test-topic").build())
                .setMaxMemoryUsageBytes(2000)
                .setConsumerName("consumer")
                .setRetryConfig(IMMEDIATE_RETRY)
                .setErrorsHandler(errorsHandler)
                .build();

        SyncReader reader = new SyncReaderImpl(mockRpc(m1, m2), settings, REGISTRY);
        reader.init();

        m1.assertSentMessagesCount(1);
        m1.assertLastMessage().isInitRequest("consumer", "/test-topic");

        m1.responseInit("read-session-1");
        m1.assertSentMessagesCount(2);
        m1.assertLastMessage().isReadRequest(2000);

        // start partition read and send 4 messages
        m1.responseStartPartition("/test-topic", 123, 0);
        m1.assertSentMessagesCount(3);
        m1.assertLastMessage().isStartPartition(1); // partition session id != partition id
        m1.responseData(1000).partition(1, 0).batch(Codec.RAW, MSG1, MSG2, MSG3, MSG4).and().send();

        // read 3 messages
        Message msg1 = reader.receive(1, TimeUnit.SECONDS);
        Assert.assertNotNull(msg1);
        Assert.assertArrayEquals(MSG1, msg1.getData());
        Message msg2 = reader.receive(1, TimeUnit.SECONDS);
        Assert.assertNotNull(msg2);
        Assert.assertArrayEquals(MSG2, msg2.getData());
        Message msg3 = reader.receive(1, TimeUnit.SECONDS);
        Assert.assertNotNull(msg3);
        Assert.assertArrayEquals(MSG3, msg3.getData());

        // commit 2 messages
        CompletableFuture<Void> c1 = msg1.commit();
        CompletableFuture<Void> c2 = msg2.commit();

        // reader doesn't merge commits
        m1.assertSentMessagesCount(5);
        m1.assertLastMessage().isCommit(1).hasPartitionOffset(1, OffsetsRange.of(1, 2));

        Assert.assertFalse(c1.isDone());
        Assert.assertFalse(c2.isDone());

        // comfirm commit for 1 message
        m1.responseCommitAck().partition(1, 1).send();

        Assert.assertTrue(c1.isDone());
        Assert.assertFalse(c2.isDone());

        // get a stream error
        errorsHandler.assertEmpty();
        m1.closeStream(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
        errorsHandler.assertCodes(StatusCode.TRANSPORT_UNAVAILABLE);

        // commit for 2 message is failed
        Assert.assertTrue(c2.isCompletedExceptionally());

        // init second stream
        m2.assertSentMessagesCount(1);
        m2.assertLastMessage().isInitRequest("consumer", "/test-topic");

        m2.responseInit("read-session-2");
        m2.assertSentMessagesCount(2);
        m2.assertLastMessage().isReadRequest(2000);

        // partition start is auto confirmed
        m2.responseStartPartition("/test-topic", 123, 1);
        m2.assertSentMessagesCount(3);
        m2.assertLastMessage().isStartPartition(1); // partition session id != partition id

        // no messages in reader queue
        Assert.assertNull(reader.receive(0, TimeUnit.SECONDS));

        m2.responseData(1000).partition(1, 1).batch(Codec.RAW, MSG2, MSG3, MSG4, MSG5).and().send();

        // commit for the lost stream is failed
        Assert.assertTrue(msg3.commit().isCompletedExceptionally());

        // read 3 messages, message from the lost message will be deleted
        msg2 = reader.receive(1, TimeUnit.SECONDS);
        Assert.assertNotNull(msg2);
        Assert.assertArrayEquals(MSG2, msg2.getData());
        msg3 = reader.receive(1, TimeUnit.SECONDS);
        Assert.assertNotNull(msg3);
        Assert.assertArrayEquals(MSG3, msg3.getData());
        Message msg4 = reader.receive(1, TimeUnit.SECONDS);
        Assert.assertNotNull(msg4);
        Assert.assertArrayEquals(MSG4, msg4.getData());

        DeferredCommitter committer = DeferredCommitter.newInstance();
        committer.add(msg2);
        committer.add(msg4);
        committer.commit();

        m2.assertLastMessage().isCommit(1).hasPartitionOffset(1, OffsetsRange.of(1, 2), OffsetsRange.of(3, 4));
        reader.shutdown();

        m2.assertIsClosed();
    }
}
