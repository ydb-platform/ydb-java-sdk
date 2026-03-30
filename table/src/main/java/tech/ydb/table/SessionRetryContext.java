package tech.ydb.table;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.annotation.Nonnull;
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


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class SessionRetryContext {
    private static final String EXECUTE_SPAN_NAME = "ydb.Execute";
    private static final String EXECUTE_WITH_RETRY_SPAN_NAME = "ydb.Retry";
    private static final String RETRY_ATTEMPT_ATTR = "ydb.retry.attempt";
    private static final String RETRY_SLEEP_MS_ATTR = "ydb.retry.sleep_ms";

    private final SessionSupplier sessionSupplier;
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
        this.sessionSupplier = b.sessionSupplier;
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

    public static Builder create(SessionSupplier sessionSupplier) {
        return new Builder(Objects.requireNonNull(sessionSupplier));
    }

    public <T> CompletableFuture<Result<T>> supplyResult(Function<Session, CompletableFuture<Result<T>>> fn) {
        return supplyResult(SessionRetryHandler.DEFAULT, fn);
    }

    public CompletableFuture<Status> supplyStatus(Function<Session, CompletableFuture<Status>> fn) {
        return supplyStatus(SessionRetryHandler.DEFAULT, fn);
    }

    public <T> CompletableFuture<Result<T>> supplyResult(SessionRetryHandler h,
                                                         Function<Session, CompletableFuture<Result<T>>> fn) {
        RetryableResultTask<T> task = new RetryableResultTask<>(h, fn);
        task.requestSession();
        return task.getFuture();
    }

    public CompletableFuture<Status> supplyStatus(SessionRetryHandler h,
                                                  Function<Session, CompletableFuture<Status>> fn) {
        RetryableStatusTask task = new RetryableStatusTask(h, fn);
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
        private final Function<Session, CompletableFuture<R>> fn;
        private final long createTimestamp = Instant.now().toEpochMilli();
        private final SessionRetryHandler handler;
        private final Tracer tracer;
        private final Span executeSpan;
        private Span retrySpan = Span.NOOP;

        BaseRetryableTask(SessionRetryHandler h, Function<Session, CompletableFuture<R>> fn) {
            this.fn = fn;
            this.handler = h;
            this.tracer = sessionSupplier.getTracer();
            this.executeSpan = tracer.startSpan(EXECUTE_SPAN_NAME, SpanKind.INTERNAL);
            this.promise.whenComplete(this::finishExecuteSpan);
        }

        CompletableFuture<R> getFuture() {
            return promise;
        }

        abstract Status toStatus(R result);

        abstract R toFailedResult(Result<Session> sessionResult);

        private long ms() {
            return Instant.now().toEpochMilli() - createTimestamp;
        }

        // called on timer expiration
        @Override
        public void run() {
            if (promise.isCancelled()) {
                handler.onCancel(SessionRetryContext.this, retryNumber.get(), ms());
                finishRetrySpan(null, null);
                return;
            }
            executor.execute(this::requestSession);
        }

        public void requestSession() {
            try (Scope ignored = executeSpan.makeCurrent()) {
                retrySpan = tracer.startSpan(EXECUTE_WITH_RETRY_SPAN_NAME, SpanKind.INTERNAL);
            }
            CompletableFuture<Result<Session>> sessionFuture = createSessionWithRetrySpanParent();
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

        private void acceptSession(@Nonnull Result<Session> sessionResult) {
            if (!sessionResult.isSuccess()) {
                handleError(sessionResult.getStatus(), toFailedResult(sessionResult));
                return;
            }

            final Session session = sessionResult.getValue();
            try {
                try (Scope ignored = retrySpan.makeCurrent()) {
                    fn.apply(session).whenComplete((fnResult, fnException) -> {
                        try {
                            try (Scope ignored1 = retrySpan.makeCurrent()) {
                                session.close();

                                if (fnException != null) {
                                    handleException(fnException);
                                    return;
                                }

                                Status status = toStatus(fnResult);
                                if (status.isSuccess()) {
                                    handler.onSuccess(SessionRetryContext.this, retryNumber.get(), ms());
                                    finishRetrySpan(status, null);
                                    promise.complete(fnResult);
                                } else {
                                    handleError(status, fnResult);
                                }
                            }
                        } catch (Throwable unexpected) {
                            handler.onError(SessionRetryContext.this, unexpected, retryNumber.get(), ms());
                            finishRetrySpan(null, unexpected);
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
                return;
            }
            sessionSupplier.getScheduler().schedule(this, delayMillis, TimeUnit.MILLISECONDS);
        }

        private void handleError(@Nonnull Status status, R result) {
            StatusCode code = status.getCode();
            if (!canRetry(code)) {
                handler.onError(SessionRetryContext.this, code, retryNumber.get(), ms());
                finishRetrySpan(status, null);
                promise.complete(result);
                return;
            }

            int failedAttempt = retryNumber.get();
            int retry = retryNumber.incrementAndGet();
            if (retry <= maxRetries) {
                long next = backoffTimeMillis(code, retry);
                handler.onRetry(SessionRetryContext.this, code, retry, next, ms());
                recordRetrySchedule(failedAttempt, next);
                finishRetrySpan(status, null);
                scheduleNext(next);
            } else {
                handler.onLimit(SessionRetryContext.this, code, maxRetries, ms());
                finishRetrySpan(status, null);
                promise.complete(result);
            }
        }

        private void handleException(@Nonnull Throwable ex) {
            // Check retrayable execption
            if (!canRetry(ex)) {
                handler.onError(SessionRetryContext.this, ex, retryNumber.get(), ms());
                finishRetrySpan(null, ex);
                promise.completeExceptionally(ex);
                return;
            }

            int failedAttempt = retryNumber.get();
            int retry = retryNumber.incrementAndGet();
            if (retry <= maxRetries) {
                long next = backoffTimeMillis(ex, retry);
                handler.onRetry(SessionRetryContext.this, ex, retry, next, ms());
                recordRetrySchedule(failedAttempt, next);
                finishRetrySpan(null, ex);
                scheduleNext(next);
            } else {
                handler.onLimit(SessionRetryContext.this, ex, maxRetries, ms());
                finishRetrySpan(null, ex);
                promise.completeExceptionally(ex);
            }
        }

        private CompletableFuture<Result<Session>> createSessionWithRetrySpanParent() {
            try (Scope ignored = retrySpan.makeCurrent()) {
                return sessionSupplier.createSession(sessionCreationTimeout);
            }
        }

        private void recordRetrySchedule(int failedAttempt, long nextDelayMillis) {
            retrySpan.setAttribute(RETRY_ATTEMPT_ATTR, failedAttempt);
            retrySpan.setAttribute(RETRY_SLEEP_MS_ATTR, nextDelayMillis);
        }

        private void finishRetrySpan(Status status, Throwable throwable) {
            retrySpan.setStatus(status, throwable);
            retrySpan.end();
            retrySpan = Span.NOOP;
        }

        private void finishExecuteSpan(R result, Throwable throwable) {
            Throwable unwrapped = FutureTools.unwrapCompletionException(throwable);
            Status status = toStatus(result);
            executeSpan.setStatus(status, unwrapped);
            executeSpan.end();
        }
    }

    /**
     * RETRYABLE RESULT TASK
     */
    private final class RetryableResultTask<T> extends BaseRetryableTask<Result<T>> {
        RetryableResultTask(SessionRetryHandler h, Function<Session, CompletableFuture<Result<T>>> fn) {
            super(h, fn);
        }

        @Override
        Status toStatus(Result<T> result) {
            return result.getStatus();
        }

        @Override
        Result<T> toFailedResult(Result<Session> sessionResult) {
            return sessionResult.map(s -> null);
        }
    }

    /**
     * RETRYABLE STATUS TASK
     */
    private final class RetryableStatusTask extends BaseRetryableTask<Status> {
        RetryableStatusTask(SessionRetryHandler h, Function<Session, CompletableFuture<Status>> fn) {
            super(h, fn);
        }

        @Override
        Status toStatus(Status status) {
            return status;
        }

        @Override
        Status toFailedResult(Result<Session> sessionResult) {
            return sessionResult.getStatus();
        }
    }

    /**
     * BUILDER
     */
    @ParametersAreNonnullByDefault
    public static final class Builder {
        private final SessionSupplier sessionSupplier;
        private Executor executor = MoreExecutors.directExecutor();
        private Duration sessionCreationTimeout = Duration.ofSeconds(5);
        private int maxRetries = 10;
        private long backoffSlotMillis = 500;
        private int backoffCeiling = 6;
        private long fastBackoffSlotMillis = 5;
        private int fastBackoffCeiling = 10;
        private boolean retryNotFound = true;
        private boolean idempotent = false;

        public Builder(SessionSupplier sessionSupplier) {
            this.sessionSupplier = sessionSupplier;
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
