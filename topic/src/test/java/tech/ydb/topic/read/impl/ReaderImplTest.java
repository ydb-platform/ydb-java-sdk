package tech.ydb.topic.read.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.OffsetsRange;
import tech.ydb.topic.read.PartitionOffsets;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;
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

    private static TestImpl startReader(ReadStreamMock mock) {
        return startReader(mock, Runnable::run, Runnable::run);
    }

    private static TestImpl startReader(ReadStreamMock mock, Executor manager) {
        return startReader(mock, manager, Runnable::run);
    }

    private static TestImpl startReader(ReadStreamMock mock, Executor manager, Executor processor) {
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TOPIC1).build())
                .setConsumerName("consumer")
                .setRetryConfig(TopicRetryConfig.NEVER)
                .setMaxMemoryUsageBytes(1000)
                .build();
        ReadConfig config = new ReadConfig(REGISTRY, manager, processor, Runnable::run, settings);
        TestImpl reader = new TestImpl(mockRpc(mock), "test-reader", settings, config);
        reader.start();
        mock.responseInit("read-session-1");
        mock.assertSentMessagesCount(2);
        mock.assertLastMessage().isReadRequest(1000);
        Assert.assertEquals(1, reader.starts.size());
        return reader;
    }

    @Test
    public void partitionStoppedBeforeStartEventTest() {
        ReadStreamMock mock = new ReadStreamMock();
        TestExecutor manager = new TestExecutor();

        TestImpl reader = startReader(mock, manager);

        PartitionSession ps1 = new PartitionSession(1, 123, TOPIC1);

        mock.responseStartPartition(ps1, 0);

        reader.verifyHandler(Mockito.never()).onPartitionStarted(Mockito.any());
        manager.assertSize(1); // on partition started

        mock.responseStopPartition(ps1.getId(), true); // graceful stop
        mock.assertSentMessagesCount(3);
        mock.assertLastMessage().isStopPartition(ps1.getId()); // partition is autoclosed

        reader.verifyHandler(Mockito.never()).onPartitionStarted(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionStopped(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionClosed(Mockito.any());

        manager.assertSize(1).executeNext(); // on partition started

        reader.verifyHandler(Mockito.never()).onPartitionStarted(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionStopped(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionClosed(Mockito.any());

        PartitionSession ps2 = new PartitionSession(1, 123, TOPIC1);

        mock.responseStartPartition(ps2, 0);

        reader.verifyHandler(Mockito.never()).onPartitionStarted(Mockito.any());
        manager.assertSize(1); // on partition started

        mock.responseStopPartition(ps2.getId(), false); // non graceful stop
        mock.assertSentMessagesCount(3); // non graceful stop doesn't need confirmation

        reader.verifyHandler(Mockito.never()).onPartitionStarted(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionStopped(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionClosed(Mockito.any());

        manager.assertSize(1).executeNext(); // on partition started

        reader.verifyHandler(Mockito.never()).onPartitionStarted(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionStopped(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionClosed(Mockito.any());

        reader.close();
        reader.assertReaderIsStopped(Status.SUCCESS);
        mock.assertIsClosed();
    }

    @Test
    public void partitionStoppedBeforeConfirmTest() {
        ReadStreamMock mock = new ReadStreamMock();

        TestImpl reader = startReader(mock);

        PartitionSession ps1 = new PartitionSession(1, 123, TOPIC1);
        mock.responseStartPartition(ps1, 0);

        ArgumentCaptor<StartPartitionSessionEvent> started = ArgumentCaptor.forClass(StartPartitionSessionEvent.class);
        ArgumentCaptor<PartitionSessionClosedEvent> closed = ArgumentCaptor.forClass(PartitionSessionClosedEvent.class);

        reader.verifyHandler(Mockito.times(1)).onPartitionStarted(started.capture());
        Assert.assertEquals(ps1, started.getValue().getPartitionSession());

        mock.responseStopPartition(ps1.getId(), true); // graceful stop
        mock.assertSentMessagesCount(3);
        mock.assertLastMessage().isStopPartition(ps1.getId()); // partition is autoclosed

        reader.verifyHandler(Mockito.times(1)).onPartitionStarted(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionStopped(Mockito.any());
        reader.verifyHandler(Mockito.times(1)).onPartitionClosed(closed.capture());

        Assert.assertEquals(started.getValue().getPartitionSession(), closed.getValue().getPartitionSession());
        started.getValue().confirm(); // has no effect

        reader.verifyHandler(Mockito.times(1)).onPartitionStarted(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionStopped(Mockito.any());
        reader.verifyHandler(Mockito.times(1)).onPartitionClosed(Mockito.any());

        PartitionSession ps2 = new PartitionSession(2, 123, TOPIC1);

        mock.responseStartPartition(ps2, 0);

        reader.verifyHandler(Mockito.times(2)).onPartitionStarted(started.capture());
        Assert.assertEquals(ps2, started.getValue().getPartitionSession());

        mock.responseStopPartition(ps2.getId(), false); // non graceful stop
        mock.assertSentMessagesCount(3); // non graceful stop doesn't need confirmation

        reader.verifyHandler(Mockito.times(2)).onPartitionStarted(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionStopped(Mockito.any());
        reader.verifyHandler(Mockito.times(2)).onPartitionClosed(closed.capture());

        Assert.assertEquals(started.getValue().getPartitionSession(), closed.getValue().getPartitionSession());
        started.getValue().confirm(); // has no effect

        reader.verifyHandler(Mockito.times(2)).onPartitionStarted(Mockito.any());
        reader.verifyHandler(Mockito.never()).onPartitionStopped(Mockito.any());
        reader.verifyHandler(Mockito.times(2)).onPartitionClosed(Mockito.any());

        reader.close();
        reader.assertReaderIsStopped(Status.SUCCESS);
        mock.assertIsClosed();
    }

    @Test
    @HideLoggers(ReadSession.class)
    public void duplicateStartPartitionRequestTest() {
        ReadStreamMock mock = new ReadStreamMock();
        TestImpl reader = startReader(mock);

        PartitionSession ps1 = new PartitionSession(1, 123, TOPIC1);
        mock.responseStartPartition(ps1, 0);

        ArgumentCaptor<StartPartitionSessionEvent> start = ArgumentCaptor.forClass(StartPartitionSessionEvent.class);
        ArgumentCaptor<PartitionSessionClosedEvent> closed = ArgumentCaptor.forClass(PartitionSessionClosedEvent.class);

        reader.verifyHandler().onPartitionStarted(start.capture());
        Assert.assertEquals(ps1, start.getValue().getPartitionSession());

        // Once recived, a second start partition request with the same id is a protocol error.
        mock.responseStartPartition(new PartitionSession(1, 222, TOPIC1), 10);


        // partiton is not started
        reader.verifyHandler(Mockito.never()).onPartitionStopped(Mockito.any());
        // but closed
        reader.verifyHandler().onPartitionClosed(closed.capture());
        Assert.assertEquals(ps1, closed.getValue().getPartitionSession());


        reader.assertReaderIsStopped(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, Issue.of(
                "Restarting read session due to receiving second StartPartitionSessionRequest with "
                        + "Partition session 1 (partition 222) for topic \"/test/topic\"",
                Issue.Severity.FATAL)));
        reader.close();

        mock.assertSentMessagesCount(2);
        mock.assertIsClosed();

        mock.assertIsClosed();
    }

    @Test
    @HideLoggers(ReadSession.class)
    public void duplicateStopPartitionRequestTest() {
        ReadStreamMock mock = new ReadStreamMock();
        TestImpl reader = startReader(mock);

        mock.responseStartPartition(TOPIC1, 123, 0);
        ArgumentCaptor<StartPartitionSessionEvent> start = ArgumentCaptor.forClass(StartPartitionSessionEvent.class);
        reader.verifyHandler().onPartitionStarted(start.capture());
        start.getValue().confirm();

        // Both requests arrive before the application confirms the stop.
        mock.responseStopPartition(1, true);
        mock.responseStopPartition(1, true);
        ArgumentCaptor<StopPartitionSessionEvent> stop = ArgumentCaptor.forClass(StopPartitionSessionEvent.class);
        reader.verifyHandler(Mockito.times(1)).onPartitionStopped(stop.capture());
        Assert.assertSame(start.getValue().getPartitionSession(), stop.getValue().getPartitionSession());

        mock.assertSentMessagesCount(3);

        stop.getValue().confirm();
        mock.assertSentMessagesCount(4);
        mock.assertLastMessage().isStopPartition(1);

        stop.getValue().confirm(); // second confirm has no effect
        mock.assertSentMessagesCount(4);
        mock.assertIsActive();

        // Once confirmed, a graceful stop for the removed session is a protocol error.
        mock.responseStopPartition(1, true);
        reader.verifyHandler(Mockito.times(1)).onPartitionStopped(Mockito.any());
        reader.assertReaderIsStopped(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, Issue.of(
                "Restarting read session due to receiving StopPartitionSessionRequest with PartitionSessionId 1 "
                        + "that SDK knows nothing about",
                Issue.Severity.FATAL)));

        mock.assertSentMessagesCount(4);
        mock.assertIsClosed();
        reader.close();
        mock.assertIsClosed();
    }

    @Test
    public void duplicateForcedStopPartitionRequestTest() {
        ReadStreamMock mock = new ReadStreamMock();
        TestImpl reader = startReader(mock);

        mock.responseStartPartition(TOPIC1, 123, 0);
        ArgumentCaptor<StartPartitionSessionEvent> start = ArgumentCaptor.forClass(StartPartitionSessionEvent.class);
        reader.verifyHandler().onPartitionStarted(start.capture());
        start.getValue().confirm();

        mock.responseStopPartition(1, false);
        mock.responseStopPartition(1, false);
        ArgumentCaptor<PartitionSessionClosedEvent> closed = ArgumentCaptor.forClass(PartitionSessionClosedEvent.class);
        reader.verifyHandler(Mockito.times(1)).onPartitionClosed(closed.capture());
        Assert.assertEquals(1L, closed.getValue().getPartitionSession().getId());
        Assert.assertTrue(reader.stops.isEmpty());
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
        ReadConfig config = new ReadConfig(REGISTRY, Runnable::run, Runnable::run, Runnable::run, settings);
        ReaderImpl reader = new TestImpl(mockRpc(mock), "test-reader", settings, config);

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
        ReaderImpl reader = startReader(mock);

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
        ReaderImpl reader = startReader(mock);

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
        ReaderImpl reader = startReader(mock);

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

    private static class TestImpl extends ReaderImpl {
        private final List<String> starts = new ArrayList<>();
        private final List<Status> stops = new ArrayList<>();
        private final ReadSession.Handler handler;

        private TestImpl(TopicRpc rpc, String id, ReaderSettings settings, ReadConfig config, ReadSession.Handler h) {
            super(rpc, id, settings, config, h);
            this.handler = h;
        }

        public TestImpl(TopicRpc rpc, String id, ReaderSettings settings, ReadConfig config) {
            this(rpc, id, settings, config, Mockito.mock(ReadSession.Handler.class));
        }

        @Override
        protected void onSessionStarted(String sessionId) {
            starts.add(sessionId);
        }

        @Override
        protected void onReaderClosed(Status status) {
            stops.add(status);
        }

        public ReadSession.Handler verifyHandler() {
            return Mockito.verify(handler);
        }

        public ReadSession.Handler verifyHandler(VerificationMode mode) {
            return Mockito.verify(handler, mode);
        }

        public void assertReaderIsStopped(Status expected) {
            Assert.assertEquals(1, stops.size());
            Assert.assertEquals(expected, stops.get(0));
        }
    }

    private static class TestExecutor implements Executor {
        private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

        @Override
        public void execute(Runnable command) {
            queue.add(command);
        }

        public TestExecutor assertSize(int size) {
            Assert.assertEquals(size, queue.size());
            return this;
        }

        public TestExecutor executeNext() {
            Runnable next = queue.poll();
            Assert.assertNotNull(next);
            next.run();
            return this;
        }
    }

    private static class TxMock implements YdbTransaction {
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
