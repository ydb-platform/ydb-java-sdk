package tech.ydb.core.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.MetricAttributes;
import tech.ydb.core.tracing.Span;

public class DiagnosticCallTest {
    private static final String DATABASE = "/local";
    private static final String ENDPOINT = "host:2136";

    @Test
    public void recordsDurationAndSuccessSpanOnStatus() {
        DoubleHistogram duration = Mockito.mock(DoubleHistogram.class);
        LongCounter failed = Mockito.mock(LongCounter.class);
        Span span = Mockito.mock(Span.class);

        long start = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(20);
        CompletableFuture<Status> future = new CompletableFuture<>();
        CompletableFuture<Status> wrapped = DiagnosticCall.endOnStatus(
                "Commit", span, start, duration, failed, future,
                MetricAttributes.DATABASE, DATABASE,
                MetricAttributes.ENDPOINT, ENDPOINT);

        future.complete(Status.SUCCESS);
        Status result = wrapped.join();

        Assert.assertEquals(Status.SUCCESS, result);

        ArgumentCaptor<Double> durationValue = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<String[]> durationAttrs = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(duration).record(durationValue.capture(), durationAttrs.capture());
        Assert.assertTrue("duration must be positive", durationValue.getValue() > 0.0);
        Assert.assertTrue("duration must be < 5s for fresh nanoTime", durationValue.getValue() < 5.0);
        Assert.assertArrayEquals(new String[] {
                MetricAttributes.OPERATION_NAME, "Commit",
                MetricAttributes.DATABASE, DATABASE,
                MetricAttributes.ENDPOINT, ENDPOINT
        }, durationAttrs.getValue());

        Mockito.verifyNoInteractions(failed);
        Mockito.verify(span).setStatus(Status.SUCCESS, null);
        Mockito.verify(span).end();
    }

    @Test
    public void incrementsFailedOnNonSuccessStatus() {
        DoubleHistogram duration = Mockito.mock(DoubleHistogram.class);
        LongCounter failed = Mockito.mock(LongCounter.class);
        Span span = Mockito.mock(Span.class);

        long start = System.nanoTime();
        CompletableFuture<Status> future = new CompletableFuture<>();
        DiagnosticCall.endOnStatus(
                "ExecuteQuery", span, start, duration, failed, future,
                MetricAttributes.DATABASE, DATABASE);

        Status badRequest = Status.of(StatusCode.BAD_REQUEST, Issue.of("bad", Issue.Severity.ERROR));
        future.complete(badRequest);

        Mockito.verify(duration).record(Mockito.anyDouble(), Mockito.any(String[].class));

        ArgumentCaptor<Long> failedValue = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String[]> failedAttrs = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(failed).add(failedValue.capture(), failedAttrs.capture());
        Assert.assertEquals(Long.valueOf(1L), failedValue.getValue());
        Assert.assertArrayEquals(new String[] {
                MetricAttributes.OPERATION_NAME, "ExecuteQuery",
                MetricAttributes.STATUS_CODE, StatusCode.BAD_REQUEST.toString(),
                MetricAttributes.DATABASE, DATABASE
        }, failedAttrs.getValue());

        Mockito.verify(span).setStatus(badRequest, null);
        Mockito.verify(span).end();
    }

    @Test
    public void resultStatusIsExtractedOnSuccess() {
        DoubleHistogram duration = Mockito.mock(DoubleHistogram.class);
        LongCounter failed = Mockito.mock(LongCounter.class);
        Span span = Mockito.mock(Span.class);

        long start = System.nanoTime();
        CompletableFuture<Result<String>> future = new CompletableFuture<>();
        CompletableFuture<Result<String>> wrapped = DiagnosticCall.endOnResult(
                "Rollback", span, start, duration, failed, future,
                MetricAttributes.DATABASE, DATABASE,
                MetricAttributes.ENDPOINT, ENDPOINT);

        future.complete(Result.success("ok"));
        Result<String> result = wrapped.join();

        Assert.assertTrue(result.isSuccess());
        Mockito.verify(duration).record(Mockito.anyDouble(), Mockito.any(String[].class));
        Mockito.verifyNoInteractions(failed);
        Mockito.verify(span).setStatus(Status.SUCCESS, null);
        Mockito.verify(span).end();
    }

