package tech.ydb.topic.write;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.settings.MultiWriterSettings;
import tech.ydb.topic.write.MultiWriteResult.ConfirmationSource;

public class MultiWriterModelsTest {
    @Test
    public void messageOwnsPayloadAndMetadataSnapshots() {
        byte[] data = { 1, 2 };
        byte[] metadata = { 3, 4 };
        List<MetadataItem> items = new ArrayList<>();
        items.add(new MetadataItem("tag", metadata));
        Instant timestamp = Instant.parse("2026-01-01T12:00:00Z");
        MultiMessage message = MultiMessage.newBuilder().setData(data).setKey("ключ")
                .setMetadataItems(items).setCreateTimestamp(timestamp).build();
        data[0] = 9;
        metadata[0] = 9;
        items.clear();
        Assert.assertArrayEquals(new byte[] { 1, 2 }, message.getData());
        Assert.assertArrayEquals(new byte[] { 3, 4 }, message.getMetadataItems().get(0).getValue());
        Assert.assertEquals("ключ", message.getKey());
        Assert.assertEquals(timestamp, message.getCreateTimestamp());
        message.getData()[0] = 8;
        message.getMetadataItems().get(0).getValue()[0] = 8;
        Assert.assertArrayEquals(new byte[] { 1, 2 }, message.getData());
        Assert.assertArrayEquals(new byte[] { 3, 4 }, message.getMetadataItems().get(0).getValue());
        Assert.assertThrows(UnsupportedOperationException.class, () -> message.getMetadataItems().clear());
    }

    @Test
    public void builderReuseDoesNotMutateEarlierMessage() {
        MultiMessage.Builder builder = MultiMessage.newBuilder().setData(new byte[] { 1 })
                .addMetadataItem(new MetadataItem("first", new byte[] { 2 }));
        MultiMessage first = builder.build();
        builder.setData(new byte[] { 3 }).addMetadataItem(new MetadataItem("second", new byte[] { 4 }));
        MultiMessage second = builder.build();
        Assert.assertArrayEquals(new byte[] { 1 }, first.getData());
        Assert.assertEquals(1, first.getMetadataItems().size());
        Assert.assertArrayEquals(new byte[] { 3 }, second.getData());
        Assert.assertEquals(2, second.getMetadataItems().size());
        Assert.assertNotNull(first.getCreateTimestamp());
        Assert.assertNull(first.getSeqNo());
        Assert.assertEquals("", first.getKey());
    }

