package tech.ydb.core.tracing;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.utils.FutureTools;

/**
 * A span represents a timed operation.
 */
public interface Span {

    String getId();

    /**
     * Sets a string attribute on the span (ignored by Noop implementation).
     *
     * @param key attribute key
     * @param value attribute value, may be null
     */
    void setAttribute(String key, @Nullable String value);

    /**
     * Sets a long attribute on the span (ignored by Noop implementation).
     *
     * @param key attribute key
     * @param value attribute value
     */
    void setAttribute(String key, long value);

    /**
     * Sets span status (success or error) with human-readable message.
     *
     * @param status operation status used to map span attributes
     * @param error operation exception used to map span attributes
     */
    void setStatus(Status status, Throwable error);

    void end();

    static CompletableFuture<Status> endOnStatus(Span span, CompletableFuture<Status> future) {
        if (span != null) {
            future.whenComplete((status, th) -> {
                span.setStatus(status, FutureTools.unwrapCompletionException(th));
                span.end();
            });
        }
        return future;
    }

    static <T> CompletableFuture<Result<T>> endOnResult(Span span, CompletableFuture<Result<T>> future) {
        if (span != null) {
            future.whenComplete((result, th) -> {
                span.setStatus(result != null ? result.getStatus() : null, FutureTools.unwrapCompletionException(th));
                span.end();
            });
        }
        return future;
    }
}
