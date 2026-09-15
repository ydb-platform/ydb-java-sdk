package tech.ydb.topic.write.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.HideLoggers;
import tech.ydb.topic.utils.HideLoggersRule;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Aleksandr Gorshenin
 */
public class WriterQueueTest {
    private static final Message SMALL_MSG = Message.of(new byte[] { 0x00, 0x01, 0x02, 0x03, 0x05 });

    @Rule
    public final HideLoggersRule hideLogger = new HideLoggersRule();

    private static Message smallMsg(int seqNo) {
        return Message.newBuilder().setData(SMALL_MSG.getData()).setSeqNo(seqNo).build();
    }
    private static WriterSettings rawSettings() {
        return WriterSettings.newBuilder()
                .setTopicPath("/test")
                .setCodec(Codec.RAW)
                .build();
    }

    private static WriterSettings gzipSettings() {
        return WriterSettings.newBuilder()
                .setTopicPath("/test")
                .setCodec(Codec.GZIP)
                .build();
    }

    private static WriterQueue rawQueue(AtomicInteger notifyCount) {
        return new WriterQueue("test", rawSettings(), new CodecRegistry(),
                Runnable::run, () -> notifyCount.incrementAndGet());
    }

    private static void assertOverflow(String msg, ThrowingRunnable runnable) {
        QueueOverflowException ex = Assert.assertThrows("Must be thrown QueueOverflowException",
                QueueOverflowException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    private static long assertSendAll(WriterQueue q, int messagesCount) {
        long lastSeqNo = 0;
        int read = 0;
        SentMessage sent = q.nextMessageToSend();
        while (sent != null) {
            lastSeqNo = sent.getSeqNo();
            read++;
            sent = q.nextMessageToSend();
        }
        Assert.assertEquals("Incorrect number of message in queue", messagesCount, read);
        return lastSeqNo;
    }

    @Test
    public void testInvalidCodecThrows() {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test")
                .setCodec(9999)
                .build();
        IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> new WriterQueue("test", settings, new CodecRegistry(), Runnable::run, () -> { }));
        Assert.assertEquals("Unsupported codec: 9999", ex.getMessage());
    }

    @Test
    public void testFlushWithNoMessages() {
        WriterQueue q = rawQueue(new AtomicInteger());
        CompletableFuture<Void> f = q.flush();
        Assert.assertTrue(f.isDone());
        Assert.assertFalse(f.isCompletedExceptionally());
    }

    @Test
    public void testRawCompressor() throws Exception {
        WriterQueue q = new WriterQueue("test", rawSettings(), new CodecRegistry(), null, () -> {});

        CompletableFuture<WriteAck> f1 = q.enqueue(SMALL_MSG, null);
        CompletableFuture<WriteAck> f2 = q.tryEnqueue(SMALL_MSG, null);
        CompletableFuture<WriteAck> f3 = q.tryEnqueue(SMALL_MSG, null, 1, TimeUnit.SECONDS);

        Assert.assertFalse(f1.isDone());
        Assert.assertFalse(f2.isDone());
        Assert.assertFalse(f3.isDone());

        long lastSeqNo = assertSendAll(q, 3);

        Assert.assertFalse(f1.isDone());
        Assert.assertFalse(f2.isDone());
        Assert.assertFalse(f3.isDone());

        q.confirmAck(new WriteAck(lastSeqNo, null, null, null));

        Assert.assertTrue(f1.isDone());
        Assert.assertTrue(f2.isDone());
        Assert.assertTrue(f3.isDone());
    }

    @Test
    @HideLoggers({ WriterImpl.class })
    public void testGzipNullCompressor() throws Exception {
        WriterQueue q = new WriterQueue("test", gzipSettings(), new CodecRegistry(), null, () -> {});

        CompletableFuture<WriteAck> f1 = q.enqueue(SMALL_MSG, null);
        CompletableFuture<WriteAck> f2 = q.tryEnqueue(SMALL_MSG, null);
        CompletableFuture<WriteAck> f3 = q.tryEnqueue(SMALL_MSG, null, 1, TimeUnit.SECONDS);

        Assert.assertFalse(f1.isDone());
        Assert.assertFalse(f2.isDone());
        Assert.assertFalse(f3.isDone());

        Assert.assertNull(q.nextMessageToSend()); // nothing to send, all messages were failed

        Assert.assertTrue(f1.isCompletedExceptionally());
        Assert.assertTrue(f2.isCompletedExceptionally());
        Assert.assertTrue(f3.isCompletedExceptionally());
    }

    @Test
    @HideLoggers({ WriterImpl.class })
    public void testWrongCodec() throws Exception {
        // Codec that always throws on encode
        Codec failingCodec = new Codec() {
            @Override
            public int getId() {
                return 9001;
            }

            @Override
            public InputStream decode(InputStream in) {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream encode(OutputStream out) throws IOException {
                throw new IOException("Simulated encoding failure");
            }
        };

        AtomicInteger notify = new AtomicInteger();
        CodecRegistry registry = new CodecRegistry(Arrays.asList(failingCodec));
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test")
                .setCodec(failingCodec.getId())
                .build();

        WriterQueue q = new WriterQueue("test", settings, registry, Runnable::run, notify::incrementAndGet);

        CompletableFuture<WriteAck> f1 = q.enqueue(SMALL_MSG, null);
        CompletableFuture<WriteAck> f2 = q.tryEnqueue(SMALL_MSG, null);
        CompletableFuture<WriteAck> f3 = q.tryEnqueue(SMALL_MSG, null, 1, TimeUnit.SECONDS);

        Assert.assertFalse(f1.isDone());
        Assert.assertFalse(f2.isDone());
        Assert.assertFalse(f3.isDone());

        Assert.assertNull(q.nextMessageToSend()); // nothing to send, all messages were failed

        Assert.assertTrue(f1.isCompletedExceptionally());
        Assert.assertTrue(f2.isCompletedExceptionally());
        Assert.assertTrue(f3.isCompletedExceptionally());
    }

    @Test
    @HideLoggers({ WriterImpl.class })
    public void badMsgBehindSentTest() throws Exception {
        // codec that fails only for messages whose first byte is 0x7F
        Codec selective = new Codec() {
            @Override
            public int getId() {
                return 10001;
            }

            @Override
            public InputStream decode(InputStream input) throws IOException {
                throw new IOException("not supported");
            }

            @Override
            public OutputStream encode(OutputStream output) throws IOException {
                return new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        if (b == 0x7F) {
                            throw new IOException("bad message");
                        }
                        output.write(b);
                    }
                };
            }
        };

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test")
                .setCodec(selective.getId())
                .build();
        AtomicInteger notify = new AtomicInteger();
        CodecRegistry registry = new CodecRegistry(Arrays.asList(selective));
        WriterQueue q = new WriterQueue("test", settings, registry, Runnable::run, notify::incrementAndGet);

        CompletableFuture<WriteAck> good1 = q.enqueue(Message.of(new byte[] { 0x01 }), null);
        CompletableFuture<WriteAck> bad = q.enqueue(Message.of(new byte[] { 0x7F }), null);
        CompletableFuture<WriteAck> good2 = q.enqueue(Message.of(new byte[] { 0x02 }), null);

        SentMessage s1 = q.nextMessageToSend();
        Assert.assertNotNull(s1);
        SentMessage s2 = q.nextMessageToSend();
        Assert.assertNotNull(s2);
        Assert.assertNull(q.nextMessageToSend());

        Assert.assertFalse(good1.isDone());
        Assert.assertFalse(good2.isDone());
        Assert.assertFalse(bad.isDone());

        // ack for the first message only -> sweep reaches the ProblemMsg
        q.confirmAck(new WriteAck(s1.getSeqNo(), WriteAck.State.WRITTEN, null, null));

        Assert.assertTrue(good1.isDone());
        Assert.assertTrue(bad.isCompletedExceptionally());
        Assert.assertFalse(good2.isDone());

    }

