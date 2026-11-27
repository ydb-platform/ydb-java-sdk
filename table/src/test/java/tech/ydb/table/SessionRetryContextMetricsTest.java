package tech.ydb.table;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.tracing.Span;
import tech.ydb.core.tracing.SpanKind;
import tech.ydb.core.tracing.Tracer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SessionRetryContextMetricsTest {
    private final SessionSupplier sessionSupplier = mock(SessionSupplier.class);
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private final Tracer tracer = mock(Tracer.class);
    private final Meter meter = mock(Meter.class);
    private final DoubleHistogram duration = mock(DoubleHistogram.class);
    private final DoubleHistogram attempts = mock(DoubleHistogram.class);

    private final ArgumentMatcher<Attr> myOp = a -> "operation.name".equals(a.getKey()) && "MyOp".equals(a.getValue());

    @Before
    public void setup() {
        when(sessionSupplier.getMeter()).thenReturn(meter);
        when(sessionSupplier.getTracer()).thenReturn(tracer);
        when(sessionSupplier.getScheduler()).thenReturn(scheduler);
        when(sessionSupplier.createSession(any()))
                .thenReturn(CompletableFuture.completedFuture(Result.success(mock(Session.class))));
        when(tracer.startSpan(anyString(), any())).thenReturn(Span.NOOP);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any())).thenAnswer(inv -> {
            inv.<Runnable>getArgument(0).run();
            return null;
        });

        when(meter.createHistogram(eq("ydb.client.retry.duration"), eq("s"), anyString(),
                eq(Arrays.asList(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1d, 2d, 5d, 10d, 30d)))).thenReturn(duration);
        when(meter.createHistogram(eq("ydb.client.retry.attempts"), eq("{attempt}"), anyString(),
                eq(Arrays.asList(1d, 2d, 3d, 4d, 5d, 7d, 10d, 20d)))).thenReturn(attempts);
    }

    private SessionRetryContext retryContext() {
        return SessionRetryContext.create(sessionSupplier).maxRetries(2).build();
    }

    @Test
    public void namedOperationSucceededOnFirstAttempt() {
        Result<Integer> result = retryContext().supplyResult("MyOp",
                session -> CompletableFuture.completedFuture(Result.success(42))).join();

        Assert.assertEquals(Integer.valueOf(42), result.getValue());
        verify(tracer).startSpan("MyOp", SpanKind.INTERNAL);
        verify(tracer, never()).startSpan("ydb.RunWithRetry", SpanKind.INTERNAL);
        verify(attempts).record(eq(1d), argThat(myOp));
        verify(duration).record(anyDouble(), argThat(myOp));
        verifyNoMoreInteractions(attempts, duration);
    }

    @Test
    public void retriedOperationWithoutNameHasNoAttributes() {
        AtomicInteger calls = new AtomicInteger();
        Status status = retryContext().supplyStatus(session -> CompletableFuture.completedFuture(
                calls.getAndIncrement() == 0 ? Status.of(StatusCode.OVERLOADED) : Status.SUCCESS)).join();

        status.expectSuccess();
        verify(tracer).startSpan("ydb.RunWithRetry", SpanKind.INTERNAL);
        verify(attempts).record(2d);
        verify(duration).record(anyDouble());
        verifyNoMoreInteractions(attempts, duration);
    }

    @Test
    public void nonRetryableFailureIsSingleAttempt() {
        Status status = retryContext().supplyStatus("MyOp", SessionRetryHandler.DEFAULT,
                session -> CompletableFuture.completedFuture(Status.of(StatusCode.SCHEME_ERROR))).join();

        Assert.assertEquals(StatusCode.SCHEME_ERROR, status.getCode());
        verify(attempts).record(eq(1d), argThat(myOp));
        verify(duration).record(anyDouble(), argThat(myOp));
        verifyNoMoreInteractions(attempts, duration);
    }

    @Test
    public void exhaustedSessionRetriesCountEveryAttempt() {
        when(sessionSupplier.createSession(any()))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(Status.of(StatusCode.OVERLOADED))));

        Status status = retryContext().supplyStatus("MyOp", session -> {
            throw new AssertionError("session must not be acquired");
        }).join();

        Assert.assertEquals(StatusCode.OVERLOADED, status.getCode());
        verify(attempts).record(eq(3d), argThat(myOp));
        verify(duration).record(anyDouble(), argThat(myOp));
        verifyNoMoreInteractions(attempts, duration);
    }
}
