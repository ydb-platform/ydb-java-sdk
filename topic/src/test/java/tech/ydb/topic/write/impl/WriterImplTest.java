package tech.ydb.topic.write.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.Mockito;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.settings.TopicRetryConfig;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Aleksandr Gorshenin
 */
public class WriterImplTest {
    private static final RetryConfig IMMEDIATELY_FOREVER = status -> (number, elapsed) -> 0;

    private static TopicRpc mockRpc(StreamMock first, StreamMock... rest) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.writeSession(Mockito.any())).thenReturn(first, rest);
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

        CompletableFuture<Void> closeFuture = writer.shutdown();

        Assert.assertTrue(closeFuture.isDone());
        Assert.assertTrue(m1.isDone());
        Assert.assertTrue(m2.isDone());

        Assert.assertSame(closeFuture, writer.shutdown());

        assertIllegalState("Writer is already stopped", writer::init);
        assertIllegalState("Writer is already stopped", () -> writer.blockingSend(MSG1, null));
        assertIllegalState("Writer is already stopped", () -> writer.blockingSend(MSG1, null, 1, TimeUnit.SECONDS));
        assertIllegalState("Writer is already stopped", () -> writer.nonblockingSend(MSG1, null));

        Assert.assertTrue(m1.isCompletedExceptionally());
        Assert.assertTrue(m2.isCompletedExceptionally());

        assertRuntimeException("Message sending was cancelled with status Status{code = SUCCESS, "
                + "issues = [Closed by client (S_INFO)]}", futureGet(m1));
        assertRuntimeException("Message sending was cancelled with status Status{code = SUCCESS, "
                + "issues = [Closed by client (S_INFO)]}", futureGet(m2));

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

        assertRuntimeException("Message had been sent but the writer was stopped with status Status{code = SUCCESS}",
                futureGet(m1));
        assertRuntimeException("Message had been sent but the writer was stopped with status Status{code = SUCCESS}",
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

        assertIllegalState("Writer is already stopped", () -> writer.blockingSend(MSG1, null));
        assertIllegalState("Writer is already stopped", () -> writer.blockingSend(MSG1, null, 1, TimeUnit.SECONDS));
        assertIllegalState("Writer is already stopped", () -> writer.nonblockingSend(MSG1, null));

        Assert.assertTrue(initFuture.isCompletedExceptionally());
        Assert.assertTrue(m1.isCompletedExceptionally());
        Assert.assertTrue(m2.isCompletedExceptionally());
        Assert.assertTrue(flushFuture.isDone());
        Assert.assertFalse(flushFuture.isCompletedExceptionally());

        CompletableFuture<Void> shutdownFuture = writer.shutdown();

        Assert.assertTrue(shutdownFuture.isDone());
        Assert.assertFalse(shutdownFuture.isCompletedExceptionally());

        Assert.assertTrue(m1.isCompletedExceptionally());
        Assert.assertTrue(m2.isCompletedExceptionally());

        assertRuntimeException("Message sending was cancelled with status Status{code = SCHEME_ERROR(code=400070)}",
                futureGet(m1));
        assertRuntimeException("Message sending was cancelled with status Status{code = SCHEME_ERROR(code=400070)}",
                futureGet(m2));

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
    public void withOutSeqNoConsistencyTest() throws Exception {
        StreamMock s = new StreamMock();
        WriterImpl writer = createWriter(mockRpc(s));
        writer.init();
        s.sendInitResponse(0L);

        // first message with seqNo — establishes isSeqNoProvided = true
        Message msg1 = Message.newBuilder().setData("msg2".getBytes()).setSeqNo(1L).build();
        writer.nonblockingSend(msg1, null);

        // second message WITHOUT seqNo must fail
        Message msg2 = Message.of("msg2".getBytes());
        assertRuntimeException("SeqNo was not provided for a message after it had been provided for another message. "
                + "SeqNo should either be provided for all messages or none of them.",
                () -> writer.nonblockingSend(msg2, null));
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
            observer.onNext(FromServer.newBuilder()
                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                    .setWriteResponse(YdbTopic.StreamWriteMessage.WriteResponse.newBuilder()
                            .addAcks(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.newBuilder()
                                    .setSeqNo(seqNo)
                                    .setWritten(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.Written.newBuilder()
                                            .setOffset(offset)
                                            .build())
                                    .build())
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
