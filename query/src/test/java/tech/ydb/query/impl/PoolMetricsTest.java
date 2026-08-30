package tech.ydb.query.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.tracing.NoopTracer;
import tech.ydb.proto.StatusCodesProtos.StatusIds;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryStream;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.table.TableClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PoolMetricsTest {
    private static final Duration TIMEOUT = Duration.ofMillis(50);
    private static final Duration IDLE = Duration.ofMinutes(5);
    private static final String POOL = "my-pool";
    private static final String PREFIX = "ydb.query.session.";

    private final Clock clock = Clock.fixed(java.time.Instant.parse("2022-07-01T00:00:00.000Z"), ZoneId.of("UTC"));
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private final TestRpc rpc = new TestRpc();

    private final Meter meter = mock(Meter.class);
    private final DoubleHistogram createTime = mock(DoubleHistogram.class);
    private final Map<String, LongCounter> counters = new HashMap<>();
    private final Map<String, Consumer<LongMeasurement>> gauges = new HashMap<>();
    private Runnable cleaner;

    private final ArgumentMatcher<Attr> poolName = a -> attr(a, "pool.name", POOL);
    private final ArgumentMatcher<Attr> stateIdle = a -> attr(a, "state", "idle");
    private final ArgumentMatcher<Attr> stateInUse = a -> attr(a, "state", "in_use");
    private final ArgumentMatcher<Attr> statusOverloaded = a -> attr(a, "status_code", "OVERLOADED");

    @Before
    public void setup() {
        when(scheduler.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any()))
                .thenAnswer(inv -> {
                    cleaner = inv.getArgument(0);
                    return mock(ScheduledFuture.class);
                });
        when(scheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenAnswer(inv -> mock(ScheduledFuture.class));

        when(meter.createCounter(anyString(), any(), any()))
                .thenAnswer(inv -> counters.computeIfAbsent(inv.getArgument(0), k -> mock(LongCounter.class)));
        when(meter.createHistogram(anyString(), any(), any())).thenReturn(createTime);
        doAnswer(inv -> {
            gauges.put(inv.getArgument(0), inv.getArgument(3));
            return null;
        }).when(meter).createLongGauge(anyString(), any(), any(), any());
    }

    @Test
    public void allInstrumentsAreCreated() {
        try (SessionPool pool = createPool(0, 2)) {
            verify(meter).createCounter(eq(PREFIX + "created"), eq("{session}"), anyString());
            verify(meter).createCounter(eq(PREFIX + "acquired"), eq("{session}"), anyString());
            verify(meter).createCounter(eq(PREFIX + "released"), eq("{session}"), anyString());
            verify(meter).createCounter(eq(PREFIX + "requested"), eq("{session}"), anyString());
            verify(meter).createCounter(eq(PREFIX + "failed"), eq("{session}"), anyString());
            verify(meter).createCounter(eq(PREFIX + "closed"), eq("{session}"), anyString());
            verify(meter).createHistogram(eq(PREFIX + "create_time"), eq("s"), anyString());
            verify(meter).createLongGauge(eq(PREFIX + "max"), eq("{session}"), anyString(), any());
            verify(meter).createLongGauge(eq(PREFIX + "min"), eq("{session}"), anyString(), any());
            verify(meter).createLongGauge(eq(PREFIX + "count"), eq("{session}"), anyString(), any());
            verify(meter).createLongGauge(eq(PREFIX + "pending_requests"), eq("{session}"), anyString(), any());
        }
    }

    @Test
    public void queryAndTableClientsUseSameClosedCounter() {
        GrpcTransport transport = mock(GrpcTransport.class);
        when(transport.getScheduler()).thenReturn(scheduler);
        when(transport.getTracer()).thenReturn(NoopTracer.getInstance());

        try (QueryClient queryClient = QueryClient.newClient(transport).withMeter(meter, POOL).build();
                TableClient tableClient = QueryClient.newTableClient(transport).withMeter(meter, POOL).build()) {
            verify(meter, times(2)).createCounter(eq(PREFIX + "closed"), eq("{session}"), anyString());
        }
    }

    @Test
    public void sessionLifecycleRecordsCounters() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            verify(counter("requested")).add(eq(1L), argThat(poolName));
            verify(counter("created")).add(eq(1L), argThat(poolName));
            verify(createTime).record(anyDouble(), argThat(poolName));
            verify(counter("acquired")).add(eq(1L), argThat(poolName));

            session.close();
            verify(counter("released")).add(eq(1L), argThat(poolName));
        }

        verify(counter("failed"), never()).add(anyLong(), any());
    }

    @Test
    public void failedCreateRecordsFailedCounter() {
        rpc.overloaded = true;
        try (SessionPool pool = createPool(0, 2)) {
            Result<QuerySession> result = pool.acquire(TIMEOUT).join();
            Assert.assertFalse(result.isSuccess());

            verify(counter("requested")).add(eq(1L), argThat(poolName));
            verify(counter("failed")).add(eq(1L), argThat(poolName), argThat(statusOverloaded));
            verify(createTime).record(anyDouble(), argThat(poolName));
            verify(counter("created"), never()).add(anyLong(), any());
            verify(counter("acquired"), never()).add(anyLong(), any());
        }
    }

    @Test
    public void gaugesObserveStats() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession s1 = acquireReady(pool);
            QuerySession s2 = acquireReady(pool);

            LongMeasurement max = mock(LongMeasurement.class);
            gauges.get(PREFIX + "max").accept(max);
            verify(max).record(eq(2L), argThat(poolName));

            LongMeasurement min = mock(LongMeasurement.class);
            gauges.get(PREFIX + "min").accept(min);
            verify(min).record(eq(0L), argThat(poolName));

            LongMeasurement count = mock(LongMeasurement.class);
            gauges.get(PREFIX + "count").accept(count);
            verify(count).record(eq(0L), argThat(poolName), argThat(stateIdle));
            verify(count).record(eq(2L), argThat(poolName), argThat(stateInUse));

            LongMeasurement pending = mock(LongMeasurement.class);
            gauges.get(PREFIX + "pending_requests").accept(pending);
            verify(pending).record(eq(0L), argThat(poolName));

            s1.close();

            LongMeasurement countAfterRelease = mock(LongMeasurement.class);
            gauges.get(PREFIX + "count").accept(countAfterRelease);
            verify(countAfterRelease).record(eq(1L), argThat(poolName), argThat(stateIdle));
            verify(countAfterRelease).record(eq(1L), argThat(poolName), argThat(stateInUse));

            s2.close();
        }
    }

    @Test
    public void poolGracefulShutdownRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            acquireReady(pool).close();
        }

        verifyClosed("pool_graceful_shutdown");
    }

    @Test
    public void poolIdleTimeoutRecordsClosedCounter() {
        SessionPool pool = new SessionPool(clock, rpc, scheduler, 0, 2, Duration.ZERO, meter, POOL);
        acquireReady(pool).close();
        cleaner.run();
        pool.close();

        verifyClosed("pool_idle_timeout");
    }

    @Test
    public void poolResizeRecordsClosedCounter() {
        SessionPool pool = createPool(0, 2);
        QuerySession first = acquireReady(pool);
        QuerySession second = acquireReady(pool);

        pool.updateMaxSize(1);
        first.close();

        verifyClosed("pool_resize");
        second.close();
        pool.close();
    }

    @Test
    public void nodeShutdownRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            rpc.sendAttachMessage(YdbQuery.SessionState.newBuilder()
                    .setStatus(StatusIds.StatusCode.SUCCESS)
                    .setNodeShutdown(YdbQuery.NodeShutdownHint.getDefaultInstance())
                    .build());
            rpc.completeAttach(Status.SUCCESS);
            session.close();
        }

        verifyClosed("node_shutdown");
    }

    @Test
    public void sessionShutdownRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            rpc.sendAttachMessage(YdbQuery.SessionState.newBuilder()
                    .setStatus(StatusIds.StatusCode.SUCCESS)
                    .setSessionShutdown(YdbQuery.SessionShutdownHint.getDefaultInstance())
                    .build());
            session.close();
        }

        verifyClosed("session_shutdown");
    }

    @Test
    public void firstAttachShutdownHintRecordsClosedCounter() {
        rpc.initialAttachMessage = YdbQuery.SessionState.newBuilder()
                .setStatus(StatusIds.StatusCode.SUCCESS)
                .setNodeShutdown(YdbQuery.NodeShutdownHint.getDefaultInstance())
                .build();

        try (SessionPool pool = createPool(0, 2)) {
            acquireReady(pool).close();
        }

        verifyClosed("node_shutdown");
    }

    @Test
    public void attachStreamEofRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            rpc.completeAttach(Status.SUCCESS);
            session.close();
        }

        verifyClosed("attach_closed");
    }

    @Test
    public void attachStreamFailureRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            rpc.failAttach(new RuntimeException("transport failure"));
            session.close();
        }

        verifyClosed("internal_error");
    }

    @Test
    public void clientQueryTimeoutRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            CompletableFuture<Result<QueryInfo>> query = session.createQuery("SELECT 1", TxMode.NONE).execute();
            rpc.completeQuery(Status.of(StatusCode.CLIENT_DEADLINE_EXCEEDED));
            Assert.assertFalse(query.join().isSuccess());

            rpc.sendAttachMessage(YdbQuery.SessionState.newBuilder()
                    .setStatus(StatusIds.StatusCode.SUCCESS)
                    .setNodeShutdown(YdbQuery.NodeShutdownHint.getDefaultInstance())
                    .build());
            rpc.completeAttach(Status.SUCCESS);
            session.close();
        }

        verifyClosed("client_timeout");
    }

    @Test
    public void queryStreamCancellationRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            QueryStream query = session.createQuery("SELECT 1", TxMode.NONE);
            CompletableFuture<Result<QueryInfo>> result = query.execute();
            query.cancel();
            Assert.assertFalse(result.join().isSuccess());

            rpc.sendAttachMessage(YdbQuery.SessionState.newBuilder()
                    .setStatus(StatusIds.StatusCode.SUCCESS)
                    .setSessionShutdown(YdbQuery.SessionShutdownHint.getDefaultInstance())
                    .build());
            rpc.completeAttach(Status.SUCCESS);
            session.close();
        }

        verifyClosed("client_cancelled");
    }

    @Test
    public void badSessionRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            CompletableFuture<Result<QueryInfo>> query = session.createQuery("SELECT 1", TxMode.NONE).execute();
            rpc.sendQueryMessage(StatusIds.StatusCode.BAD_SESSION);
            rpc.completeQuery(Status.SUCCESS);
            Assert.assertFalse(query.join().isSuccess());
            session.close();
        }

        verifyClosed("bad_session");
    }

    @Test
    public void sessionExpiredRecordsBadSessionCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            CompletableFuture<Result<QueryInfo>> query = session.createQuery("SELECT 1", TxMode.NONE).execute();
            rpc.sendQueryMessage(StatusIds.StatusCode.SESSION_EXPIRED);
            rpc.completeQuery(Status.SUCCESS);
            Assert.assertFalse(query.join().isSuccess());
            session.close();
        }

        verifyClosed("bad_session");
    }

    @Test
    public void sessionBusyRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            CompletableFuture<Result<QueryInfo>> query = session.createQuery("SELECT 1", TxMode.NONE).execute();
            rpc.sendQueryMessage(StatusIds.StatusCode.SESSION_BUSY);
            rpc.completeQuery(Status.SUCCESS);
            Assert.assertFalse(query.join().isSuccess());
            session.close();
        }

        verifyClosed("session_busy");
    }

    @Test
    public void transportUnavailableRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            CompletableFuture<Result<QueryInfo>> query = session.createQuery("SELECT 1", TxMode.NONE).execute();
            rpc.completeQuery(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));
            Assert.assertFalse(query.join().isSuccess());
            session.close();
        }

        verifyClosed("transport_error");
    }

    @Test
    public void internalErrorRecordsClosedCounter() {
        try (SessionPool pool = createPool(0, 2)) {
            QuerySession session = acquireReady(pool);
            CompletableFuture<Result<QueryInfo>> query = session.createQuery("SELECT 1", TxMode.NONE).execute();
            rpc.sendQueryMessage(StatusIds.StatusCode.INTERNAL_ERROR);
            rpc.completeQuery(Status.SUCCESS);
            Assert.assertFalse(query.join().isSuccess());
            session.close();
        }

        verifyClosed("internal_error");
    }

    @Test
    public void primaryAttachFailureDoesNotRecordClosedCounter() {
        rpc.initialAttachMessage = YdbQuery.SessionState.newBuilder()
                .setStatus(StatusIds.StatusCode.BAD_SESSION)
                .build();
        try (SessionPool pool = createPool(0, 2)) {
            Assert.assertFalse(pool.acquire(TIMEOUT).join().isSuccess());
        }

        verify(counter("closed"), never()).add(anyLong(), any());
    }

    private SessionPool createPool(int minSize, int maxSize) {
        return new SessionPool(clock, rpc, scheduler, minSize, maxSize, IDLE, meter, POOL);
    }

    private QuerySession acquireReady(SessionPool pool) {
        Result<QuerySession> result = pool.acquire(TIMEOUT).join();
        Assert.assertTrue("acquire must succeed", result.isSuccess());
        return result.getValue();
    }

    private LongCounter counter(String shortName) {
        return counters.get(PREFIX + shortName);
    }

    private void verifyClosed(String reason) {
        verify(counter("closed")).add(
                eq(1L),
                argThat(poolName),
                argThat(a -> a.getKey().equals("reason") && a.getValue().equals(reason))
        );
        verifyNoMoreInteractions(counter("closed"));
    }

    private static boolean attr(Attr attr, String shortKey, String value) {
        return attr.getKey().equals(PREFIX + shortKey) && attr.getValue().equals(value);
    }

    private static final GrpcTransport DUMMY_TRANSPORT = mock(GrpcTransport.class);

    static {
        when(DUMMY_TRANSPORT.getTracer()).thenReturn(NoopTracer.getInstance());
    }

    private static final class TestRpc extends QueryServiceRpc {
        private final AtomicInteger ids = new AtomicInteger();
        private volatile boolean overloaded = false;
        private volatile YdbQuery.SessionState initialAttachMessage = YdbQuery.SessionState.newBuilder()
                .setStatus(StatusIds.StatusCode.SUCCESS)
                .build();
        private TestAttachStream attachStream;
        private TestQueryStream queryStream;

        TestRpc() {
            super(DUMMY_TRANSPORT);
        }

        @Override
        public CompletableFuture<Result<YdbQuery.CreateSessionResponse>> createSession(
                YdbQuery.CreateSessionRequest request, GrpcRequestSettings settings) {
            StatusIds.StatusCode code = overloaded ? StatusIds.StatusCode.OVERLOADED : StatusIds.StatusCode.SUCCESS;
            YdbQuery.CreateSessionResponse response = YdbQuery.CreateSessionResponse.newBuilder()
                    .setStatus(code)
                    .setSessionId("session-" + ids.incrementAndGet())
                    .setNodeId(42)
                    .build();
            return CompletableFuture.completedFuture(Result.success(response));
        }

        @Override
        public GrpcReadStream<YdbQuery.SessionState> attachSession(
                YdbQuery.AttachSessionRequest request, GrpcRequestSettings settings) {
            attachStream = new TestAttachStream(initialAttachMessage);
            return attachStream;
                }

        void sendAttachMessage(YdbQuery.SessionState message) {
            attachStream.observer.onNext(message);
        }

        void completeAttach(Status status) {
            attachStream.completion.complete(status);
        }

        void failAttach(Throwable th) {
            attachStream.completion.completeExceptionally(th);
        }

                @Override
        public GrpcReadStream<YdbQuery.ExecuteQueryResponsePart> executeQuery(
                YdbQuery.ExecuteQueryRequest request, GrpcRequestSettings settings) {
            queryStream = new TestQueryStream();
            return queryStream;
                }

        void sendQueryMessage(StatusIds.StatusCode status) {
            queryStream.observer.onNext(YdbQuery.ExecuteQueryResponsePart.newBuilder()
                    .setStatus(status)
                    .build());
        }

        void completeQuery(Status status) {
            queryStream.completion.complete(status);
        }

        @Override
        public CompletableFuture<Result<YdbQuery.DeleteSessionResponse>> deleteSession(
                YdbQuery.DeleteSessionRequest request, GrpcRequestSettings settings) {
            return CompletableFuture.completedFuture(Result.success(
                    YdbQuery.DeleteSessionResponse.newBuilder()
                            .setStatus(StatusIds.StatusCode.SUCCESS)
                            .build()));
        }
    }

    private static final class TestAttachStream implements GrpcReadStream<YdbQuery.SessionState> {
        private final YdbQuery.SessionState initialMessage;
        private final CompletableFuture<Status> completion = new CompletableFuture<>();
        private Observer<YdbQuery.SessionState> observer;

        TestAttachStream(YdbQuery.SessionState initialMessage) {
            this.initialMessage = initialMessage;
}

        @Override
        public CompletableFuture<Status> start(Observer<YdbQuery.SessionState> observer) {
            this.observer = observer;
            if (initialMessage != null) {
                observer.onNext(initialMessage);
            }
            return completion;
        }

        @Override
        public void cancel() {
        }
    }

    private static final class TestQueryStream implements GrpcReadStream<YdbQuery.ExecuteQueryResponsePart> {
        private final CompletableFuture<Status> completion = new CompletableFuture<>();
        private Observer<YdbQuery.ExecuteQueryResponsePart> observer;

        @Override
        public CompletableFuture<Status> start(Observer<YdbQuery.ExecuteQueryResponsePart> observer) {
            this.observer = observer;
            return completion;
        }

        @Override
        public void cancel() {
            completion.complete(Status.of(StatusCode.CLIENT_CANCELLED));
        }
    }
}
