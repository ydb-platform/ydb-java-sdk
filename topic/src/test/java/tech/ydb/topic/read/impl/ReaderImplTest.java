package tech.ydb.topic.read.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.TopicRetryConfig;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;
import tech.ydb.topic.utils.HideLoggers;
import tech.ydb.topic.utils.HideLoggersRule;

public class ReaderImplTest {
    private static final CodecRegistry REGISTRY = new CodecRegistry();

    private static final String TOPIC1 = "/test/topic";
    private static final String TOPIC2 = "/test/topic2";

    @Rule
    public final HideLoggersRule hideLogger = new HideLoggersRule();

    private static TopicRpc mockRpc(ReadStreamMock first, ReadStreamMock... rest) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.readSession(Mockito.any(String.class))).thenReturn(first, rest);
        Mockito.when(rpc.updateOffsetsInTransaction(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Status.SUCCESS));
        return rpc;
    }

    private static void assertIllegalArgument(String msg, ThrowingRunnable runnable) {
        IllegalArgumentException ex = Assert.assertThrows("Must be thrown IllegalArgumentException",
                IllegalArgumentException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    private static ReaderImpl startReader(ReaderImpl.Handler handler, ReadStreamMock mock) {
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TOPIC1).build())
                .setConsumerName("consumer")
                .setRetryConfig(TopicRetryConfig.NEVER)
                .setMaxMemoryUsageBytes(1000)
                .build();
        ReadConfig config = new ReadConfig(REGISTRY, Runnable::run, Runnable::run, settings);
        ReaderImpl reader = new ReaderImpl(mockRpc(mock), "test-reader", settings, config, handler);
        reader.start();
        mock.responseInit("read-session-1");
        mock.assertSentMessagesCount(2);
        mock.assertLastMessage().isReadRequest(1000);
        return reader;
    }

    @Test
    @HideLoggers({ ReadSession.class })
    public void duplicateStartPartitionRequestTest() {
        ReadStreamMock mock = new ReadStreamMock();
        ReaderImpl.Handler handler = Mockito.mock(ReaderImpl.Handler.class);
        ReaderImpl reader = startReader(handler, mock);

        mock.responseStartPartition(TOPIC1, 123, 0, 1);

        ArgumentCaptor<StartPartitionSessionEvent> start = ArgumentCaptor.forClass(StartPartitionSessionEvent.class);
        Mockito.verify(handler).handleStartPartitionSessionRequest(start.capture());

        Assert.assertEquals(new PartitionSession(1, 123, TOPIC1), start.getValue().getPartitionSession());

        // Once recived, a second start partition request is a protocol error.
        mock.responseStartPartition(TOPIC1, 123, 0, 1);

        // partiton is not started
        Mockito.verify(handler, Mockito.never()).handleStopPartitionSession(Mockito.any());

        ArgumentCaptor<Status> closed = ArgumentCaptor.forClass(Status.class);
        Mockito.verify(handler).handleReaderClosed(closed.capture());
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, closed.getValue().getCode());
        mock.assertSentMessagesCount(2);
        mock.assertIsClosed();
        reader.close();
        mock.assertIsClosed();
    }

    @Test
    @HideLoggers({ ReadSession.class })
    public void duplicateStopPartitionRequestTest() {
        ReadStreamMock mock = new ReadStreamMock();
        ReaderImpl.Handler handler = Mockito.mock(ReaderImpl.Handler.class);
        ReaderImpl reader = startReader(handler, mock);

        mock.responseStartPartition(TOPIC1, 123, 0);
        ArgumentCaptor<StartPartitionSessionEvent> start = ArgumentCaptor.forClass(StartPartitionSessionEvent.class);
        Mockito.verify(handler).handleStartPartitionSessionRequest(start.capture());
        start.getValue().confirm();

        // Both requests arrive before the application confirms the stop.
        mock.responseStopPartition(1, true);
        mock.responseStopPartition(1, true);
        ArgumentCaptor<StopPartitionSessionEvent> stop = ArgumentCaptor.forClass(StopPartitionSessionEvent.class);
        Mockito.verify(handler, Mockito.times(2)).handleStopPartitionSession(stop.capture());
        for (StopPartitionSessionEvent event : stop.getAllValues()) {
            Assert.assertSame(start.getValue().getPartitionSession(), event.getPartitionSession());
        }
        mock.assertSentMessagesCount(3);

        stop.getAllValues().get(0).confirm();
        mock.assertSentMessagesCount(4);
        mock.assertLastMessage().isStopPartition(1);
        stop.getAllValues().get(1).confirm();
        stop.getAllValues().get(0).confirm();
        mock.assertSentMessagesCount(4);
        mock.assertIsActive();

        // Once confirmed, a graceful stop for the removed session is a protocol error.
        mock.responseStopPartition(1, true);
        Mockito.verify(handler, Mockito.times(2)).handleStopPartitionSession(Mockito.any());
        ArgumentCaptor<Status> closed = ArgumentCaptor.forClass(Status.class);
        Mockito.verify(handler).handleReaderClosed(closed.capture());
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, closed.getValue().getCode());
        mock.assertSentMessagesCount(4);
        mock.assertIsClosed();
        reader.close();
        mock.assertIsClosed();
    }

    @Test
    public void duplicateForcedStopPartitionRequestTest() {
        ReadStreamMock mock = new ReadStreamMock();
        ReaderImpl.Handler handler = Mockito.mock(ReaderImpl.Handler.class);
        ReaderImpl reader = startReader(handler, mock);

        mock.responseStartPartition(TOPIC1, 123, 0);
        ArgumentCaptor<StartPartitionSessionEvent> start = ArgumentCaptor.forClass(StartPartitionSessionEvent.class);
        Mockito.verify(handler).handleStartPartitionSessionRequest(start.capture());
        start.getValue().confirm();

        mock.responseStopPartition(1, false);
        mock.responseStopPartition(1, false);
        Mockito.verify(handler).handleClosePartitionSession(start.getValue().getPartitionSession());
        Mockito.verify(handler, Mockito.never()).handleStopPartitionSession(Mockito.any());
        Mockito.verify(handler, Mockito.never()).handleReaderClosed(Mockito.any());
        mock.assertSentMessagesCount(3); // forced stops do not require acknowledgement
        mock.assertIsActive();

        reader.close();
        mock.assertIsClosed();
    }

    @Test
    public void updateTokenTest() {
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TOPIC1)
                        .setMaxLag(Duration.ofDays(1))
                        .setReadFrom(Instant.EPOCH.plusSeconds(1000000))
                        .build())
                .addTopic(TopicReadSettings.newBuilder().setPath(TOPIC2).build())
                .setRetryConfig(TopicRetryConfig.NEVER)
                .setMaxMemoryUsageBytes(1000)
                .withoutConsumer()
                .build();

        ReadStreamMock mock = new ReadStreamMock();
        ReadConfig config = new ReadConfig(REGISTRY, Runnable::run, Runnable::run, settings);
        ReaderImpl.Handler handler = Mockito.mock(ReaderImpl.Handler.class);

        ReaderImpl reader = new ReaderImpl(mockRpc(mock), "test-reader", settings, config, handler);

        reader.start();
        mock.assertSentMessagesCount(1);
        mock.assertLastMessage().isInitRequest(null, TOPIC1, TOPIC2);

        mock.updateTokenValue("new-token-value");

        mock.assertSentMessagesCount(1);
        mock.responseInit("read-session-1");

        mock.assertSentMessagesCount(3); // update token + read request
        mock.assertPreLastMessage().isUpdateToken("new-token-value");
        mock.assertLastMessage().isReadRequest(1000);

        mock.responseUpdateToken();

        reader.close();
        mock.assertIsClosed();
    }

    @Test
    public void updateOffsetsInTxValidationTest() {
        ReadStreamMock mock = new ReadStreamMock();
        ReaderImpl.Handler handler = Mockito.mock(ReaderImpl.Handler.class);
        ReaderImpl reader = startReader(handler, mock);

        UpdateOffsetsInTransactionSettings updateSettings = UpdateOffsetsInTransactionSettings.newBuilder()
                .withTraceId("test-trace").build();

        TxMock finished = new TxMock(CompletableFuture.completedFuture(Status.SUCCESS));
        assertIllegalArgument("Transaction is not active. "
                + "Can only read topic messages in already running transactions from other services",
                () -> reader.updateOffsetsInTransaction(finished, Collections.emptyMap(), updateSettings)
        );

        CompletableFuture<Status> txStatus = new CompletableFuture<>();
        TxMock active = new TxMock(txStatus);
        assertIllegalArgument("Empty topic list to update in transaction",
                () -> reader.updateOffsetsInTransaction(active, Collections.emptyMap(), updateSettings)
        );

        assertIllegalArgument("Empty offsets range to update in transaction",
                () -> reader.updateOffsetsInTransaction(
                        active, Collections.singletonMap(TOPIC1, new ArrayList<PartitionOffsets>()), updateSettings
                )
        );

        List<PartitionOffsets> offsets = Arrays.asList(
                new PartitionOffsets(new PartitionSession(1, 1, TOPIC1), Arrays.asList(OffsetsRange.of(0, 10))),
                new PartitionOffsets(new PartitionSession(2, 2, TOPIC1),
                        Arrays.asList(OffsetsRange.of(0, 1), OffsetsRange.of(2, 3)))
        );
        reader.updateOffsetsInTransaction(active, Collections.singletonMap(TOPIC1, offsets), updateSettings);


        txStatus.complete(Status.SUCCESS);
        mock.assertIsActive();
        reader.close();

        mock.assertIsClosed();
    }

    @Test
    public void updateOffsetsInTxFailTest() {
        ReadStreamMock mock = new ReadStreamMock();
        ReaderImpl.Handler handler = Mockito.mock(ReaderImpl.Handler.class);
        ReaderImpl reader = startReader(handler, mock);

        UpdateOffsetsInTransactionSettings updateSettings = UpdateOffsetsInTransactionSettings.newBuilder().build();

        CompletableFuture<Status> txStatus = new CompletableFuture<>();
        TxMock active = new TxMock(txStatus);
        List<PartitionOffsets> offsets = Arrays.asList(
                new PartitionOffsets(new PartitionSession(1, 1, TOPIC1), Arrays.asList(OffsetsRange.of(0, 10))),
                new PartitionOffsets(new PartitionSession(2, 2, TOPIC1),
                        Arrays.asList(OffsetsRange.of(0, 1), OffsetsRange.of(2, 3)))
        );
        reader.updateOffsetsInTransaction(active, Collections.singletonMap(TOPIC1, offsets), updateSettings);

        txStatus.complete(Status.of(StatusCode.ABORTED));
        mock.assertIsClosed();
        reader.close();
        mock.assertIsClosed();
    }

    @Test
    public void updateOffsetsInTxErrorTest() {
        ReadStreamMock mock = new ReadStreamMock();
        ReaderImpl.Handler handler = Mockito.mock(ReaderImpl.Handler.class);
        ReaderImpl reader = startReader(handler, mock);

        UpdateOffsetsInTransactionSettings updateSettings = UpdateOffsetsInTransactionSettings.newBuilder().build();

        CompletableFuture<Status> txStatus = new CompletableFuture<>();
        TxMock active = new TxMock(txStatus);
        List<PartitionOffsets> offsets = Arrays.asList(
                new PartitionOffsets(new PartitionSession(1, 1, TOPIC1), Arrays.asList(OffsetsRange.of(0, 10))),
                new PartitionOffsets(new PartitionSession(2, 2, TOPIC1),
                        Arrays.asList(OffsetsRange.of(0, 1), OffsetsRange.of(2, 3)))
        );
        reader.updateOffsetsInTransaction(active, Collections.singletonMap(TOPIC1, offsets), updateSettings);

        txStatus.completeExceptionally(new RuntimeException("tx problem"));
        mock.assertIsClosed();
        reader.close();
        mock.assertIsClosed();
    }

    private class TxMock implements YdbTransaction {
        private final CompletableFuture<Status> status;

        public TxMock(CompletableFuture<Status> status) {
            this.status = status;
        }

        @Override
        public boolean isActive() {
            return !status.isDone();
        }

        @Override
        public String getId() {
            return "tx-id";
        }

        @Override
        public TxMode getTxMode() {
            return TxMode.NONE;
        }

        @Override
        public String getSessionId() {
            return "session-tx-id";
        }

        @Override
        public CompletableFuture<Status> getStatusFuture() {
            return status;
        }
    }
}