    @Test
    public void testFlushCompletesWhenMessageIsAcked() throws Exception {
        AtomicInteger notify = new AtomicInteger();
        WriterQueue q = rawQueue(notify);

        q.enqueue(SMALL_MSG, null);
        q.enqueue(SMALL_MSG, null);
        q.enqueue(SMALL_MSG, null);

        CompletableFuture<Void> flushFuture = q.flush();

        long lastSeqNo = assertSendAll(q, 3);

        Assert.assertFalse(flushFuture.isDone());
        q.confirmAck(new WriteAck(lastSeqNo, WriteAck.State.WRITTEN, null, null));

        Assert.assertTrue(flushFuture.isDone());
        Assert.assertFalse(flushFuture.isCompletedExceptionally());
    }

    @Test
    public void testSmallBufferWriting() throws QueueOverflowException {
        AtomicInteger notify = new AtomicInteger();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test")
                .setCodec(Codec.RAW)
                .setMaxSendBufferMemorySize(12)
                .build();

        WriterQueue q = new WriterQueue("test", settings, new CodecRegistry(), null, notify::incrementAndGet);

        q.tryEnqueue(smallMsg(10), null); // success
        q.tryEnqueue(smallMsg(20), null); // success
        assertOverflow("[test] Rejecting a message of 5 bytes: not enough space in message queue. "
                + "Buffer currently has 2 messages with 2 / 12 bytes available",
                () -> q.tryEnqueue(smallMsg(30), null));

        Assert.assertEquals(20, assertSendAll(q, 2));
        q.confirmAck(new WriteAck(10, null, null, null)); // free one message

        q.tryEnqueue(smallMsg(30), null); // success
        assertOverflow("[test] Rejecting a message of 5 bytes: not enough space in message queue. "
                + "Buffer currently has 2 messages with 2 / 12 bytes available",
                () -> q.tryEnqueue(smallMsg(40), null));

        Assert.assertEquals(30, assertSendAll(q, 1));
        q.confirmAck(new WriteAck(30, null, null, null)); // free one message
    }

