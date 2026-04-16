package tech.ydb.query.tools;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.tracing.Scope;
import tech.ydb.core.tracing.Span;
import tech.ydb.core.tracing.SpanKind;
import tech.ydb.core.tracing.Tracer;
import tech.ydb.core.utils.FutureTools;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class SessionRetryContext {
    private static final String EXECUTE_SPAN_NAME = "ydb.RunWithRetry";
    private static final String EXECUTE_WITH_RETRY_SPAN_NAME = "ydb.Try";
    private static final String RETRY_BACKOFF_MS_ATTR = "ydb.retry.backoff_ms";

    private final QueryClient queryClient;
    private final Executor executor;
    private final Duration sessionCreationTimeout;
    private final int maxRetries;
    private final long backoffSlotMillis;
    private final int backoffCeiling;
    private final long fastBackoffSlotMillis;
    private final int fastBackoffCeiling;
    private final boolean retryNotFound;
    private final boolean idempotent;

    private SessionRetryContext(Builder b) {
        this.queryClient = b.queryClient;
        this.executor = b.executor;
        this.sessionCreationTimeout = b.sessionCreationTimeout;
        this.maxRetries = b.maxRetries;
        this.backoffSlotMillis = b.backoffSlotMillis;
        this.backoffCeiling = b.backoffCeiling;
        this.fastBackoffSlotMillis = b.fastBackoffSlotMillis;
        this.fastBackoffCeiling = b.fastBackoffCeiling;
        this.retryNotFound = b.retryNotFound;
        this.idempotent = b.idempotent;
    }

    public static Builder create(QueryClient sessionSupplier) {
        return new Builder(Objects.requireNonNull(sessionSupplier));
    }

    public <T> CompletableFuture<Result<T>> supplyResult(Function<QuerySession, CompletableFuture<Result<T>>> fn) {
        RetryableResultTask<T> task = new RetryableResultTask<>(fn);
        task.requestSession();
        return task.getFuture();
    }

    public CompletableFuture<Status> supplyStatus(Function<QuerySession, CompletableFuture<Status>> fn) {
        RetryableStatusTask task = new RetryableStatusTask(fn);
        task.requestSession();
        return task.getFuture();
    }

    private boolean canRetry(StatusCode code) {
        return code.isRetryable(idempotent) || (retryNotFound && code == StatusCode.NOT_FOUND);
    }

    private boolean canRetry(Throwable t) {
        Throwable cause = FutureTools.unwrapCompletionException(t);
        if (cause instanceof UnexpectedResultException) {
            StatusCode statusCode = ((UnexpectedResultException) cause).getStatus().getCode();
            return canRetry(statusCode);
        }
        return false;
    }

    private long backoffTimeMillisInternal(int retryNumber, long backoffSlotMillis, int backoffCeiling) {
        int slots = 1 << Math.min(retryNumber, backoffCeiling);
        long delay = backoffSlotMillis * slots;
        return delay + ThreadLocalRandom.current().nextLong(delay);
    }

    private long slowBackoffTimeMillis(int retryNumber) {
        return backoffTimeMillisInternal(retryNumber, backoffSlotMillis, backoffCeiling);
    }

    private long fastBackoffTimeMillis(int retryNumber) {
        return backoffTimeMillisInternal(retryNumber, fastBackoffSlotMillis, fastBackoffCeiling);
    }

    private long backoffTimeMillis(StatusCode code, int retryNumber) {
        switch (code) {
            case BAD_SESSION:
                // Instant retry
                return 0;
            case ABORTED:
            case CLIENT_CANCELLED:
            case CLIENT_INTERNAL_ERROR:
            case SESSION_BUSY:
            case TRANSPORT_UNAVAILABLE:
            case UNAVAILABLE:
            case UNDETERMINED:
                // Fast backoff
                return fastBackoffTimeMillis(retryNumber);
            case NOT_FOUND:
            case OVERLOADED:
            case CLIENT_RESOURCE_EXHAUSTED:
            default:
                // Slow backoff
                return slowBackoffTimeMillis(retryNumber);
        }
    }

    private long backoffTimeMillis(Throwable t, int retryNumber) {
        Throwable cause = FutureTools.unwrapCompletionException(t);
        if (cause instanceof UnexpectedResultException) {
            StatusCode statusCode = ((UnexpectedResultException) cause).getStatus().getCode();
            return backoffTimeMillis(statusCode, retryNumber);
        }
        return slowBackoffTimeMillis(retryNumber);
    }

    /**
     * BASE RETRYABLE TASK
     */
    private abstract class BaseRetryableTask<R> implements Runnable {
        private final CompletableFuture<R> promise = new CompletableFuture<>();
        private final AtomicInteger retryNumber = new AtomicInteger();
        private final Function<QuerySession, CompletableFuture<R>> fn;
        private final Tracer tracer;
        private final Span executeSpan;
        private Span trySpan;

        BaseRetryableTask(Function<QuerySession, CompletableFuture<R>> fn) {
            this.fn = fn;
            this.tracer = queryClient.getTracer();
            this.executeSpan = tracer.startSpan(EXECUTE_SPAN_NAME, SpanKind.INTERNAL);

            try (@SuppressWarnings("unused") Scope ignored = executeSpan.makeCurrent()) {
                this.trySpan = tracer.startSpan(EXECUTE_WITH_RETRY_SPAN_NAME, SpanKind.INTERNAL);
            }
        }

        CompletableFuture<R> getFuture() {
            return promise;
        }

        abstract Status toStatus(R result);

        abstract R toFailedResult(Result<QuerySession> sessionResult);

        // called on timer expiration
        @Override
        public void run() {
            if (promise.isCancelled()) {
                finishOnCancel();
                return;
            }
            executor.execute(this::requestSession);
        }

        public void requestSession() {
            CompletableFuture<Result<QuerySession>> sessionFuture = createSessionWithRetrySpanParent();
            if (sessionFuture.isDone() && !sessionFuture.isCompletedExceptionally()) {
                // faster than subscribing on future
                acceptSession(sessionFuture.join());
            } else {
                sessionFuture.whenCompleteAsync((result, th) -> {
                    if (result != null) {
                        acceptSession(result);
                    }
                    if (th != null) {
                        handleException(th);
                    }
                }, executor);
            }
        }

        private void acceptSession(@Nonnull Result<QuerySession> sessionResult) {
            if (!sessionResult.isSuccess()) {
                handleError(sessionResult.getStatus(), toFailedResult(sessionResult));
                return;
            }

            final QuerySession session = sessionResult.getValue();
            try {
                try (@SuppressWarnings("unused") Scope ignored = trySpan.makeCurrent()) {
                    fn.apply(session).whenComplete((fnResult, fnException) -> {
                        try {
                            try (@SuppressWarnings("unused") Scope ignored1 = trySpan.makeCurrent()) {
                                session.close();

                                if (fnException != null) {
                                    handleException(fnException);
                                    return;
                                }

                                Status status = toStatus(fnResult);
                                if (status.isSuccess()) {
                                    if (promise.complete(fnResult)) {
                                        finishSpans(status, null);
                                    } else if (promise.isCancelled()) {
                                        finishOnCancel();
                                    }
                                } else {
                                    handleError(status, fnResult);
                                }
                            }
                        } catch (Throwable unexpected) {
                            finishSpans(null, unexpected);
                            promise.completeExceptionally(unexpected);
                        }
                    });
                }
            } catch (RuntimeException ex) {
                session.close();
                handleException(ex);
            }
        }

        private void scheduleNext(long delayMillis) {
            if (promise.isCancelled()) {
                finishOnCancel();
                return;
            }
            queryClient.getScheduler().schedule(this, delayMillis, TimeUnit.MILLISECONDS);
        }

        private void finishOnCancel() {
            finishSpans(null, new CancellationException("RunWithRetry was cancelled"));
        }

        private void handleError(@Nonnull Status status, R result) {
            // Check retrayable status
            if (!canRetry(status.getCode())) {
                finishSpans(status, null);
                promise.complete(result);
                return;
            }

            int retry = retryNumber.incrementAndGet();
            if (retry <= maxRetries) {
                long next = backoffTimeMillis(status.getCode(), retry);
                finishTrySpan(status, null);
                startNextRetrySpan(next);
                scheduleNext(next);
            } else {
                finishSpans(status, null);
                promise.complete(result);
            }
        }

        private void handleException(@Nonnull Throwable ex) {
            // Check retrayable execption
            if (!canRetry(ex)) {
                finishSpans(null, ex);
                promise.completeExceptionally(ex);
                return;
            }

            int retry = retryNumber.incrementAndGet();
            if (retry <= maxRetries) {
                long next = backoffTimeMillis(ex, retry);
                finishTrySpan(null, ex);
                startNextRetrySpan(next);
                scheduleNext(next);
            } else {
                finishSpans(null, ex);
                promise.completeExceptionally(ex);
            }
        }

        private void startNextRetrySpan(long backoffMs) {
            try (@SuppressWarnings("unused") Scope ignored = executeSpan.makeCurrent()) {
                trySpan = tracer.startSpan(EXECUTE_WITH_RETRY_SPAN_NAME, SpanKind.INTERNAL);
            }
            trySpan.setAttribute(RETRY_BACKOFF_MS_ATTR, backoffMs);
        }

        private CompletableFuture<Result<QuerySession>> createSessionWithRetrySpanParent() {
            try (@SuppressWarnings("unused") Scope ignored = trySpan.makeCurrent()) {
                return queryClient.createSession(sessionCreationTimeout);
            }
        }

        private void finishTrySpan(Status status, Throwable throwable) {
            trySpan.setStatus(status, throwable);
            trySpan.end();
            trySpan = Span.NOOP;
        }

        private void finishSpans(@Nullable Status status, Throwable throwable) {
            Throwable unwrapped = FutureTools.unwrapCompletionException(throwable);
            finishTrySpan(status, throwable);
            executeSpan.setStatus(status, unwrapped);
            executeSpan.end();
        }
    }

    /**
     * RETRYABLE RESULT TASK
     */
    private final class RetryableResultTask<T> extends BaseRetryableTask<Result<T>> {
        RetryableResultTask(Function<QuerySession, CompletableFuture<Result<T>>> fn) {
            super(fn);
        }

        @Override
        Status toStatus(Result<T> result) {
            return result.getStatus();
        }

        @Override
        Result<T> toFailedResult(Result<QuerySession> sessionResult) {
            return sessionResult.map(s -> null);
        }
    }

    /**
     * RETRYABLE STATUS TASK
     */
    private final class RetryableStatusTask extends BaseRetryableTask<Status> {
        RetryableStatusTask(Function<QuerySession, CompletableFuture<Status>> fn) {
            super(fn);
        }

        @Override
        Status toStatus(Status status) {
            return status;
        }

        @Override
        Status toFailedResult(Result<QuerySession> sessionResult) {
            return sessionResult.getStatus();
        }
    }

    /**
     * BUILDER
     */
    @ParametersAreNonnullByDefault
    public static final class Builder {
        private final QueryClient queryClient;
        private Executor executor = MoreExecutors.directExecutor();
        private Duration sessionCreationTimeout = Duration.ofSeconds(5);
        private int maxRetries = 10;
        private long backoffSlotMillis = 500;
        private int backoffCeiling = 6;
        private long fastBackoffSlotMillis = 5;
        private int fastBackoffCeiling = 10;
        private boolean retryNotFound = true;
        private boolean idempotent = false;

        public Builder(QueryClient queryClient) {
            this.queryClient = queryClient;
        }

        public Builder executor(Executor executor) {
            this.executor = Objects.requireNonNull(executor);
            return this;
        }

        public Builder sessionCreationTimeout(Duration duration) {
            this.sessionCreationTimeout = duration;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder backoffSlot(Duration duration) {
            Preconditions.checkArgument(!duration.isNegative(), "backoffSlot(%s) is negative", duration);
            this.backoffSlotMillis = duration.toMillis();
            return this;
        }

        public Builder backoffCeiling(int backoffCeiling) {
            this.backoffCeiling = backoffCeiling;
            return this;
        }

        public Builder fastBackoffSlot(Duration duration) {
            Preconditions.checkArgument(!duration.isNegative(), "backoffSlot(%s) is negative", duration);
            this.fastBackoffSlotMillis = duration.toMillis();
            return this;
        }

        public Builder fastBackoffCeiling(int backoffCeiling) {
            this.fastBackoffCeiling = backoffCeiling;
            return this;
        }

        public Builder retryNotFound(boolean retryNotFound) {
            this.retryNotFound = retryNotFound;
            return this;
        }

        public Builder idempotent(boolean idempotent) {
            this.idempotent = idempotent;
            return this;
        }

        public SessionRetryContext build() {
            return new SessionRetryContext(this);
        }
    }
}
