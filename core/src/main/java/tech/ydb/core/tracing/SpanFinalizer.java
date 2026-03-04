package tech.ydb.core.tracing;

import java.util.function.BiConsumer;

import tech.ydb.core.Status;
import tech.ydb.core.utils.FutureTools;

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

        if (status != null && !status.isSuccess()) {
            span.setError(status);
        }

        span.end();
    }

    public static void finishByError(Span span, Throwable error) {
        if (span == null) {
            return;
        }

        if (error != null) {
            span.setError(FutureTools.unwrapCompletionException(error));
        }

        span.end();
    }

    public static BiConsumer<Status, Throwable> whenComplete(Span span) {
        return (status, error) -> {
            if (span == null) {
                return;
            }

            if (error != null) {
                finishByError(span, error);
                return;
            }
            finishByStatus(span, status);
        };
    }
}