    @Test
    public void testIncorrectSeqNumbers() throws Exception {
        WriterQueue q = rawQueue(new AtomicInteger());

        CompletableFuture<WriteAck> f1 = q.enqueue(smallMsg(10), null);
        CompletableFuture<WriteAck> f2 = q.enqueue(smallMsg(20), null);
        CompletableFuture<WriteAck> f3 = q.enqueue(smallMsg(20), null);
        CompletableFuture<WriteAck> f4 = q.enqueue(smallMsg(30), null);
        CompletableFuture<WriteAck> f5 = q.enqueue(smallMsg(11), null);
        CompletableFuture<WriteAck> f6 = q.enqueue(smallMsg(40), null);

        Assert.assertFalse(f1.isDone());
        Assert.assertFalse(f2.isDone());
        Assert.assertFalse(f3.isDone());
        Assert.assertFalse(f4.isDone());
        Assert.assertFalse(f5.isDone());
        Assert.assertFalse(f6.isDone());

        long lastSeqNo = assertSendAll(q, 6);
        Assert.assertEquals(40, lastSeqNo);
        // all message must be resent
        List<SentMessage> resend = q.updateSeqNo(15);

        Assert.assertEquals(6, resend.size());
        Assert.assertEquals(10, resend.get(0).getSeqNo());
        Assert.assertEquals(20, resend.get(1).getSeqNo());
        Assert.assertEquals(20, resend.get(2).getSeqNo());
        Assert.assertEquals(30, resend.get(3).getSeqNo());
        Assert.assertEquals(11, resend.get(4).getSeqNo());
        Assert.assertEquals(40, resend.get(5).getSeqNo());

        Assert.assertFalse(f1.isDone());
        Assert.assertFalse(f2.isDone());
        Assert.assertFalse(f3.isDone());
        Assert.assertFalse(f4.isDone());
        Assert.assertFalse(f5.isDone());
        Assert.assertFalse(f6.isDone());

        // all message must be confirmed
        q.confirmAck(new WriteAck(10, WriteAck.State.ALREADY_WRITTEN, null, null));
        q.confirmAck(new WriteAck(20, WriteAck.State.WRITTEN, null, null));
        q.confirmAck(new WriteAck(20, WriteAck.State.ALREADY_WRITTEN, null, null)); // will be skipped
        q.confirmAck(new WriteAck(30, WriteAck.State.WRITTEN, null, null));
        q.confirmAck(new WriteAck(11, WriteAck.State.ALREADY_WRITTEN, null, null)); // will be skipped
        q.confirmAck(new WriteAck(40, WriteAck.State.WRITTEN, null, null));

        Assert.assertTrue(f1.isDone());
        Assert.assertTrue(f2.isDone());
        Assert.assertTrue(f3.isDone());
        Assert.assertTrue(f4.isDone());
        Assert.assertTrue(f5.isDone());
        Assert.assertTrue(f6.isDone());

        Assert.assertEquals(WriteAck.State.ALREADY_WRITTEN, f1.join().getState());
        Assert.assertEquals(10, f1.join().getSeqNo());
        Assert.assertEquals(WriteAck.State.WRITTEN, f2.join().getState());
        Assert.assertEquals(20, f2.join().getSeqNo());
        Assert.assertEquals(WriteAck.State.WRITTEN, f3.join().getState());
        Assert.assertEquals(20, f3.join().getSeqNo());
        Assert.assertEquals(WriteAck.State.WRITTEN, f4.join().getState());
        Assert.assertEquals(30, f4.join().getSeqNo());
        Assert.assertEquals(WriteAck.State.WRITTEN, f5.join().getState());
        Assert.assertEquals(30, f5.join().getSeqNo());
        Assert.assertEquals(WriteAck.State.WRITTEN, f6.join().getState());
        Assert.assertEquals(40, f6.join().getSeqNo());
    }