    @Test
    public void resultFailureIncrementsCounterAndKeepsBaseAttributesOrder() {
        DoubleHistogram duration = Mockito.mock(DoubleHistogram.class);
        LongCounter failed = Mockito.mock(LongCounter.class);
        Span span = Mockito.mock(Span.class);

        Status overloaded = Status.of(StatusCode.OVERLOADED);
        long start = System.nanoTime();
        CompletableFuture<Result<String>> future = new CompletableFuture<>();
        DiagnosticCall.endOnResult(
                "ExecuteQuery", span, start, duration, failed, future,
                MetricAttributes.DATABASE, DATABASE,
                MetricAttributes.ENDPOINT, ENDPOINT);

        future.complete(Result.fail(overloaded));

        ArgumentCaptor<String[]> failedAttrs = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(failed).add(Mockito.eq(1L), failedAttrs.capture());
        Assert.assertArrayEquals(new String[] {
                MetricAttributes.OPERATION_NAME, "ExecuteQuery",
                MetricAttributes.STATUS_CODE, StatusCode.OVERLOADED.toString(),
                MetricAttributes.DATABASE, DATABASE,
                MetricAttributes.ENDPOINT, ENDPOINT
        }, failedAttrs.getValue());

        Mockito.verify(span).setStatus(overloaded, null);
        Mockito.verify(span).end();
    }

    @Test
    public void exceptionalCompletionEndsSpanWithUnwrappedError() {
        DoubleHistogram duration = Mockito.mock(DoubleHistogram.class);
        LongCounter failed = Mockito.mock(LongCounter.class);
        Span span = Mockito.mock(Span.class);

        RuntimeException boom = new RuntimeException("boom");
        long start = System.nanoTime();
        CompletableFuture<Status> future = new CompletableFuture<>();
        CompletableFuture<Status> wrapped = DiagnosticCall.endOnStatus(
                "Commit", span, start, duration, failed, future);

        future.completeExceptionally(boom);
        try {
            wrapped.join();
            Assert.fail("future must propagate exception");
        } catch (CompletionException ex) {
            Assert.assertSame(boom, ex.getCause());
        }

        ArgumentCaptor<String[]> durationAttrs = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(duration).record(Mockito.anyDouble(), durationAttrs.capture());
        Assert.assertArrayEquals(
                new String[] {MetricAttributes.OPERATION_NAME, "Commit"},
                durationAttrs.getValue());

        Mockito.verifyNoInteractions(failed);
        Mockito.verify(span).setStatus(null, boom);
        Mockito.verify(span).end();
    }

    @Test
    public void worksWithoutBaseAttributes() {
        DoubleHistogram duration = Mockito.mock(DoubleHistogram.class);
        LongCounter failed = Mockito.mock(LongCounter.class);
        Span span = Mockito.mock(Span.class);

        long start = System.nanoTime();
        CompletableFuture<Result<String>> future = new CompletableFuture<>();
        DiagnosticCall.endOnResult("Foo", span, start, duration, failed, future);

        Status unavailable = Status.of(StatusCode.UNAVAILABLE);
        future.complete(Result.fail(unavailable));

        ArgumentCaptor<String[]> durationAttrs = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(duration).record(Mockito.anyDouble(), durationAttrs.capture());
        Assert.assertArrayEquals(
                new String[] {MetricAttributes.OPERATION_NAME, "Foo"},
                durationAttrs.getValue());

        ArgumentCaptor<String[]> failedAttrs = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(failed).add(Mockito.eq(1L), failedAttrs.capture());
        Assert.assertArrayEquals(new String[] {
                MetricAttributes.OPERATION_NAME, "Foo",
                MetricAttributes.STATUS_CODE, StatusCode.UNAVAILABLE.toString()
        }, failedAttrs.getValue());

        Mockito.verify(span).setStatus(unavailable, null);
        Mockito.verify(span).end();
    }

    @Test
    public void durationMatchesElapsedNanoTime() throws InterruptedException {
        DoubleHistogram duration = Mockito.mock(DoubleHistogram.class);
        LongCounter failed = Mockito.mock(LongCounter.class);
        Span span = Mockito.mock(Span.class);

        long start = System.nanoTime();
        Thread.sleep(20);
        CompletableFuture<Status> future = new CompletableFuture<>();
        DiagnosticCall.endOnStatus("Sleep", span, start, duration, failed, future);
        future.complete(Status.SUCCESS);

        ArgumentCaptor<Double> durationValue = ArgumentCaptor.forClass(Double.class);
        Mockito.verify(duration).record(durationValue.capture(), Mockito.any(String[].class));
        double seconds = durationValue.getValue();
        Assert.assertTrue("expected at least 15ms, got " + seconds, seconds >= 0.015);
        Assert.assertTrue("expected less than 5s, got " + seconds, seconds < 5.0);
    }
}
