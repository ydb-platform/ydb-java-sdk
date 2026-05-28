package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;

/**
 * Regression test for the flow-control stall that happens when a partition session
 * is stopped while batches are still queued in {@link ReadPartitionSession#readingQueue}.
 *
 * The scenario:
 * <ol>
 *   <li>ReadResponse R1 arrives and its batch is handed to the user handler (whose future is pending).</li>
 *   <li>While the user is still processing R1, ReadResponse R2 arrives -- its batch is enqueued
 *       behind the in-flight one (because {@code isReadingNow == true}).</li>
 *   <li>Server gracefully stops the partition; the user confirms and the partition session is removed,
 *       {@link ReadPartitionSession#stop()} is invoked.</li>
 *   <li>If {@code stop()} fails to complete the queued batch futures, the per-response
 *       {@code CompletableFuture.allOf(batchReadFutures)} in {@link ReadSession#onReadResponse}
 *       never fires, which means {@link ReadSession#sendReadRequest()} is never called,
 *       starving the reader of any further data.</li>
 * </ol>
 */
public class ReadPartitionSessionFlowControlTest {

    private static final long MAX_MEMORY = 10_000L;
    private static final long PARTITION_SESSION_ID = 1L;
    private static final long PARTITION_ID = 42L;
    private static final long RESPONSE1_BYTES = 300L;
    private static final long RESPONSE2_BYTES = 400L;