    @Test
    public void testLostAcks() throws Exception {
        WriterQueue q = rawQueue(new AtomicInteger());

        List<SentMessage> before = q.updateSeqNo(0);
        Assert.assertTrue(before.isEmpty());

        CompletableFuture<WriteAck> f1 = q.enqueue(smallMsg(10), null);
        CompletableFuture<WriteAck> f2 = q.enqueue(smallMsg(20), null);
        CompletableFuture<WriteAck> f3 = q.enqueue(smallMsg(30), null);
        CompletableFuture<WriteAck> f4 = q.enqueue(smallMsg(40), null);
        CompletableFuture<WriteAck> f5 = q.enqueue(smallMsg(50), null);

        Assert.assertFalse(f1.isDone());
        Assert.assertFalse(f2.isDone());
        Assert.assertFalse(f3.isDone());
        Assert.assertFalse(f4.isDone());
        Assert.assertFalse(f5.isDone());

        long lastSeqNo = assertSendAll(q, 5);
        Assert.assertEquals(50, lastSeqNo);

        q.confirmAck(new WriteAck(10, WriteAck.State.WRITTEN, null, null));

        Assert.assertTrue(f1.isDone());
        Assert.assertEquals(WriteAck.State.WRITTEN, f1.join().getState());
        Assert.assertFalse(f2.isDone());
        Assert.assertFalse(f3.isDone());
        Assert.assertFalse(f4.isDone());
        Assert.assertFalse(f5.isDone());

        // lost others acks and reconnect with new lastSeqNo
        List<SentMessage> retry = q.updateSeqNo(30);

        // All messages without acks will be resent
        Assert.assertEquals(4, retry.size());
        Assert.assertEquals(20, retry.get(0).getSeqNo());
        Assert.assertEquals(30, retry.get(1).getSeqNo());
        Assert.assertEquals(40, retry.get(2).getSeqNo());
        Assert.assertEquals(50, retry.get(3).getSeqNo());

        Assert.assertFalse(f2.isDone());
        Assert.assertFalse(f3.isDone());
        Assert.assertFalse(f4.isDone());
        Assert.assertFalse(f5.isDone());

        q.confirmAck(new WriteAck(20, WriteAck.State.ALREADY_WRITTEN, null, null));
        q.confirmAck(new WriteAck(30, WriteAck.State.ALREADY_WRITTEN, null, null));
        q.confirmAck(new WriteAck(40, WriteAck.State.WRITTEN, null, null));
        q.confirmAck(new WriteAck(50, WriteAck.State.WRITTEN, null, null));

        Assert.assertTrue(f2.isDone());
        Assert.assertTrue(f3.isDone());
        Assert.assertTrue(f4.isDone());
        Assert.assertTrue(f5.isDone());
        Assert.assertEquals(WriteAck.State.ALREADY_WRITTEN, f2.join().getState());
        Assert.assertEquals(WriteAck.State.ALREADY_WRITTEN, f3.join().getState());
        Assert.assertEquals(WriteAck.State.WRITTEN, f4.join().getState());
        Assert.assertEquals(WriteAck.State.WRITTEN, f5.join().getState());
    }
}
