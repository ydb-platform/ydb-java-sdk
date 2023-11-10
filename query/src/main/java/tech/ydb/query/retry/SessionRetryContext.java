package tech.ydb.query.retry;

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
import tech.ydb.core.utils.Async;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class SessionRetryContext {

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
        Throwable cause = Async.unwrapCompletionException(t);
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
        Throwable cause = Async.unwrapCompletionException(t);
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
        private final long createTimestamp = Instant.now().toEpochMilli();

        BaseRetryableTask(Function<QuerySession, CompletableFuture<R>> fn) {
            this.fn = fn;
        }

        CompletableFuture<R> getFuture() {
            return promise;
        }

        abstract StatusCode toStatusCode(R result);
        abstract R toFailedResult(Result<QuerySession> sessionResult);

        private long ms() {
            return Instant.now().toEpochMilli() - createTimestamp;
        }

        // called on timer expiration
        @Override
        public void run() {
            if (promise.isCancelled()) {
                return;
            }
            executor.execute(this::requestSession);
        }

        public void requestSession() {
            CompletableFuture<Result<QuerySession>> sessionFuture = queryClient.createSession(sessionCreationTimeout);
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
                handleError(sessionResult.getStatus().getCode(), toFailedResult(sessionResult));
                return;
            }

            final QuerySession session = sessionResult.getValue();
            Async.safeCall(session, fn)
                .whenComplete((fnResult, fnException) -> {
                    try {
                        session.close();

                        if (fnException != null) {
                            handleException(fnException);
                            return;
                        }

                        StatusCode statusCode = toStatusCode(fnResult);
                        if (statusCode == StatusCode.SUCCESS) {
                            promise.complete(fnResult);
                        } else {
                            handleError(statusCode, fnResult);
                        }
                    } catch (Throwable unexpected) {
                        promise.completeExceptionally(unexpected);
                    }
                });
        }

        private void scheduleNext(long delayMillis) {
            if (promise.isCancelled()) {
                return;
            }
            queryClient.getScheduler().schedule(this, delayMillis, TimeUnit.MILLISECONDS);
        }

        private void handleError(@Nonnull StatusCode code, R result) {
            // Check retrayable status
            if (!canRetry(code)) {
                promise.complete(result);
                return;
            }

            int retry = retryNumber.incrementAndGet();
            if (retry <= maxRetries) {
                long next = backoffTimeMillis(code, retry);
                scheduleNext(next);
            } else {
                promise.complete(result);
            }
        }

        private void handleException(@Nonnull Throwable ex) {
            // Check retrayable execption
            if (!canRetry(ex)) {
                promise.completeExceptionally(ex);
                return;
            }

            int retry = retryNumber.incrementAndGet();
            if (retry <= maxRetries) {
                long next = backoffTimeMillis(ex, retry);
                scheduleNext(next);
            } else {
                promise.completeExceptionally(ex);
            }
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
        StatusCode toStatusCode(Result<T> result) {
            return result.getStatus().getCode();
        }

        @Override
        Result<T> toFailedResult(Result<QuerySession> sessionResult) {
            return sessionResult.map(null);
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
        StatusCode toStatusCode(Status status) {
            return status.getCode();
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
