package tech.ydb.topic.write.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.Mockito;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.settings.TopicRetryConfig;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Aleksandr Gorshenin
 */
public class WriterImplTest {
    private static final RetryConfig IMMEDIATELY_FOREVER = status -> (number, elapsed) -> 0;

    private static TopicRpc mockRpc(StreamMock first, StreamMock... rest) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.writeSession(Mockito.any(String.class))).thenReturn(first, rest);
        return rpc;
    }

    private static WriterImpl createWriter(TopicRpc rpc) {
        return createWriter(rpc, TopicRetryConfig.NEVER);
    }

    private static WriterImpl createWriter(TopicRpc rpc, RetryConfig retryConfig) {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test/topic")
                .setProducerId("test-producer")
                .setCodec(Codec.RAW)
                .setRetryConfig(retryConfig)
                .build();
        return new WriterImpl(rpc, settings, Runnable::run, new CodecRegistry());
    }

    private static void assertIllegalState(String msg, ThrowingRunnable runnable) {
        IllegalStateException ex = Assert.assertThrows("Must be thrown IllegalStateException",
                IllegalStateException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    private static void assertRuntimeException(String msg, ThrowingRunnable runnable) {
        RuntimeException ex = Assert.assertThrows("Must be thrown RuntimeException",
                RuntimeException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    private static ThrowingRunnable futureGet(CompletableFuture<?> future) {
        return () -> {
            try {
                future.get();
            } catch (ExecutionException ex) {
                throw ex.getCause();
            }
        };
    }

    private static final Message MSG1 = Message.of(new byte[] { 0x00, 0x01, 0x02 });

    @Test
    public void doubleInitTest() throws Exception {
        StreamMock s = new StreamMock();
        WriterImpl writer = createWriter(mockRpc(s));

        CompletableFuture<WriteAck> m1 = writer.blockingSend(MSG1, null);
        CompletableFuture<WriteAck> m2 = writer.blockingSend(MSG1, null);

        Assert.assertFalse(m1.isDone());
        Assert.assertFalse(m2.isDone());

        CompletableFuture<InitResult> initFuture = writer.init();

        Assert.assertEquals(1, s.messages.size()); // init req
        Assert.assertSame(initFuture, writer.init());
        Assert.assertEquals(1, s.messages.size()); // init req

        s.sendInitResponse(123L);

        Assert.assertEquals(2, s.messages.size()); // init req + write request

        Assert.assertFalse(m1.isDone());
        Assert.assertFalse(m2.isDone());

        s.sendAckResponse(124, 1);
        s.sendAckResponse(125, 2);

        Assert.assertEquals(124, m1.join().getSeqNo());
        Assert.assertEquals(125, m2.join().getSeqNo());

        writer.shutdown();

        Assert.assertNotNull(s.observer);
        Assert.assertTrue(s.isClosed);
        Assert.assertFalse(s.isCanceled);
    }

    @Test
    public void closeBeforeInitTest() throws Exception {
        StreamMock s = new StreamMock();
        WriterImpl writer = createWriter(mockRpc(s));

        CompletableFuture<WriteAck> m1 = writer.blockingSend(MSG1, null);
        CompletableFuture<WriteAck> m2 = writer.blockingSend(MSG1, null);

        Assert.assertFalse(m1.isDone());
        Assert.assertFalse(m2.isDone());

        CompletableFuture<Status> closeFuture = writer.shutdown();

        Assert.assertTrue(closeFuture.isDone());
        Assert.assertTrue(m1.isDone());
        Assert.assertTrue(m2.isDone());

        Assert.assertSame(closeFuture, writer.shutdown());

        String expected = "Status{code = SUCCESS, issues = [Closed by client (S_INFO)]}";
        assertIllegalState("Writer is already stopped with " + expected, writer::init);
        assertIllegalState("Writer is already stopped with " + expected, () -> writer.blockingSend(MSG1, null));
        assertIllegalState("Writer is already stopped with " + expected, () -> writer.blockingSend(MSG1, null, 1,
                TimeUnit.SECONDS));
        assertIllegalState("Writer is already stopped with " + expected, () -> writer.nonblockingSend(MSG1, null));

        Assert.assertTrue(m1.isCompletedExceptionally());
        Assert.assertTrue(m2.isCompletedExceptionally());

        assertRuntimeException("Message sending was cancelled with " + expected, futureGet(m1));
        assertRuntimeException("Message sending was cancelled with " + expected, futureGet(m2));

        Assert.assertNull(s.observer);
        Assert.assertFalse(s.isClosed);
        Assert.assertFalse(s.isCanceled);
    }

    @Test
    public void shutdownCancelsPendingMessages() throws Exception {
        StreamMock s = new StreamMock();
        WriterImpl writer = createWriter(mockRpc(s));
        writer.init();

        s.sendInitResponse(0L);

        CompletableFuture<WriteAck> m1 = writer.blockingSend(MSG1, null);
        CompletableFuture<WriteAck> m2 = writer.blockingSend(MSG1, null);

        writer.shutdown();
        Assert.assertTrue(s.isClosed);
        s.close(Status.of(StatusCode.SUCCESS));

        Assert.assertTrue(m1.isCompletedExceptionally());
        Assert.assertTrue(m2.isCompletedExceptionally());

        assertRuntimeException("Message had been sent but the writer was stopped with Status{code = SUCCESS}",
                futureGet(m1));
        assertRuntimeException("Message had been sent but the writer was stopped with Status{code = SUCCESS}",
                futureGet(m2));
    }

    @Test
    public void streamFailureTest() throws Exception {
        StreamMock s = new StreamMock();
        WriterImpl writer = createWriter(mockRpc(s));

        CompletableFuture<InitResult> initFuture = writer.init();
        CompletableFuture<WriteAck> m1 = writer.blockingSend(MSG1, null);
        CompletableFuture<WriteAck> m2 = writer.blockingSend(MSG1, null);
        CompletableFuture<Void> flushFuture = writer.flush();

        Assert.assertFalse(initFuture.isDone());
        Assert.assertFalse(m1.isDone());
        Assert.assertFalse(m2.isDone());
        Assert.assertFalse(flushFuture.isDone());

        s.close(Status.of(StatusCode.SCHEME_ERROR));

        String expected = " with Status{code = SCHEME_ERROR(code=400070)}";
        assertIllegalState("Writer is already stopped" + expected, () -> writer.blockingSend(MSG1, null));
        assertIllegalState("Writer is already stopped" + expected, () -> writer.blockingSend(MSG1, null, 1,
                TimeUnit.SECONDS));
        assertIllegalState("Writer is already stopped" + expected, () -> writer.nonblockingSend(MSG1, null));

        Assert.assertTrue(initFuture.isCompletedExceptionally());
        Assert.assertTrue(m1.isCompletedExceptionally());
        Assert.assertTrue(m2.isCompletedExceptionally());
        Assert.assertTrue(flushFuture.isDone());
        Assert.assertFalse(flushFuture.isCompletedExceptionally());

        CompletableFuture<Status> shutdownFuture = writer.shutdown();

        Assert.assertTrue(shutdownFuture.isDone());
        Assert.assertFalse(shutdownFuture.isCompletedExceptionally());

        Assert.assertTrue(m1.isCompletedExceptionally());
        Assert.assertTrue(m2.isCompletedExceptionally());

        assertRuntimeException("Message sending was cancelled" + expected, futureGet(m1));
        assertRuntimeException("Message sending was cancelled" + expected, futureGet(m2));

        Assert.assertNotNull(s.observer);
        Assert.assertFalse(s.isClosed); // stream was closed itself
        Assert.assertFalse(s.isCanceled);
    }

    @Test
    public void withSeqNoConsistencyTest() throws Exception {
        StreamMock s = new StreamMock();
        WriterImpl writer = createWriter(mockRpc(s));
        writer.init();
        s.sendInitResponse(0L);

        // first message without seqNo — establishes isSeqNoProvided = false
        Message msg1 = Message.of("msg1".getBytes());
        writer.nonblockingSend(msg1, null);

        // second message WITH seqNo must fail
        Message msg2 = Message.newBuilder().setData("msg2".getBytes()).setSeqNo(2L).build();
        assertRuntimeException("SeqNo was provided for a message after it had not been provided for another message. "
                + "SeqNo should either be provided for all messages or none of them.",
                () -> writer.nonblockingSend(msg2, null));
    }

    @Test
    public void withSeqNoIdempotencyTest() throws Exception {
        StreamMock s = new StreamMock();
        WriterImpl writer = createWriter(mockRpc(s));
        writer.init();
        s.sendInitResponse(0L);

        byte[][] msgList = new byte[100][];
        for (int idx = 0; idx < 100; idx++) {
            msgList[idx] = new byte[idx * 3 + 1];
            Arrays.fill(msgList[idx], (byte) idx);
        }

        AtomicInteger written = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        Runnable func = () -> {
            try {
                Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw new AssertionError("cannot start a worker", ex);
            }

            for (int idx = 0; idx < 100; idx++) {
                try {
                    writer.blockingSend(Message.newBuilder().setData(msgList[idx]).setSeqNo(idx * 3 + 7).build(), null);
                    written.incrementAndGet();
                } catch (IllegalArgumentException ex) {
                    Assert.assertEquals("SeqNo provided for a message is less or equal than SeqNo provided for "
                            + "previous message. SeqNo must be strictly growing.", ex.getMessage());
                    failed.incrementAndGet();
                } catch (InterruptedException | QueueOverflowException ex) {
                    throw new AssertionError("cannot write to queue", ex);
                }
            }
        };
        CompletableFuture<?>[] tasks = new CompletableFuture<?>[10];
        for (int idx = 0; idx < 10; idx++) {
            tasks[idx] = CompletableFuture.runAsync(func);
        }
        latch.countDown();
        CompletableFuture.allOf(tasks).join();
        Assert.assertEquals(100, written.get());
        Assert.assertEquals(900, failed.get());
    }

    @Test
    public void withOutSeqNoConsistencyTest() throws Exception {
        StreamMock s = new StreamMock();
        WriterImpl writer = createWriter(mockRpc(s));
        writer.init();
        s.sendInitResponse(0L);

        // message with negotive seqNo must fail
        Message msg0 = Message.newBuilder().setData("msg2".getBytes()).setSeqNo(0L).build();
        assertRuntimeException("SeqNo provided for a message must be greater than zero.",
                () -> writer.nonblockingSend(msg0, null));

        // first message with seqNo — establishes isSeqNoProvided = true
        Message msg1 = Message.newBuilder().setData("msg2".getBytes()).setSeqNo(1L).build();
        writer.nonblockingSend(msg1, null);

        // message WITHOUT seqNo must fail
        Message msg2 = Message.of("msg2".getBytes());
        assertRuntimeException("SeqNo was not provided for a message after it had been provided for another message. "
                + "SeqNo should either be provided for all messages or none of them.",
                () -> writer.nonblockingSend(msg2, null));

        // message with negative seqNo must fail
        Message msg3 = Message.newBuilder().setData("msg2".getBytes()).setSeqNo(-1L).build();
        assertRuntimeException("SeqNo provided for a message must be greater than zero.",
                () -> writer.nonblockingSend(msg3, null));
    }

    @Test
    public void retryResendsPendingMessagesTest() throws Exception {
        StreamMock s1 = new StreamMock();
        StreamMock s2 = new StreamMock();
        WriterImpl writer = createWriter(mockRpc(s1, s2), IMMEDIATELY_FOREVER);

        writer.init();
        s1.sendInitResponse(0L);

        CompletableFuture<WriteAck> m1 = writer.nonblockingSend(MSG1, null);
        CompletableFuture<WriteAck> m2 = writer.nonblockingSend(MSG1, null);

        Assert.assertEquals(3, s1.messages.size()); // init req + 2 write requests
        Assert.assertFalse(m1.isDone());
        Assert.assertFalse(m2.isDone());

        // stream 1 fails — first retry delay is 0ms, so start() is called synchronously
        s1.close(Status.of(StatusCode.UNAVAILABLE));

        // stream 2 is now connected; lastSeqNo=0 means message was not yet persisted
        s2.sendInitResponse(0L);

        Assert.assertEquals(2, s2.messages.size()); // init req + write request (with two messages)

        s2.sendAckResponse(1L, 42L);

        Assert.assertTrue(m1.isDone());
        Assert.assertEquals(1L, m1.join().getSeqNo());
        Assert.assertEquals(WriteAck.State.WRITTEN, m1.join().getState());

        Assert.assertFalse(m2.isDone());
    }

    @Test
    public void writerCountersCountAcceptedMessagesAndAcknowledgements() throws Exception {
        RecordingMeter meter = new RecordingMeter();
        StreamMock firstStream = new StreamMock();
        StreamMock replacementStream = new StreamMock();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test/topic").setCodec(Codec.RAW)
                .setRetryConfig(IMMEDIATELY_FOREVER).setMaxSendBufferMessagesCount(3)
                .withMeter(meter, "writer").build();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(firstStream, replacementStream))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(settings);
            try {
                CompletableFuture<InitResult> initialized = writer.init();
                firstStream.sendInitResponse(0);
                initialized.get(1, TimeUnit.SECONDS);
                Message message = Message.newBuilder().setData(new byte[]{1, 2})
                        .addMetadataItem(new MetadataItem("key", new byte[10])).build();
                CompletableFuture<WriteAck> first = writer.send(message);
                CompletableFuture<WriteAck> second = writer.send(Message.of(new byte[]{3, 4, 5}));
                CompletableFuture<WriteAck> third = writer.send(Message.of(new byte[0]));
                Assert.assertThrows(QueueOverflowException.class, () -> writer.send(MSG1));
                Assert.assertEquals(3, meter.value("sending.messages"));
                Assert.assertEquals(5, meter.value("sending.bytes"));
                Assert.assertEquals(0, meter.value("written.messages"));
                Assert.assertEquals(3, meter.count("sending.bytes"));

                firstStream.close(Status.of(StatusCode.UNAVAILABLE));
                replacementStream.sendInitResponse(0);
                Assert.assertEquals(3, meter.value("sending.messages"));
                Assert.assertEquals(5, meter.value("sending.bytes"));
                CompletableFuture<Void> observed = first.thenAccept(ack ->
                        Assert.assertEquals(1, meter.value("written.messages")));
                replacementStream.sendAckResponse(1, 10);
                observed.get(1, TimeUnit.SECONDS);
                Assert.assertEquals(WriteAck.State.WRITTEN, first.get().getState());
                Assert.assertFalse(second.isDone());
                Assert.assertFalse(third.isDone());

                YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.Builder skipped =
                        YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.newBuilder().setSeqNo(2);
                skipped.getSkippedBuilder();
                YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.Builder inTx =
                        YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.newBuilder().setSeqNo(3);
                inTx.getWrittenInTxBuilder();
                replacementStream.sendAckResponse(skipped.build(), inTx.build());
                Assert.assertEquals(WriteAck.State.ALREADY_WRITTEN, second.get(1, TimeUnit.SECONDS).getState());
                Assert.assertEquals(WriteAck.State.WRITTEN_IN_TX, third.get(1, TimeUnit.SECONDS).getState());
                Assert.assertEquals(3, meter.value("written.messages"));
                replacementStream.sendAckResponse(3, 12);
                Assert.assertEquals(3, meter.value("written.messages"));
                for (String name : Arrays.asList("sending.messages", "sending.bytes", "written.messages")) {
                    meter.assertAttributes(name, Attr.of("topic", "/test/topic"), Attr.of("writer.name", "writer"));
                }

                CompletableFuture<Void> closed = writer.shutdown();
                replacementStream.close(Status.SUCCESS);
                closed.get(1, TimeUnit.SECONDS);
                Assert.assertThrows(IllegalStateException.class, () -> writer.send(MSG1));
                Assert.assertEquals(3, meter.value("sending.messages"));
                Assert.assertEquals(5, meter.value("sending.bytes"));
                Assert.assertEquals(3, meter.value("written.messages"));
            } finally {
                writer.shutdown();
                firstStream.close(Status.SUCCESS);
                replacementStream.close(Status.SUCCESS);
            }
        }
    }

    @Test
    public void writerCountersRetainAcceptedMessagesWithoutAcknowledgements() throws Exception {
        RecordingMeter meter = new RecordingMeter();
        WriterSettings settings = WriterSettings.newBuilder().setTopicPath("/test/topic")
                .withMeter(meter, "writer").setRetryConfig(TopicRetryConfig.NEVER).build();
        StreamMock stream = new StreamMock();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream)).setCompressionExecutor(task -> {
            throw new RejectedExecutionException("Encoding task rejected");
        }).build()) {
            AsyncWriter writer = client.createAsyncWriter(settings);
            try {
                writer.init();
                stream.sendInitResponse(0);
                CompletableFuture<WriteAck> rejected = writer.send(MSG1);
                Assert.assertTrue(rejected.isCompletedExceptionally());
                Assert.assertEquals(1, meter.value("sending.messages"));
                Assert.assertEquals(MSG1.getData().length, meter.value("sending.bytes"));
                Assert.assertEquals(0, meter.value("written.messages"));
            } finally {
                writer.shutdown();
                stream.close(Status.SUCCESS);
            }
        }

        WriterSettings rawSettings = WriterSettings.newBuilder().setTopicPath("/test/topic").setCodec(Codec.RAW)
                .withMeter(meter, "writer").build();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(new StreamMock()))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(rawSettings);
            CompletableFuture<WriteAck> pending;
            try {
                pending = writer.send(MSG1);
                Assert.assertFalse(pending.isDone());
            } finally {
                writer.shutdown().get(1, TimeUnit.SECONDS);
            }
            Assert.assertTrue(pending.isCompletedExceptionally());
            Assert.assertEquals(2, meter.value("sending.messages"));
            Assert.assertEquals(2 * MSG1.getData().length, meter.value("sending.bytes"));
            Assert.assertEquals(0, meter.value("written.messages"));
        }
    }

    @Test
    public void writerMetricsRequireExplicitName() {
        Assert.assertSame(Meter.NOOP, WriterSettings.newBuilder().build().getMeter());
        Assert.assertNull(WriterSettings.newBuilder().build().getWriterName());
        Assert.assertThrows(IllegalArgumentException.class,
                () -> WriterSettings.newBuilder().withMeter(null, "writer"));
        for (String name : Arrays.asList(null, "", " ")) {
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> WriterSettings.newBuilder().withMeter(new RecordingMeter(), name));
        }
        RecordingMeter meter = new RecordingMeter();
        WriterSettings settings = WriterSettings.newBuilder().withMeter(meter, "writer").build();
        Assert.assertSame(meter, settings.getMeter());
        Assert.assertEquals("writer", settings.getWriterName());
    }

    private static class RecordingMeter implements Meter {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> measurements = new ConcurrentHashMap<>();
        private final Map<String, Attr[]> attributes = new ConcurrentHashMap<>();

        @Override
        public LongCounter createCounter(String name, String unit, String description) {
            AtomicLong counter = counters.computeIfAbsent(name, key -> new AtomicLong());
            AtomicLong count = measurements.computeIfAbsent(name, key -> new AtomicLong());
            return (value, attrs) -> {
                counter.addAndGet(value);
                count.incrementAndGet();
                attributes.put(name, attrs);
            };
        }

        long count(String name) {
            return measurements.get("ydb.topic.writer." + name).get();
        }

        long value(String name) {
            AtomicLong counter = counters.get("ydb.topic.writer." + name);
            return counter == null ? 0 : counter.get();
        }

        void assertAttributes(String name, Attr... expected) {
            Attr[] observed = attributes.get("ydb.topic.writer." + name);
            Assert.assertEquals(expected.length, observed.length);
            for (Attr attribute : expected) {
                Assert.assertTrue(Arrays.stream(observed).anyMatch(actual ->
                        attribute.getKey().equals(actual.getKey()) && attribute.getValue().equals(actual.getValue())));
            }
        }
    }

    private static class StreamMock implements GrpcReadWriteStream<FromServer, FromClient> {
        private final CompletableFuture<Status> future = new CompletableFuture<>();
        private final List<FromClient> messages = new ArrayList<>();
        private GrpcReadStream.Observer<FromServer> observer = null;
        private boolean isClosed = false;
        private boolean isCanceled = false;

        void sendInitResponse(long lastSeqNo) {
            observer.onNext(FromServer.newBuilder()
                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                    .setInitResponse(YdbTopic.StreamWriteMessage.InitResponse.newBuilder()
                            .setLastSeqNo(lastSeqNo)
                            .setSessionId("test-session")
                            .build())
                    .build());
        }

        void sendAckResponse(long seqNo, long offset) {
            sendAckResponse(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.newBuilder()
                    .setSeqNo(seqNo)
                    .setWritten(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.Written.newBuilder()
                            .setOffset(offset).build())
                    .build());
        }

        void sendAckResponse(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck... acks) {
            observer.onNext(FromServer.newBuilder()
                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                    .setWriteResponse(YdbTopic.StreamWriteMessage.WriteResponse.newBuilder()
                            .addAllAcks(Arrays.asList(acks))
                            .build())
                    .build()
            );
        }


        void close(Status status) {
            future.complete(status);
        }

        @Override
        public String authToken() {
            return "token";
        }

        @Override
        public void sendNext(FromClient message) {
            messages.add(message);
        }

        @Override
        public void close() {
            this.isClosed = true;
        }

        @Override
        public CompletableFuture<Status> start(GrpcReadStream.Observer<FromServer> observer) {
            this.observer = observer;
            return future;
        }

        @Override
        public void cancel() {
            this.isCanceled = true;
        }
    }

}
