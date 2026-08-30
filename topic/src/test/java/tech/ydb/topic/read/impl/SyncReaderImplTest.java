package tech.ydb.topic.read.impl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class SyncReaderImplTest {
    private static final CodecRegistry REGISTRY = new CodecRegistry();
    private static final byte[] MSG1 = new byte[] { 0x00 };
    private static final byte[] MSG2 = new byte[] { };
    private static final byte[] MSG3 = new byte[] { 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02 };
    private static final byte[] MSG4 = new byte[] {
        0x01, 0x23, 0x34, 0x45, 0x67,
        (byte) 0x89, (byte) 0xAB, (byte) 0xCD,(byte) 0xEF };
    private static final byte[] MSG5 = "utf8 encoded message".getBytes();

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

        Exception ex = Assert.assertThrows(RuntimeException.class, () -> reader.receive(0, TimeUnit.MILLISECONDS));
        Assert.assertEquals("Reader was stopped", ex.getMessage());

        mock.closeStream(Status.SUCCESS);
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
        mock.responseStartPartition("/test-topic", 123);
        mock.assertSentMessagesCount(3);
        mock.assertLastMessage().isStartPartition(1); // partition session id != partition id

        // start second partition
        mock.responseStartPartition("/test-topic", 345);
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
}
