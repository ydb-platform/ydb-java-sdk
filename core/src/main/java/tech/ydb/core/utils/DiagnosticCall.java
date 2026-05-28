package tech.ydb.core.utils;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import io.grpc.ExperimentalApi;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.MetricAttributes;
import tech.ydb.core.tracing.Span;

/**
 * Static helpers for recording diagnostics (span + metrics) around client operations.
 */
@ExperimentalApi("YDB diagnostic API is experimental and may change without notice")
public final class DiagnosticCall {
    private static final double NANOS_IN_SECOND = 1_000_000_000.0;

    private DiagnosticCall() {
    }

    /**
     * Subscribes to a {@link Status} future and records operation span and metrics when it completes.
     *
     * @param operationName logical operation name
     * @param span operation span
     * @param startNanos operation start timestamp from {@link System#nanoTime()}
     * @param duration operation duration histogram
     * @param failed operation failure counter
     * @param future future to observe
     * @param baseAttributes alternating {@code key, value} pairs added to every metric point
     * @return completion stage with diagnostics attached
     */
    public static CompletableFuture<Status> endOnStatus(
            String operationName,
            Span span,
            long startNanos,
            DoubleHistogram duration,
            LongCounter failed,
            CompletableFuture<Status> future,
            String... baseAttributes) {
        return future.whenComplete((status, th) -> {
            recordDuration(operationName, startNanos, duration, baseAttributes);
            recordFailed(operationName, failed, status, baseAttributes);
            endSpan(span, status, FutureTools.unwrapCompletionException(th));
        });
    }

    /**
     * Subscribes to a {@link Result} future and records operation span and metrics when it completes.
     *
     * @param <T> result value type
     * @param operationName logical operation name
     * @param span operation span
     * @param startNanos operation start timestamp from {@link System#nanoTime()}
     * @param duration operation duration histogram
     * @param failed operation failure counter
     * @param future future to observe
     * @param baseAttributes alternating {@code key, value} pairs added to every metric point
     * @return completion stage with diagnostics attached
     */
    public static <T> CompletableFuture<Result<T>> endOnResult(
            String operationName,
            Span span,
            long startNanos,
            DoubleHistogram duration,
            LongCounter failed,
            CompletableFuture<Result<T>> future,
            String... baseAttributes) {
        return future.whenComplete((result, th) -> {
            Status status = result != null ? result.getStatus() : null;
            recordDuration(operationName, startNanos, duration, baseAttributes);
            recordFailed(operationName, failed, status, baseAttributes);
            endSpan(span, status, FutureTools.unwrapCompletionException(th));
        });
    }

    private static void recordDuration(
            String operationName,
            long startNanos,
            DoubleHistogram duration,
            String... baseAttributes) {
        duration.record((System.nanoTime() - startNanos) / NANOS_IN_SECOND,
                operationAttributes(operationName, baseAttributes));
    }

    private static void recordFailed(
            String operationName,
            LongCounter failed,
            @Nullable Status status,
            String... baseAttributes) {
        if (status != null && !status.isSuccess()) {
            failed.add(1L, failedAttributes(operationName, status, baseAttributes));
        }
    }

    private static void endSpan(Span span, @Nullable Status status, @Nullable Throwable error) {
        span.setStatus(status, error);
        span.end();
    }

    private static String[] operationAttributes(String operationName, String[] baseAttributes) {
        String[] attributes = new String[baseLength(baseAttributes) + 2];
        attributes[0] = MetricAttributes.OPERATION_NAME;
        attributes[1] = operationName;
        copyBaseAttributes(baseAttributes, attributes, 2);
        return attributes;
    }

    private static String[] failedAttributes(String operationName, Status status, String[] baseAttributes) {
        String[] attributes = new String[baseLength(baseAttributes) + 4];
        attributes[0] = MetricAttributes.OPERATION_NAME;
        attributes[1] = operationName;
        attributes[2] = MetricAttributes.STATUS_CODE;
        attributes[3] = status.getCode().toString();
        copyBaseAttributes(baseAttributes, attributes, 4);
        return attributes;
    }

    private static int baseLength(String[] baseAttributes) {
        return baseAttributes == null ? 0 : baseAttributes.length;
    }

    private static void copyBaseAttributes(String[] source, String[] target, int offset) {
        if (source != null && source.length > 0) {
            System.arraycopy(source, 0, target, offset, source.length);
        }
    }
}
