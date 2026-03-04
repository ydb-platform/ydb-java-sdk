package tech.ydb.core.tracing;

import tech.ydb.core.Status;

/**
 * Shared helpers to finish spans for status/exception outcomes.
 */
public final class SpanFinalizer {
    private SpanFinalizer() {
    }

    public static void finishByStatus(Span span, Status status) {
        if (span == null) {
            return;
        }

        if (!status.isSuccess()) {
            span.setError(status);
        }

        span.end();
    }

    public static void finishByError(Span span, Throwable error) {
        if (span == null) {
            return;
        }

        if (error != null) {
            span.setError(error);
        }

        span.end();
    }
}
