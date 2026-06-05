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

import tech.ydb.core.Result;
import tech.ydb.core.Status;
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
import tech.ydb.query.QuerySession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    private final ArgumentMatcher<Attr> poolName = a -> attr(a, "pool.name", POOL);
    private final ArgumentMatcher<Attr> stateIdle = a -> attr(a, "state", "idle");
    private final ArgumentMatcher<Attr> stateInUse = a -> attr(a, "state", "in_use");
    private final ArgumentMatcher<Attr> statusOverloaded = a -> attr(a, "status_code", "OVERLOADED");

    @Before
    public void setup() {
        when(scheduler.scheduleAtFixedRate(any(), anyLong(), anyLong(), any()))
                .thenAnswer(inv -> mock(ScheduledFuture.class));
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
            verify(meter).createCounter(eq(PREFIX + "deleted"), eq("{session}"), anyString());
            verify(meter).createCounter(eq(PREFIX + "acquired"), eq("{session}"), anyString());
            verify(meter).createCounter(eq(PREFIX + "released"), eq("{session}"), anyString());
            verify(meter).createCounter(eq(PREFIX + "requested"), eq("{session}"), anyString());
            verify(meter).createCounter(eq(PREFIX + "failed"), eq("{session}"), anyString());
            verify(meter).createHistogram(eq(PREFIX + "create_time"), eq("s"), anyString());
            verify(meter).createLongGauge(eq(PREFIX + "max"), eq("{session}"), anyString(), any());
            verify(meter).createLongGauge(eq(PREFIX + "min"), eq("{session}"), anyString(), any());
            verify(meter).createLongGauge(eq(PREFIX + "count"), eq("{session}"), anyString(), any());
            verify(meter).createLongGauge(eq(PREFIX + "pending_requests"), eq("{session}"), anyString(), any());
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

        verify(counter("deleted")).add(eq(1L), argThat(poolName));
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
                    .build();
            return CompletableFuture.completedFuture(Result.success(response));
        }

        @Override
        public GrpcReadStream<YdbQuery.SessionState> attachSession(
                YdbQuery.AttachSessionRequest request, GrpcRequestSettings settings) {
            YdbQuery.SessionState message = YdbQuery.SessionState.newBuilder()
                    .setStatus(StatusIds.StatusCode.SUCCESS)
                    .build();
            return new GrpcReadStream<YdbQuery.SessionState>() {
                @Override
                public CompletableFuture<Status> start(Observer<YdbQuery.SessionState> observer) {
                    observer.onNext(message);
                    return new CompletableFuture<>();
                }

                @Override
                public void cancel() {
                }
            };
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
}
