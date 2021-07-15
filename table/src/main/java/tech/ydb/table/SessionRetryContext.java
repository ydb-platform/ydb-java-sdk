package tech.ydb.table;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.util.concurrent.MoreExecutors;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.utils.Async;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class SessionRetryContext {

    private static final EnumSet<StatusCode> RETRYABLE_STATUSES = EnumSet.of(
        StatusCode.ABORTED,
        StatusCode.UNAVAILABLE,
        StatusCode.OVERLOADED,
        StatusCode.CLIENT_RESOURCE_EXHAUSTED,
        StatusCode.BAD_SESSION,
        StatusCode.SESSION_BUSY
    );

    private final SessionSupplier sessionSupplier;
    private final Executor executor;
    private final int maxRetries;
    private final long backoffSlotMillis;
    private final int backoffCeiling;
    private final long fastBackoffSlotMillis;
    private final int fastBackoffCeiling;
    private final Duration sessionSupplyTimeout;
    private final boolean retryNotFound;

    private SessionRetryContext(Builder b) {
        this.sessionSupplier = b.sessionSupplier;
        this.executor = b.executor;
        this.maxRetries = b.maxRetries;
        this.backoffSlotMillis = b.backoffSlotMillis;
        this.backoffCeiling = b.backoffCeiling;
        this.fastBackoffSlotMillis = b.fastBackoffSlotMillis;
        this.fastBackoffCeiling = b.fastBackoffCeiling;
        this.sessionSupplyTimeout = b.sessionSupplyTimeout;
        this.retryNotFound = b.retryNotFound;
    }

    public static Builder create(SessionSupplier sessionSupplier) {
        return new Builder(Objects.requireNonNull(sessionSupplier));
    }

    public <T> CompletableFuture<Result<T>> supplyResult(Function<Session, CompletableFuture<Result<T>>> fn) {
        RetryableResultTask<T> task = new RetryableResultTask<>(fn);
        task.run();
        return task.getFuture();
    }

    public CompletableFuture<Status> supplyStatus(Function<Session, CompletableFuture<Status>> fn) {
        RetryableStatusTask task = new RetryableStatusTask(fn);
        task.run();
        return task.getFuture();
    }

    private boolean canRetry(Throwable t) {
        Throwable cause = Async.unwrapCompletionException(t);
        if (cause instanceof UnexpectedResultException) {
            StatusCode statusCode = ((UnexpectedResultException) cause).getStatusCode();
            return canRetry(statusCode);
        }
        return false;
    }

    private boolean canRetry(StatusCode code) {
        if (RETRYABLE_STATUSES.contains(code)) {
            return true;
        }
        if (code == StatusCode.NOT_FOUND && retryNotFound) {
            return true;
        }
        return false;
    }

    private long backoffTimeMillisInternal(int retryNumber, long backoffSlotMillis, int backoffCeiling) {
        int slots = 1 << Math.min(retryNumber, backoffCeiling);
        long maxDurationMillis = backoffSlotMillis * slots;
        return backoffSlotMillis + ThreadLocalRandom.current().nextLong(maxDurationMillis);
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
            case SESSION_BUSY:
                // Instant retry
                return 0;
            case ABORTED:
            case UNAVAILABLE:
                // Fast backoff
                return fastBackoffTimeMillis(retryNumber);
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
            StatusCode statusCode = ((UnexpectedResultException) cause).getStatusCode();
            return backoffTimeMillis(statusCode, retryNumber);
        }
        return slowBackoffTimeMillis(retryNumber);
    }

    /**
     * BASE RETRYABLE TASK
     */
    private abstract class BaseRetryableTask<R> implements TimerTask, BiConsumer<Result<Session>, Throwable> {
        private final CompletableFuture<R> promise = new CompletableFuture<>();
        private final AtomicInteger retryNumber = new AtomicInteger();
        private final Function<Session, CompletableFuture<R>> fn;

        BaseRetryableTask(Function<Session, CompletableFuture<R>> fn) {
            this.fn = fn;
        }

        CompletableFuture<R> getFuture() {
            return promise;
        }

        abstract StatusCode toStatusCode(R result);
        abstract R toFailedResult(Result<Session> sessionResult);

        // called on timer expiration
        @Override
        public void run(Timeout timeout) {
            if (promise.isCancelled()) {
                return;
            }
            retryNumber.incrementAndGet();
            // call run() method outside of the timer thread
            executor.execute(this::run);
        }

        public void run() {
            CompletableFuture<Result<Session>> sessionFuture = sessionSupplier.getOrCreateSession(sessionSupplyTimeout);
            if (sessionFuture.isDone() && !sessionFuture.isCompletedExceptionally()) {
                // faster than subscribing on future
                accept(sessionFuture.getNow(null), null);
            } else {
                sessionFuture.whenCompleteAsync(this, executor);
            }
        }

        // called on session acquiring
        @Override
        public void accept(Result<Session> sessionResult, Throwable sessionException) {
            assert (sessionResult == null) != (sessionException == null);

            if (sessionException != null) {
                retryIfPossible(null, null, sessionException);
                return;
            }

            if (!sessionResult.isSuccess()) {
                retryIfPossible(sessionResult.getCode(), toFailedResult(sessionResult), null);
                return;
            }

            final Session session = sessionResult.expect("session must present");
            Async.safeCall(session, fn)
                .whenComplete((fnResult, fnException) -> {
                    try {
                        session.release();

                        if (fnException != null) {
                            retryIfPossible(null, null, fnException);
                            return;
                        }

                        StatusCode statusCode = toStatusCode(fnResult);
                        if (statusCode == StatusCode.SUCCESS) {
                            promise.complete(fnResult);
                        } else {
                            retryIfPossible(statusCode, fnResult, null);
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
            Async.runAfter(this, delayMillis, TimeUnit.MILLISECONDS);
        }

        private void retryIfPossible(@Nullable StatusCode code, @Nullable R result, @Nullable Throwable ex) {
            assert (result == null) != (ex == null);
            assert (code == null) == (result == null);

            int retry = retryNumber.incrementAndGet();

            if (ex != null) {
                if (retry <= maxRetries && canRetry(ex)) {
                    scheduleNext(backoffTimeMillis(ex, retry));
                } else {
                    promise.completeExceptionally(ex);
                }
            } else {
                if (retry <= maxRetries && canRetry(code)) {
                    scheduleNext(backoffTimeMillis(code, retry));
                } else {
                    promise.complete(result);
                }
            }
        }
    }

    /**
     * RETRYABLE RESULT TASK
     */
    private final class RetryableResultTask<T> extends BaseRetryableTask<Result<T>> {
        RetryableResultTask(Function<Session, CompletableFuture<Result<T>>> fn) {
            super(fn);
        }

        @Override
        StatusCode toStatusCode(Result<T> result) {
            return result.getCode();
        }

        @Override
        Result<T> toFailedResult(Result<Session> sessionResult) {
            return sessionResult.cast();
        }
    }

    /**
     * RETRYABLE STATUS TASK
     */
    private final class RetryableStatusTask extends BaseRetryableTask<Status> {
        RetryableStatusTask(Function<Session, CompletableFuture<Status>> fn) {
            super(fn);
        }

        @Override
        StatusCode toStatusCode(Status status) {
            return status.getCode();
        }

        @Override
        Status toFailedResult(Result<Session> sessionResult) {
            return sessionResult.toStatus();
        }
    }

    /**
     * BUILDER
     */
    @ParametersAreNonnullByDefault
    public static final class Builder {
        private final SessionSupplier sessionSupplier;
        private Executor executor = MoreExecutors.directExecutor();
        private int maxRetries = 10;
        private long backoffSlotMillis = 1000;
        private int backoffCeiling = 6;
        private long fastBackoffSlotMillis = 5;
        private int fastBackoffCeiling = 10;
        private Duration sessionSupplyTimeout = Duration.ofSeconds(5);
        private boolean retryNotFound = true;

        public Builder(SessionSupplier sessionSupplier) {
            this.sessionSupplier = sessionSupplier;
        }

        public Builder executor(Executor executor) {
            this.executor = Objects.requireNonNull(executor);
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder backoffSlot(Duration duration) {
            checkArgument(!duration.isNegative(), "backoffSlot(%s) is negative", duration);
            this.backoffSlotMillis = duration.toMillis();
            return this;
        }

        public Builder backoffCeiling(int backoffCeiling) {
            this.backoffCeiling = backoffCeiling;
            return this;
        }

        public Builder fastBackoffSlot(Duration duration) {
            checkArgument(!duration.isNegative(), "backoffSlot(%s) is negative", duration);
            this.fastBackoffSlotMillis = duration.toMillis();
            return this;
        }

        public Builder fastBackoffCeiling(int backoffCeiling) {
            this.fastBackoffCeiling = backoffCeiling;
            return this;
        }

        public Builder sessionSupplyTimeout(Duration duration) {
            checkArgument(!duration.isNegative(), "sessionSupplyTimeout(%s) is negative", duration);
            this.sessionSupplyTimeout = duration;
            return this;
        }

        public Builder retryNotFound(boolean retryNotFound) {
            this.retryNotFound = retryNotFound;
            return this;
        }

        public SessionRetryContext build() {
            return new SessionRetryContext(this);
        }
    }
}
