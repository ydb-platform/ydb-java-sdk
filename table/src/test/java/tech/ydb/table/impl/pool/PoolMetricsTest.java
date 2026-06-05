package tech.ydb.table.impl.pool;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import tech.ydb.core.Result;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.table.Session;

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

public class PoolMetricsTest extends FutureHelper {
    private static final Duration TIMEOUT = Duration.ofMillis(50);
    private static final String POOL = "my-pool";
    private static final String PREFIX = "ydb.table.session.";

    private final MockedClock clock = MockedClock.create(ZoneId.of("UTC"));
    private final MockedScheduler scheduler = new MockedScheduler(clock);
    private final MockedTableRpc tableRpc = new MockedTableRpc(clock, scheduler);

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
        clock.reset(Instant.parse("2022-07-01T00:00:00.000Z"));
        when(meter.createCounter(anyString(), any(), any()))
                .thenAnswer(inv -> counters.computeIfAbsent(inv.getArgument(0), k -> mock(LongCounter.class)));
        when(meter.createHistogram(anyString(), any(), any())).thenReturn(createTime);
        doAnswer(inv -> {
            gauges.put(inv.getArgument(0), inv.getArgument(3));
            return null;
        }).when(meter).createLongGauge(anyString(), any(), any(), any());
    }

    @After
    public void close() {
        scheduler.shutdown();
        scheduler.check().isClosed().hasNoTasks();
        tableRpc.check().hasNoSessions().hasNoPendingRequests();
    }

    @Test
    public void allInstrumentsAreCreated() {
        SessionPool pool = createPool(0, 2);

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

        pool.close();
    }

    @Test
    public void sessionLifecycleRecordsCounters() {
        SessionPool pool = createPool(0, 2);

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        verify(counter("requested")).add(eq(1L), argThat(poolName));

        tableRpc.nextCreateSession().completeSuccess();
        verify(counter("created")).add(eq(1L), argThat(poolName));
        verify(createTime).record(anyDouble(), argThat(poolName));
        verify(counter("acquired")).add(eq(1L), argThat(poolName));

        Session s1 = futureIsReady(f1).getValue();
        s1.close();
        verify(counter("released")).add(eq(1L), argThat(poolName));

        pool.close();
        verify(counter("deleted")).add(eq(1L), argThat(poolName));
        tableRpc.completeSessionDeleteRequests();

        verify(counter("failed"), never()).add(anyLong(), any());
    }

    @Test
    public void failedCreateRecordsFailedCounter() {
        SessionPool pool = createPool(0, 2);

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        tableRpc.nextCreateSession().completeOverloaded();

        futureIsReady(f1);
        verify(counter("requested")).add(eq(1L), argThat(poolName));
        verify(counter("failed")).add(eq(1L), argThat(poolName), argThat(statusOverloaded));
        verify(createTime).record(anyDouble(), argThat(poolName));
        verify(counter("created"), never()).add(anyLong(), any());
        verify(counter("acquired"), never()).add(anyLong(), any());

        pool.close();
    }

    @Test
    public void gaugesObserveStats() {
        SessionPool pool = createPool(0, 2);

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();

        Session s1 = futureIsReady(f1).getValue();
        Session s2 = futureIsReady(f2).getValue();

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
        pool.close();
        tableRpc.completeSessionDeleteRequests();
    }

    private SessionPool createPool(int minSize, int maxSize) {
        return new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT.withSize(minSize, maxSize), meter, POOL);
    }

    private LongCounter counter(String shortName) {
        return counters.get(PREFIX + shortName);
    }

    private static boolean attr(Attr attr, String shortKey, String value) {
        return attr.getKey().equals(PREFIX + shortKey) && attr.getValue().equals(value);
    }
}