    @Test(timeout = 10_000)
    public void readerKeepsRequestingDataAfterGracefulStopWithPendingBatches() {
        StreamMock stream = new StreamMock();
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.readSession(Mockito.any())).thenReturn(stream);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));

        ReaderSettings settings = ReaderSettings.newBuilder()
                .setMaxMemoryUsageBytes(MAX_MEMORY)
                .setConsumerName("test-consumer")
                .addTopic(TopicReadSettings.newBuilder().setPath("/test/topic").build())
                .setDecompressionExecutor(Runnable::run)
                .build();

        TestReader reader = new TestReader(rpc, settings);
        reader.init();

        // Init request must be the first message sent
        Assert.assertTrue("init request expected", stream.lastSent().hasInitRequest());

        // Step 1: Server sends InitResponse -- SDK should reply with the initial ReadRequest
        stream.deliver(initResponse("sess-1"));
        Assert.assertEquals(MAX_MEMORY, stream.lastSent().getReadRequest().getBytesSize());

        // Step 2: Start partition session -- reader auto-confirms
        stream.deliver(startPartition(PARTITION_SESSION_ID, PARTITION_ID, 0L));
        Assert.assertTrue("start partition response expected",
                stream.lastSent().hasStartPartitionSessionResponse());

        // Step 3: ReadResponse #1 -- batch handed to user, future stays pending
        stream.deliver(readResponse(PARTITION_SESSION_ID, RESPONSE1_BYTES, 0L));
        Assert.assertEquals("handler must be invoked exactly once for R1",
                1, reader.pendingDataFutures.size());

        // Step 4: ReadResponse #2 -- arrives while R1 is in flight, so its batch
        // is enqueued in readingQueue but never delivered to the user
        // (isReadingNow == true).
        stream.deliver(readResponse(PARTITION_SESSION_ID, RESPONSE2_BYTES, 1L));
        Assert.assertEquals("handler must NOT be invoked again while R1 is in flight",
                1, reader.pendingDataFutures.size());

        // Bytes the SDK had asked for *before* the partition stop -- only the initial
        // maxMemoryUsageBytes ReadRequest emitted after InitResponse should be in here,
        // because no data callback has completed yet.
        long readRequestBytesBefore = stream.sumReadRequestBytes();

        // Step 5: Graceful stop -- reader auto-confirms. This triggers
        // ReadPartitionSession.stop() while the batch from R2 is still queued.
        stream.deliver(stopPartition(PARTITION_SESSION_ID, true, 1L));
        Assert.assertTrue("stop partition response expected",
                stream.hasSentStopPartitionSessionResponse());

        // Step 6: Unblock R1's handler future. This completes R1's allOf and
        // triggers a ReadRequest replenishing R1's bytes. R2's allOf, however,
        // should also complete because the queued batch must be drained by stop().
        CompletableFuture<Void> r1Future = reader.pendingDataFutures.poll();
        Assert.assertNotNull("R1 handler future is missing", r1Future);
        r1Future.complete(null);

        long readRequestBytesAfter = stream.sumReadRequestBytes();
        long replenishedBytes = readRequestBytesAfter - readRequestBytesBefore;

        // The SDK must have replenished the full server-side budget consumed by R1+R2.
        // If R2's batch is orphaned in readingQueue, only R1's bytes get replenished
        // and the reader silently stops requesting data.
        Assert.assertEquals("ReadRequest bytes after partition stop must include both responses",
                RESPONSE1_BYTES + RESPONSE2_BYTES, replenishedBytes);
    }

    // ---------------------------------------------------------------------
    // Test fixtures
    // ---------------------------------------------------------------------

    private static FromServer initResponse(String sessionId) {
        return FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setInitResponse(YdbTopic.StreamReadMessage.InitResponse.newBuilder()
                        .setSessionId(sessionId).build())
                .build();
    }

    private static FromServer startPartition(long psid, long pid, long committed) {
        return FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setStartPartitionSessionRequest(YdbTopic.StreamReadMessage.StartPartitionSessionRequest.newBuilder()
                        .setPartitionSession(YdbTopic.StreamReadMessage.PartitionSession.newBuilder()
                                .setPartitionSessionId(psid)
                                .setPartitionId(pid)
                                .setPath("/test/topic")
                                .build())
                        .setCommittedOffset(committed)
                        .setPartitionOffsets(YdbTopic.OffsetsRange.newBuilder()
                                .setStart(committed)
                                .setEnd(committed + 1_000_000L)
                                .build())
                        .build())
                .build();
    }

    private static FromServer readResponse(long psid, long bytes, long firstOffset) {
        // codec=RAW (1) so that the batch is "ready" immediately and the message goes
        // straight from addBatches to sendDataToReadersIfNeeded -> handleDataReceivedEvent
        // without any async decoder steps.
        YdbTopic.StreamReadMessage.ReadResponse.MessageData message =
                YdbTopic.StreamReadMessage.ReadResponse.MessageData.newBuilder()
                        .setOffset(firstOffset)
                        .setSeqNo(firstOffset + 1)
                        .setData(ByteString.copyFromUtf8("payload-" + firstOffset))
                        .build();

        YdbTopic.StreamReadMessage.ReadResponse.Batch batch =
                YdbTopic.StreamReadMessage.ReadResponse.Batch.newBuilder()
                        .setProducerId("test-producer")
                        .setCodec(1) // Codec.RAW
                        .addMessageData(message)
                        .build();

        YdbTopic.StreamReadMessage.ReadResponse.PartitionData partitionData =
                YdbTopic.StreamReadMessage.ReadResponse.PartitionData.newBuilder()
                        .setPartitionSessionId(psid)
                        .addBatches(batch)
                        .build();

        return FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setReadResponse(YdbTopic.StreamReadMessage.ReadResponse.newBuilder()
                        .setBytesSize(bytes)
                        .addPartitionData(partitionData)
                        .build())
                .build();
    }

    private static FromServer stopPartition(long psid, boolean graceful, long committed) {
        return FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setStopPartitionSessionRequest(YdbTopic.StreamReadMessage.StopPartitionSessionRequest.newBuilder()
                        .setPartitionSessionId(psid)
                        .setGraceful(graceful)
                        .setCommittedOffset(committed)
                        .build())
                .build();
    }

    /**
     * Minimal {@link ReaderImpl} that runs the handlers synchronously and exposes the
     * {@link DataReceivedEvent} future so the test can finely control when the user's
     * processing is considered complete.
     */
    private static final class TestReader extends ReaderImpl {
        final LinkedBlockingQueue<CompletableFuture<Void>> pendingDataFutures = new LinkedBlockingQueue<>();

        TestReader(TopicRpc topicRpc, ReaderSettings settings) {
            super(topicRpc, settings, new CodecRegistry());
        }

        void init() {
            initImpl();
        }

        @Override
        protected CompletableFuture<Void> handleDataReceivedEvent(DataReceivedEvent event) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            pendingDataFutures.add(future);
            return future;
        }

        @Override
        protected void handleSessionStarted(String sessionId) {
            // nothing
        }

        @Override
        protected void handleCommitResponse(long committedOffset, PartitionSession partition) {
            // nothing
        }

        @Override
        protected void handleStartPartitionSessionRequest(StartPartitionSessionEvent event) {
            event.confirm();
        }

        @Override
        protected void handleStopPartitionSession(StopPartitionSessionEvent event) {
            event.confirm();
        }

        @Override
        protected void handleClosePartitionSession(PartitionSession partition) {
            // nothing
        }
    }

    /**
     * Stub for the bidirectional gRPC stream that records every {@link FromClient} sent
     * and replays {@link FromServer} messages synchronously into the captured observer.
     */
    private static final class StreamMock implements GrpcReadWriteStream<FromServer, FromClient> {
        private final CompletableFuture<Status> streamFuture = new CompletableFuture<>();
        private final List<FromClient> sent = new ArrayList<>();
        private GrpcReadStream.Observer<FromServer> observer;

        void deliver(FromServer message) {
            Assert.assertNotNull("observer is not set yet", observer);
            observer.onNext(message);
        }

        FromClient lastSent() {
            Assert.assertFalse("no messages were sent", sent.isEmpty());
            return sent.get(sent.size() - 1);
        }

        long sumReadRequestBytes() {
            long total = 0L;
            for (FromClient msg : sent) {
                if (msg.hasReadRequest()) {
                    total += msg.getReadRequest().getBytesSize();
                }
            }
            return total;
        }

        boolean hasSentStopPartitionSessionResponse() {
            for (FromClient msg : sent) {
                if (msg.hasStopPartitionSessionResponse()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String authToken() {
            return "token";
        }

        @Override
        public void sendNext(FromClient message) {
            sent.add(message);
        }

        @Override
        public CompletableFuture<Status> start(GrpcReadStream.Observer<FromServer> observer) {
            this.observer = observer;
            return streamFuture;
        }

        @Override
        public void close() {
            streamFuture.complete(Status.SUCCESS);
        }

        @Override
        public void cancel() {
            streamFuture.complete(Status.SUCCESS);
        }
    }

}