    @Test
    public void rejectsAmbiguousRouteInvalidSequenceAndReservedMetadata() {
        Assert.assertThrows(IllegalArgumentException.class, () -> MultiMessage.newBuilder().setData(new byte[0])
                .setKey("key").setPartitionId(1).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> MultiMessage.newBuilder().setData(new byte[0])
                .setPartitionId(-1).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> MultiMessage.newBuilder().setData(new byte[0])
                .setSeqNo(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> MultiMessage.newBuilder().setData(new byte[0])
                .addMetadataItem(new MetadataItem("__partition_key", new byte[] { 1 })).build());
        Assert.assertThrows(NullPointerException.class, () -> MultiMessage.newBuilder().build());
        Assert.assertThrows(NullPointerException.class, () -> MultiMessage.of(new byte[0], null));
        Assert.assertThrows(NullPointerException.class, () -> MultiMessage.newBuilder().setData(new byte[0])
                .setMetadataItems(Collections.singletonList(new MetadataItem("key", null))).build());
        Assert.assertEquals(Long.valueOf(0), MultiMessage.newBuilder().setData(new byte[0])
                .setPartitionId(0).setSeqNo(1).build().getPartitionId());
    }

    @Test
    public void historyAndStagingResultsPreserveUnknownPhysicalLocation() {
        MultiWriteResult history = new MultiWriteResult(42, WriteAck.State.WRITTEN,
                ConfirmationSource.PRODUCER_HISTORY, null, null);
        Assert.assertEquals(42, history.getSeqNo());
        Assert.assertEquals(ConfirmationSource.PRODUCER_HISTORY, history.getSource());
        Assert.assertNull(history.getPartitionId());
        Assert.assertNull(history.getOffset());
        MultiWriteResult staging = new MultiWriteResult(43, WriteAck.State.WRITTEN_IN_TX,
                ConfirmationSource.ACK, 9L, null);
        Assert.assertEquals(WriteAck.State.WRITTEN_IN_TX, staging.getState());
        Assert.assertNull(staging.getOffset());
        MultiWriteResult ack = new MultiWriteResult(44, WriteAck.State.WRITTEN, ConfirmationSource.ACK, 9L, 12L);
        Assert.assertEquals(Long.valueOf(12), ack.getOffset());
    }

    @Test
    public void resultRejectsInventedOffsetOrTransactionalHistorySuccess() {
        Assert.assertThrows(IllegalArgumentException.class, () -> new MultiWriteResult(1,
                WriteAck.State.WRITTEN_IN_TX, ConfirmationSource.PRODUCER_HISTORY, null, null));
        Assert.assertThrows(IllegalArgumentException.class, () -> new MultiWriteResult(1,
                WriteAck.State.WRITTEN, ConfirmationSource.PRODUCER_HISTORY, 1L, 1L));
        Assert.assertThrows(IllegalArgumentException.class, () -> new MultiWriteResult(1,
                WriteAck.State.ALREADY_WRITTEN, ConfirmationSource.ACK, 1L, 1L));
        Assert.assertThrows(IllegalArgumentException.class, () -> new MultiWriteResult(1,
                WriteAck.State.WRITTEN, ConfirmationSource.ACK, null, 1L));
    }

    @Test
    public void settingsDefaultsAndExactBufferBoundary() {
        MultiWriterSettings config = settings().build();
        Assert.assertEquals(Codec.RAW, config.getCodec());
        Assert.assertEquals(MultiWriterSettings.RoutingMode.AUTO, config.getRoutingMode());
        Assert.assertEquals(64L * 1024 * 1024, config.getBufferBytes());
        Assert.assertEquals(10000, config.getBufferMessages());
        Assert.assertEquals(Duration.ZERO, config.getDeliveryTimeout());
        Assert.assertEquals(Duration.ofSeconds(30), config.getInitTimeout());
        Assert.assertFalse(config.isDirectWrite());
        Assert.assertEquals(Integer.MAX_VALUE - 1L,
                settings().setWriterBufferBytes(Integer.MAX_VALUE - 1L).build().getWriterBufferBytes());
        Assert.assertThrows(IllegalArgumentException.class,
                () -> settings().setWriterBufferBytes(Integer.MAX_VALUE).build());
    }

    @Test
    public void settingsRejectImpossibleCapacityAndUnboundedDeadlines() {
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setBufferBytes(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setBufferMessages(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setMaxMessageBytes(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setBufferBytes(1).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setWriterBufferBytes(1).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setMaxWriters(1).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setMaxConcurrentInits(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setIoThreads(0).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setInitTimeout(Duration.ZERO).build());
        Assert.assertThrows(IllegalArgumentException.class,
                () -> settings().setDeliveryTimeout(Duration.ofNanos(-1)).build());
        Assert.assertThrows(IllegalArgumentException.class,
                () -> settings().setRecoveryTimeout(Duration.ofSeconds(Long.MAX_VALUE)).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setCodec(0).build());
    }

    @Test
    public void producerLengthIsMeasuredAsUtf8Bytes() {
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            prefix.append('я');
        }
        Assert.assertEquals(prefix.toString(), settings().setProducerPrefix(prefix.toString()).build()
                .getProducerPrefix());
        prefix.append('я');
        Assert.assertThrows(IllegalArgumentException.class,
                () -> settings().setProducerPrefix(prefix.toString()).build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setProducerPrefix(" ").build());
        Assert.assertThrows(IllegalArgumentException.class, () -> settings().setTopicPath(null).build());
    }

    @Test
    public void syncAdapterReturnsBeforeAckAndDelegatesBarriers() throws Exception {
        AsyncMultiWriter async = Mockito.mock(AsyncMultiWriter.class);
        MultiMessage message = MultiMessage.of(new byte[] { 1 }, "key");
        CompletableFuture<MultiWriteResult> ack = new CompletableFuture<>();
        Mockito.when(async.send(message)).thenReturn(ack);
        Mockito.when(async.init()).thenReturn(CompletableFuture.completedFuture(new InitResult(41)));
        Mockito.when(async.flush()).thenReturn(CompletableFuture.completedFuture(null));
        Mockito.when(async.shutdown()).thenReturn(CompletableFuture.completedFuture(null));
        SyncMultiWriter sync = new SyncMultiWriter(async);
        Assert.assertEquals(41, sync.initAndWait().getSeqNo());
        sync.send(message);
        Assert.assertFalse(ack.isDone());
        sync.flush();
        sync.shutdown();
        Mockito.verify(async).send(message);
        Mockito.verify(async).flush();
        Mockito.verify(async).shutdown();
    }

    @Test
    public void syncAdapterPreservesAdmissionFailureAndShutdownWaitTimeout() throws Exception {
        AsyncMultiWriter async = Mockito.mock(AsyncMultiWriter.class);
        MultiMessage message = MultiMessage.of(new byte[] { 1 }, "key");
        Mockito.when(async.send(message)).thenThrow(new QueueOverflowException("full"));
        CompletableFuture<Void> stopped = new CompletableFuture<>();
        Mockito.when(async.shutdown()).thenReturn(stopped);
        SyncMultiWriter sync = new SyncMultiWriter(async);
        Assert.assertThrows(QueueOverflowException.class, () -> sync.send(message));
        Assert.assertThrows(TimeoutException.class, () -> sync.shutdown(0, TimeUnit.NANOSECONDS));
        Assert.assertFalse(stopped.isCancelled());
        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("delivery failed"));
        Mockito.when(async.flush()).thenReturn(failed);
        Assert.assertThrows(CompletionException.class, sync::flush);
    }

    @Test
    public void additiveClientFactoriesDoNotRequireExistingImplementationsToAddMethods() throws Exception {
        Assert.assertTrue(TopicClient.class.getMethod("createAsyncMultiWriter", MultiWriterSettings.class).isDefault());
        Assert.assertTrue(TopicClient.class.getMethod("createSyncMultiWriter", MultiWriterSettings.class).isDefault());
        Assert.assertTrue(TopicClient.class.getMethod("createTransactionalMultiWriter", MultiWriterSettings.class,
                YdbTransaction.class).isDefault());
    }

    private static MultiWriterSettings.Builder settings() {
        return MultiWriterSettings.newBuilder().setTopicPath("/test/topic").setProducerPrefix("owner");
    }
}
