package tech.ydb.topic.read.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.DeferredCommitter;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.events.CommitOffsetAcknowledgementEvent;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.ReaderClosedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.read.impl.events.SessionStartedEvent;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.utils.ErrorsHandler;
import tech.ydb.topic.utils.HideLoggers;
import tech.ydb.topic.utils.HideLoggersRule;

public class AsyncReaderImplTest {
    private static final CodecRegistry REGISTRY = new CodecRegistry();
    private static final RetryConfig IMMEDIATE_RETRY = status -> (number, elapsed) -> 0;

    private static final byte[] MSG1 = new byte[] { 0x00 };
    private static final byte[] MSG2 = new byte[] { };
    private static final byte[] MSG3 = new byte[] { 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02 };
    private static final byte[] MSG4 = new byte[] {
        0x01, 0x23, 0x34, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };
    private static final byte[] MSG5 = "utf8 encoded message".getBytes();

    @Rule
    public final HideLoggersRule hideLogger = new HideLoggersRule();

    private final ReadEventHandler handler = Mockito.mock(ReadEventHandler.class, Mockito.CALLS_REAL_METHODS);

    private static TopicRpc mockRpc(ReadStreamMock first, ReadStreamMock... rest) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.any(String.class))).thenReturn(first, rest);
        return rpc;
    }

    private static ReaderSettings.Builder settings() {
        return ReaderSettings.newBuilder()
                .addTopic("/test-topic")
                .setConsumerName("consumer")
                .setDecompressionExecutor(Runnable::run);
    }

    private AsyncReader reader(ReaderSettings settings, ReadStreamMock first, ReadStreamMock... rest) {
        ReadEventHandlersSettings handlers = ReadEventHandlersSettings.newBuilder()
                .setEventHandler(handler)
                .setExecutor(Runnable::run)
                .build();
        return new AsyncReaderImpl(mockRpc(first, rest), settings, handlers, REGISTRY);
    }

    private static void assertMessages(List<byte[]> expected, List<Message> messages, long offset) {
        Assert.assertEquals(expected.size(), messages.size());
        for (int idx = 0; idx < expected.size(); idx++) {
            Assert.assertEquals(offset + idx, messages.get(idx).getOffset());
            Assert.assertArrayEquals(expected.get(idx), messages.get(idx).getData());
        }
    }

    @Test
    public void initAndShutdownTest() {
        ReadStreamMock mock = new ReadStreamMock();
        AsyncReader reader = reader(settings().setReaderName("test-reader-name").build(), mock);
        mock.assertSentMessagesCount(0);
        Mockito.verifyNoInteractions(handler);

        CompletableFuture<Void> init = reader.init();
        Assert.assertFalse(init.isDone());
        mock.assertSentMessagesCount(1);
        mock.assertLastMessage().isInitRequest("consumer", "/test-topic");

        Assert.assertSame(init, reader.init()); // double init is allowed
        mock.assertSentMessagesCount(1);
        mock.responseInit("read-session-1");

        Assert.assertTrue(init.isDone());
        Assert.assertFalse(init.isCompletedExceptionally());
        ArgumentCaptor<SessionStartedEvent> started = ArgumentCaptor.forClass(SessionStartedEvent.class);
        Mockito.verify(handler).onSessionStarted(started.capture());
        Assert.assertEquals("read-session-1", started.getValue().getSessionId());
        mock.assertSentMessagesCount(2);
        mock.assertLastMessage().isReadRequest(100 * 1024 * 1024);

        CompletableFuture<Void> shutdown = reader.shutdown();
        mock.assertIsClosed();
        Assert.assertTrue(shutdown.isDone());
        Assert.assertSame(shutdown, reader.shutdown()); // double shutdown is allowed
        mock.assertIsClosed();
        mock.closeStream(Status.SUCCESS);

        Assert.assertTrue(shutdown.isDone());
        Assert.assertFalse(shutdown.isCompletedExceptionally());
        Mockito.verify(handler).onReaderClosed(Mockito.any(ReaderClosedEvent.class));
        Mockito.verify(handler, Mockito.never()).onMessages(Mockito.any(DataReceivedEvent.class));
    }

    @Test
    public void shutdownBeforeInitTest() {
        ReadStreamMock mock = new ReadStreamMock();
        AsyncReader reader = reader(settings().build(), mock);
        CompletableFuture<Void> init = reader.init();
        mock.assertSentMessagesCount(1);
        mock.assertLastMessage().isInitRequest("consumer", "/test-topic");

        CompletableFuture<Void> shutdown = reader.shutdown();
        mock.assertIsClosed();
        Assert.assertTrue(init.isCompletedExceptionally());
        Assert.assertTrue(shutdown.isDone());
        mock.closeStream(Status.SUCCESS);

        Assert.assertTrue(shutdown.isDone());
        Assert.assertFalse(shutdown.isCompletedExceptionally());
        CompletionException ex = Assert.assertThrows(CompletionException.class, init::join);
        Assert.assertEquals("Reader closed with Status{code = SUCCESS}", ex.getCause().getMessage());
        Assert.assertSame(init, reader.init());
        Mockito.verify(handler).onReaderClosed(Mockito.any(ReaderClosedEvent.class));
        Mockito.verify(handler, Mockito.never()).onSessionStarted(Mockito.any(SessionStartedEvent.class));
    }

    @Test
    public void shutdownWithoutInitTest() {
        ReadStreamMock mock = new ReadStreamMock();
        AsyncReader reader = reader(settings().build(), mock);
        CompletableFuture<Void> shutdown = reader.shutdown();
        Assert.assertTrue(shutdown.isDone());
        Assert.assertFalse(shutdown.isCompletedExceptionally());
        Assert.assertSame(shutdown, reader.shutdown());

        CompletionException ex = Assert.assertThrows(CompletionException.class, () -> reader.init().join());
        Assert.assertEquals("Reader closed with Status{code = SUCCESS, issues = [Closed by client (S_INFO)]}",
                ex.getCause().getMessage());
        mock.assertIsNotStarted();
        mock.assertSentMessagesCount(0);
        Mockito.verifyNoInteractions(handler);
    }

    @Test
    public void readClosedPartitionTest() {
        ReadStreamMock mock = new ReadStreamMock();
        Queue<Runnable> decoding = new ArrayDeque<>();
        AsyncReader reader = reader(settings().setMaxMemoryUsageBytes(200000)
                .setDecompressionExecutor(decoding::add).build(), mock);
        reader.init();
        mock.responseInit("read-session-1");
        mock.assertSentMessagesCount(2);
        mock.assertLastMessage().isReadRequest(200000);

        mock.responseStartPartition("/test-topic", 123, 0);
        mock.assertLastMessage().isStartPartition(1);
        mock.responseStartPartition("/test-topic", 345, 0);
        mock.assertSentMessagesCount(4);
        mock.assertLastMessage().isStartPartition(2);
        Mockito.verify(handler, Mockito.times(2)).onStartPartitionSession(Mockito.any(StartPartitionSessionEvent.class));

        mock.responseData(10000).partition(1, 0).batch(Codec.GZIP, MSG1, MSG2, MSG3, MSG4, MSG5).and().send();
        mock.responseData(10000).partition(2, 0).batch(Codec.GZIP, MSG5, MSG4, MSG3, MSG2, MSG1).and().send();
        Mockito.verify(handler, Mockito.never()).onMessages(Mockito.any(DataReceivedEvent.class));

        // Stop the first partition before its queued messages are decoded.
        mock.responseStopPartition(1, true);
        mock.assertSentMessagesCount(5);
        mock.assertLastMessage().isStopPartition(1);
        ArgumentCaptor<StopPartitionSessionEvent> stopped = ArgumentCaptor.forClass(StopPartitionSessionEvent.class);
        Mockito.verify(handler).onStopPartitionSession(stopped.capture());
        Assert.assertEquals(1, stopped.getValue().getPartitionSessionId());

        while (!decoding.isEmpty()) {
            decoding.remove().run();
        }
        ArgumentCaptor<DataReceivedEvent> data = ArgumentCaptor.forClass(DataReceivedEvent.class);
        Mockito.verify(handler, Mockito.times(5)).onMessages(data.capture());

        List<Message> messages = new ArrayList<>();
        data.getAllValues().forEach(ev -> messages.addAll(ev.getMessages()));
        assertMessages(Arrays.asList(MSG5, MSG4, MSG3, MSG2, MSG1), messages, 0);

        Assert.assertEquals(345, data.getValue().getMessages().get(0).getPartitionSession().getPartitionId());
        mock.assertSentMessagesCount(6);
        mock.assertLastMessage().isReadRequest(20000);

        reader.shutdown();
        mock.closeStream(Status.SUCCESS);
    }

    @Test
    @HideLoggers({ BufferManager.class, ReaderImpl.class })
    public void invalidBatchesTest() {
        ReadStreamMock mock = new ReadStreamMock();
        AsyncReader reader = reader(settings().setMaxMemoryUsageBytes(2000).build(), mock);
        reader.init();
        mock.responseInit("read-session-1");
        mock.assertSentMessagesCount(2);
        mock.assertLastMessage().isReadRequest(2000);
        mock.responseStartPartition("/test-topic", 123, 0);
        mock.assertSentMessagesCount(3);
        mock.assertLastMessage().isStartPartition(1);

        // Empty batches release their memory without invoking the message handler.
        mock.responseData(1000).partition(1, 0).batch(Codec.RAW).and().send();
        mock.assertSentMessagesCount(4);
        mock.assertLastMessage().isReadRequest(1000);

        reader.shutdown();
        mock.responseData(1200).partition(1, 1000).batch(Codec.RAW, MSG1, MSG2, MSG3, MSG4, MSG5).and().send();
        mock.assertSentMessagesCount(5);
        mock.assertLastMessage().isReadRequest(1200);
        Mockito.verify(handler, Mockito.never()).onMessages(Mockito.any(DataReceivedEvent.class));
        mock.closeStream(Status.of(StatusCode.INTERNAL_ERROR));
    }

    @Test
    public void retrySkipsQueuedMessagesTest() {
        ErrorsHandler errorsHandler = new ErrorsHandler();
        ReadStreamMock m1 = new ReadStreamMock();
        ReadStreamMock m2 = new ReadStreamMock();
        Queue<Runnable> decoding = new ArrayDeque<>();
        AsyncReader reader = reader(settings().setMaxMemoryUsageBytes(2000)
                .setDecompressionExecutor(decoding::add)
                .setRetryConfig(IMMEDIATE_RETRY)
                .setErrorsHandler(errorsHandler)
                .build(), m1, m2);
        reader.init();
        m1.assertLastMessage().isInitRequest("consumer", "/test-topic");
        m1.responseInit("read-session-1");
        m1.assertLastMessage().isReadRequest(2000);
        m1.responseStartPartition("/test-topic", 123, 0);
        m1.assertLastMessage().isStartPartition(1);
        m1.responseData(1000).partition(1, 0).batch(Codec.RAW, MSG1, MSG2, MSG3).and().send();
        while (!decoding.isEmpty()) {
            decoding.remove().run();
        }

        ArgumentCaptor<DataReceivedEvent> data = ArgumentCaptor.forClass(DataReceivedEvent.class);
        Mockito.verify(handler).onMessages(data.capture());
        assertMessages(Arrays.asList(MSG1, MSG2, MSG3), data.getValue().getMessages(), 0);
        List<Message> messages = data.getValue().getMessages();
        CompletableFuture<Void> c1 = messages.get(0).commit();
        CompletableFuture<Void> c2 = messages.get(1).commit();
        m1.assertLastMessage().isCommit(1).hasPartitionOffset(1, OffsetsRange.of(1, 2));
        Assert.assertFalse(c1.isDone());
        Assert.assertFalse(c2.isDone());

        m1.responseCommitAck().partition(1, 1).send();
        Assert.assertTrue(c1.isDone());
        Assert.assertFalse(c1.isCompletedExceptionally());
        Assert.assertFalse(c2.isDone());
        ArgumentCaptor<CommitOffsetAcknowledgementEvent> ack =
                ArgumentCaptor.forClass(CommitOffsetAcknowledgementEvent.class);
        Mockito.verify(handler).onCommitResponse(ack.capture());
        Assert.assertEquals(1, ack.getValue().getCommittedOffset());
        Assert.assertEquals(123, ack.getValue().getPartitionSession().getPartitionId());

        // Leave a batch queued on the failed stream to check that it is discarded.
        m1.responseData(1000).partition(1, 3).batch(Codec.GZIP, MSG4, MSG2).and().send();
        errorsHandler.assertEmpty();
        m1.closeStream(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
        errorsHandler.assertCodes(StatusCode.TRANSPORT_UNAVAILABLE);
        Assert.assertTrue(c2.isCompletedExceptionally());
        Assert.assertTrue(messages.get(2).commit().isCompletedExceptionally());
        ArgumentCaptor<PartitionSessionClosedEvent> closed = ArgumentCaptor.forClass(PartitionSessionClosedEvent.class);
        Mockito.verify(handler).onPartitionSessionClosed(closed.capture());
        Assert.assertEquals(123, closed.getValue().getPartitionSession().getPartitionId());
        Mockito.verify(handler, Mockito.never()).onReaderClosed(Mockito.any(ReaderClosedEvent.class));

        m2.assertSentMessagesCount(1);
        m2.assertLastMessage().isInitRequest("consumer", "/test-topic");
        m2.responseInit("read-session-2");
        m2.assertLastMessage().isReadRequest(2000);
        m2.responseStartPartition("/test-topic", 123, 1);
        m2.assertLastMessage().isStartPartition(1);
        while (!decoding.isEmpty()) {
            decoding.remove().run();
        }
        Mockito.verify(handler).onMessages(Mockito.any(DataReceivedEvent.class));

        m2.responseData(1000).partition(1, 1).batch(Codec.RAW, MSG2, MSG3, MSG4, MSG5).and().send();
        while (!decoding.isEmpty()) {
            decoding.remove().run();
        }
        Mockito.verify(handler, Mockito.times(2)).onMessages(data.capture());
        assertMessages(Arrays.asList(MSG2, MSG3, MSG4, MSG5), data.getValue().getMessages(), 1);
        ArgumentCaptor<SessionStartedEvent> started = ArgumentCaptor.forClass(SessionStartedEvent.class);
        Mockito.verify(handler, Mockito.times(2)).onSessionStarted(started.capture());
        Assert.assertEquals("read-session-2", started.getValue().getSessionId());

        DeferredCommitter committer = DeferredCommitter.newInstance();
        committer.add(data.getValue().getMessages().get(0));
        committer.add(data.getValue().getMessages().get(2));
        committer.commit();
        m2.assertLastMessage().isCommit(1).hasPartitionOffset(1, OffsetsRange.of(1, 2), OffsetsRange.of(3, 4));
        reader.shutdown();
        m2.assertIsClosed();
        m2.closeStream(Status.SUCCESS);
    }
}
