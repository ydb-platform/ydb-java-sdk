package tech.ydb.topic.write.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.settings.TopicRetryConfig;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * Characterizes the existing public AsyncWriter contract used by the multiwriter.
 * These observations deliberately do not assert a server-side split or commit guarantee.
 */
public class MultiWriterPublicContractTest {
    @Test
    public void overflowConsumesExplicitSequenceNumber() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        AsyncWriter writer = writer(stream, settings().setMaxSendBufferMessagesCount(1).build());
        writer.init();
        stream.sendInitResponse(0);
        CompletableFuture<WriteAck> first = writer.send(message(41));
        Assert.assertThrows(QueueOverflowException.class, () -> writer.send(message(42)));
        stream.sendAckResponse(41, 0);
        Assert.assertTrue(first.isDone());
        Assert.assertThrows(IllegalArgumentException.class, () -> writer.send(message(42)));
        CompletableFuture<WriteAck> next = writer.send(message(43));
        stream.sendAckResponse(43, 1);
        Assert.assertEquals(43, next.join().getSeqNo());
        close(writer, stream);
    }

    @Test
    public void byteOverflowAlsoConsumesExplicitSequenceNumber() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        AsyncWriter writer = writer(stream, settings().setMaxSendBufferMemorySize(1).build());
        writer.init();
        stream.sendInitResponse(0);
        writer.send(message(41));
        Assert.assertThrows(QueueOverflowException.class, () -> writer.send(message(42)));
        stream.sendAckResponse(41, 0);
        Assert.assertThrows(IllegalArgumentException.class, () -> writer.send(message(42)));
        close(writer, stream);
    }

    @Test
    public void shutdownBeforeInitFailsQueuedMessagesWithoutOpeningStream() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        AsyncWriter writer = writer(stream, settings().build());
        CompletableFuture<WriteAck> pending = writer.send(message(1));
        Assert.assertTrue(writer.shutdown().isDone());
        Assert.assertTrue(pending.isCompletedExceptionally());
        Assert.assertNull(stream.observer);
        Assert.assertThrows(IllegalStateException.class, writer::init);
        Assert.assertThrows(IllegalStateException.class, () -> writer.send(message(2)));
        Assert.assertTrue(writer.shutdown().isDone());
    }

    @Test
    public void shutdownDuringInitWaitsForServerClose() {
        WriteStreamMock stream = new WriteStreamMock();
        AsyncWriter writer = writer(stream, settings().build());
        CompletableFuture<InitResult> init = writer.init();
        CompletableFuture<Void> shutdown = writer.shutdown();
        Assert.assertTrue(stream.isClosed);
        Assert.assertFalse(stream.isCanceled);
        Assert.assertFalse(shutdown.isDone());
        Assert.assertFalse(init.isDone());
        stream.close(Status.SUCCESS);
        Assert.assertTrue(shutdown.isDone());
        Assert.assertTrue(init.isCompletedExceptionally());
    }

    @Test
    public void lateAckAfterHalfCloseCanStillCompleteMessage() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        AsyncWriter writer = writer(stream, settings().build());
        writer.init();
        stream.sendInitResponse(0);
        CompletableFuture<WriteAck> pending = writer.send(message(1));
        CompletableFuture<Void> shutdown = writer.shutdown();
        Assert.assertFalse(shutdown.isDone());
        Assert.assertFalse(pending.isDone());
        Assert.assertFalse(stream.isCanceled);
        stream.sendAckResponse(1, 7);
        Assert.assertEquals(7, pending.join().getDetails().getOffset());
        Assert.assertFalse(shutdown.isDone());
        stream.close(Status.SUCCESS);
        shutdown.join();
    }

    @Test
    public void shutdownFutureCanCompleteBeforeMessageFutures() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        AsyncWriter writer = writer(stream, settings().build());
        writer.init();
        stream.sendInitResponse(0);
        CompletableFuture<WriteAck> pending = writer.send(message(1));
        AtomicBoolean pendingAtShutdownCallback = new AtomicBoolean();
        CompletableFuture<Void> observed = writer.shutdown().thenRun(() ->
                pendingAtShutdownCallback.set(!pending.isDone()));
        stream.close(Status.SUCCESS);
        observed.join();
        Assert.assertTrue(pendingAtShutdownCallback.get());
        Assert.assertTrue(pending.isCompletedExceptionally());
    }

    @Test
    public void shutdownDoesNotRunQueuedCompressionTask() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        ManualExecutor compression = new ManualExecutor();
        AsyncWriter writer = new AsyncWriterImpl(rpc(stream), settings().setCodec(Codec.GZIP).build(),
                compression, new CodecRegistry());
        writer.init();
        stream.sendInitResponse(0);
        CompletableFuture<WriteAck> pending = writer.send(message(1));
        Assert.assertEquals(1, compression.tasks.size());
        close(writer, stream);
        Assert.assertTrue(pending.isCompletedExceptionally());
        Assert.assertEquals(1, stream.messages.size());
        compression.tasks.remove(0).run();
        Assert.assertEquals(1, stream.messages.size());
    }

    @Test
    public void shutdownDoesNotAwaitRunningCompression() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        BlockingCodec codec = new BlockingCodec();
        ManualExecutor compression = new ManualExecutor();
        AsyncWriter writer = new AsyncWriterImpl(rpc(stream), settings().setCodec(codec.getId()).build(),
                compression, new CodecRegistry(Collections.singletonList(codec)));
        writer.init();
        stream.sendInitResponse(0);
        CompletableFuture<WriteAck> pending = writer.send(message(1));
        CompletableFuture<Void> encoding = CompletableFuture.runAsync(compression.tasks.remove(0));
        try {
            Assert.assertTrue(codec.entered.await(5, TimeUnit.SECONDS));
            close(writer, stream);
            Assert.assertTrue(pending.isCompletedExceptionally());
            Assert.assertFalse(encoding.isDone());
        } finally {
            codec.release.countDown();
            encoding.get(5, TimeUnit.SECONDS);
        }
        Assert.assertEquals(1, stream.messages.size());
    }

    @Test
    public void terminalMessageFailurePrecedesErrorsHandler() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        List<String> events = new ArrayList<>();
        AsyncWriter writer = writer(stream, settings().setErrorsHandler((status, error) -> events.add("status"))
                .build());
        writer.init();
        stream.sendInitResponse(0);
        writer.send(message(1)).whenComplete((ack, error) -> events.add("message"));
        stream.close(Status.of(StatusCode.OVERLOADED));
        Assert.assertEquals(Arrays.asList("message", "status"), events);
        Assert.assertTrue(writer.shutdown().isDone());
    }

    @Test
    public void transientErrorHandlerDoesNotMeanMessageFailed() throws Exception {
        WriteStreamMock first = new WriteStreamMock();
        WriteStreamMock second = new WriteStreamMock();
        List<StatusCode> events = new ArrayList<>();
        WriterSettings config = settings().setRetryConfig(status -> (number, elapsed) -> 0)
                .setErrorsHandler((status, error) -> events.add(status.getCode())).build();
        AsyncWriter writer = new AsyncWriterImpl(rpc(first, second), config, Runnable::run, new CodecRegistry());
        writer.init();
        first.sendInitResponse(0);
        CompletableFuture<WriteAck> pending = writer.send(message(1));
        first.close(Status.of(StatusCode.UNAVAILABLE));
        Assert.assertEquals(Collections.singletonList(StatusCode.UNAVAILABLE), events);
        Assert.assertFalse(pending.isDone());
        second.sendInitResponse(0);
        Assert.assertEquals(1, second.messages.get(1).getWriteRequest().getMessages(0).getSeqNo());
        second.sendAckResponse(1, 0);
        Assert.assertEquals(1, pending.join().getSeqNo());
        close(writer, second);
    }

    @Test
    public void throwingErrorsHandlerCanPreventReconnect() throws Exception {
        WriteStreamMock first = new WriteStreamMock();
        WriteStreamMock second = new WriteStreamMock();
        TopicRpc topicRpc = rpc(first, second);
        WriterSettings config = settings().setRetryConfig(status -> (number, elapsed) -> 0)
                .setErrorsHandler((status, error) -> {
                    throw new IllegalStateException("User handler failure");
                }).build();
        AsyncWriter writer = new AsyncWriterImpl(topicRpc, config, Runnable::run, new CodecRegistry());
        writer.init();
        first.sendInitResponse(0);
        CompletableFuture<WriteAck> pending = writer.send(message(1));
        first.close(Status.of(StatusCode.UNAVAILABLE));
        Assert.assertNull(second.observer);
        Assert.assertFalse(pending.isDone());
        Mockito.verify(topicRpc, Mockito.times(1)).writeSession(Mockito.anyString());
        writer.shutdown().join();
        Assert.assertTrue(pending.isCompletedExceptionally());
    }

    @Test
    public void normalServerCloseDoesNotInvokeErrorsHandler() {
        WriteStreamMock stream = new WriteStreamMock();
        AtomicBoolean called = new AtomicBoolean();
        AsyncWriter writer = writer(stream, settings().setErrorsHandler((status, error) -> called.set(true)).build());
        writer.init();
        stream.sendInitResponse(0);
        close(writer, stream);
        Assert.assertFalse(called.get());
    }

    @Test
    public void sendCanReturnAlreadyCompletedFuture() throws Exception {
        WriteStreamMock stream = new WriteStreamMock() {
            @Override
            public void sendNext(FromClient message) {
                super.sendNext(message);
                if (message.hasWriteRequest()) {
                    sendAckResponse(message.getWriteRequest().getMessages(0).getSeqNo(), 8);
                }
            }
        };
        AsyncWriter writer = writer(stream, settings().build());
        writer.init();
        stream.sendInitResponse(0);
        CompletableFuture<WriteAck> pending = writer.send(message(7));
        Assert.assertTrue(pending.isDone());
        Assert.assertEquals(8, pending.join().getDetails().getOffset());
        close(writer, stream);
    }

    @Test
    public void cumulativeAckSharesIdentityAndOffsetWithEarlierMessage() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        AsyncWriter writer = writer(stream, settings().build());
        writer.init();
        stream.sendInitResponse(0);
        CompletableFuture<WriteAck> first = writer.send(message(1));
        CompletableFuture<WriteAck> second = writer.send(message(2));
        stream.sendAckResponse(2, 101);
        Assert.assertSame(first.join(), second.join());
        Assert.assertEquals(2, first.join().getSeqNo());
        Assert.assertEquals(101, first.join().getDetails().getOffset());
        stream.sendAckResponse(2, 101);
        Assert.assertSame(first.join(), second.join());
        close(writer, stream);
    }

    @Test
    public void skippedAndStagedAcksHaveNoOffsetAndDoNotFinishTransaction() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        AsyncWriter writer = writer(stream, settings().build());
        MutableTransaction tx = new MutableTransaction();
        writer.init();
        stream.sendInitResponse(0);
        CompletableFuture<WriteAck> first = writer.send(message(1));
        sendAck(stream, YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.newBuilder().setSeqNo(1)
                .setSkipped(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.Skipped.newBuilder()).build());
        Assert.assertEquals(WriteAck.State.ALREADY_WRITTEN, first.join().getState());
        Assert.assertNull(first.join().getDetails());
        CompletableFuture<WriteAck> staged = writer.send(message(2),
                SendSettings.newBuilder().setTransaction(tx).build());
        sendAck(stream, YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.newBuilder().setSeqNo(2)
                .setWrittenInTx(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.WrittenInTx.newBuilder()).build());
        Assert.assertEquals(WriteAck.State.WRITTEN_IN_TX, staged.join().getState());
        Assert.assertNull(staged.join().getDetails());
        Assert.assertFalse(tx.getStatusFuture().isDone());
        tx.status.complete(Status.of(StatusCode.ABORTED));
        Assert.assertEquals(WriteAck.State.WRITTEN_IN_TX, staged.join().getState());
        close(writer, stream);
    }

    @Test
    public void pinnedAndUnpinnedProducerInitUseDifferentSelectors() {
        WriteStreamMock pinned = new WriteStreamMock();
        WriteStreamMock unpinned = new WriteStreamMock();
        AsyncWriter active = writer(pinned, settings().setProducerId("prefix-10").setPartitionId(10).build());
        AsyncWriter history = writer(unpinned, settings().setProducerId("prefix-10").build());
        CompletableFuture<InitResult> activeInit = active.init();
        CompletableFuture<InitResult> historyInit = history.init();
        Assert.assertEquals("prefix-10", pinned.messages.get(0).getInitRequest().getProducerId());
        Assert.assertEquals(10, pinned.messages.get(0).getInitRequest().getPartitionId());
        Assert.assertTrue(pinned.messages.get(0).getInitRequest().hasPartitionId());
        Assert.assertEquals("prefix-10", unpinned.messages.get(0).getInitRequest().getProducerId());
        Assert.assertFalse(unpinned.messages.get(0).getInitRequest().hasPartitionId());
        pinned.sendInitResponse(22);
        unpinned.sendInitResponse(33);
        Assert.assertEquals(22, activeInit.join().getSeqNo());
        Assert.assertEquals(33, historyInit.join().getSeqNo());
        close(active, pinned);
        close(history, unpinned);
        Assert.assertThrows(IllegalArgumentException.class, () -> writer(new WriteStreamMock(),
                settings().setPartitionId(10).setMessageGroupId("prefix-10").build()));
    }

    @Test
    public void transactionIdentityIsReadAtActualSendNotAdmission() throws Exception {
        WriteStreamMock stream = new WriteStreamMock();
        AsyncWriter writer = writer(stream, settings().build());
        MutableTransaction tx = new MutableTransaction();
        writer.send(message(1), SendSettings.newBuilder().setTransaction(tx).build());
        tx.id.set("new-transaction");
        writer.init();
        stream.sendInitResponse(0);
        Assert.assertEquals("new-transaction", stream.messages.get(1).getWriteRequest().getTx().getId());
        Assert.assertEquals("session", stream.messages.get(1).getWriteRequest().getTx().getSession());
        close(writer, stream);
    }

    private static WriterSettings.Builder settings() {
        return WriterSettings.newBuilder().setTopicPath("/test/multi-contract").setProducerId("prefix-0")
                .setCodec(Codec.RAW).setRetryConfig(TopicRetryConfig.NEVER);
    }

    private static Message message(long seqNo) {
        return Message.newBuilder().setData(new byte[] { 1 }).setSeqNo(seqNo).build();
    }

    private static TopicRpc rpc(WriteStreamMock first, WriteStreamMock... rest) {
        TopicRpc topicRpc = Mockito.mock(TopicRpc.class);
        Mockito.when(topicRpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(topicRpc.writeSession(Mockito.anyString())).thenReturn(first, rest);
        return topicRpc;
    }

    private static AsyncWriter writer(WriteStreamMock stream, WriterSettings settings) {
        return new AsyncWriterImpl(rpc(stream), settings, Runnable::run, new CodecRegistry());
    }

    private static void close(AsyncWriter writer, WriteStreamMock stream) {
        CompletableFuture<Void> shutdown = writer.shutdown();
        stream.close(Status.SUCCESS);
        shutdown.join();
    }

    private static void sendAck(WriteStreamMock stream, YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack) {
        stream.observer.onNext(FromServer.newBuilder().setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setWriteResponse(YdbTopic.StreamWriteMessage.WriteResponse.newBuilder().addAcks(ack)).build());
    }

    private static final class ManualExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }
    }

    private static final class BlockingCodec implements Codec {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public int getId() {
            return 10001;
        }

        @Override
        public InputStream decode(InputStream input) {
            return input;
        }

        @Override
        public OutputStream encode(OutputStream output) throws IOException {
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("Timed out waiting for test to release compression");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException(ex);
            }
            return output;
        }
    }

    private static final class MutableTransaction implements YdbTransaction {
        private final AtomicReference<String> id = new AtomicReference<>("original-transaction");
        private final CompletableFuture<Status> status = new CompletableFuture<>();

        @Override
        public String getId() {
            return id.get();
        }

        @Override
        public TxMode getTxMode() {
            return TxMode.SERIALIZABLE_RW;
        }

        @Override
        public String getSessionId() {
            return "session";
        }

        @Override
        public CompletableFuture<Status> getStatusFuture() {
            return status;
        }
    }
}
