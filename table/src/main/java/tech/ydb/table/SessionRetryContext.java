package tech.ydb.table;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.netty.shaded.io.netty.util.Timeout;
import io.grpc.netty.shaded.io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.utils.Async;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class SessionRetryContext {
    private static final Logger logger = LoggerFactory.getLogger(SessionRetryContext.class);

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
            StatusCode statusCode = ((UnexpectedResultException) cause).getStatus().getCode();
            return canRetry(statusCode);
        }
        return false;
    }

    private String errorMsg(Throwable t) {
        if (!logger.isDebugEnabled()) {
            return "unknown";
        }
        Throwable cause = Async.unwrapCompletionException(t);
        if (cause instanceof UnexpectedResultException) {
            StatusCode statusCode = ((UnexpectedResultException) cause).getStatus().getCode();
            return statusCode.name();
        }
        return t.getMessage();
    }

    private boolean canRetry(StatusCode code) {
        if (RETRYABLE_STATUSES.contains(code)) {
            return true;
        }
        switch (code) {
            case NOT_FOUND:
                return retryNotFound;
            case CLIENT_CANCELLED:
            case CLIENT_INTERNAL_ERROR:
            case UNDETERMINED:
            case TRANSPORT_UNAVAILABLE:
                return idempotent;
            default:
                break;
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
    private abstract class BaseRetryableTask<R> implements TimerTask, BiConsumer<Result<Session>, Throwable> {
        private final CompletableFuture<R> promise = new CompletableFuture<>();
        private final AtomicInteger retryNumber = new AtomicInteger();
        private final Function<Session, CompletableFuture<R>> fn;
        private final long createTimestamp = Instant.now().toEpochMilli();

        BaseRetryableTask(Function<Session, CompletableFuture<R>> fn) {
            this.fn = fn;
        }

        CompletableFuture<R> getFuture() {
            return promise;
        }

        abstract StatusCode toStatusCode(R result);
        abstract R toFailedResult(Result<Session> sessionResult);

        private long ms() {
            return Instant.now().toEpochMilli() - createTimestamp;
        }

        // called on timer expiration
        @Override
        public void run(Timeout timeout) {
            if (promise.isCancelled()) {
                logger.debug("RetryCtx[{}] cancelled, {} retries, {} ms", hashCode(), retryNumber.get(), ms());
                return;
            }
            // call run() method outside of the timer thread
            executor.execute(this::run);
        }

        public void run() {
            CompletableFuture<Result<Session>> sessionFuture = sessionSupplier.createSession(sessionCreationTimeout);
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
                handleException(sessionException);
                return;
            }

            if (!sessionResult.isSuccess()) {
                handleError(sessionResult.getStatus().getCode(), toFailedResult(sessionResult));
                return;
            }

            final Session session = sessionResult.getValue();
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
                            logger.debug("RetryCtx[{}] OK, finished after {} retries, {} ms total",
                                    hashCode(), retryNumber.get(), ms());
                            promise.complete(fnResult);
                        } else {
                            handleError(statusCode, fnResult);
                        }
                    } catch (Throwable unexpected) {
                        logger.debug("RetryCtx[{}] UNEXPECTED[{}], finished after {} retries, {} ms total",
                                hashCode(), unexpected.getMessage(), retryNumber.get(), ms());
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

        private void handleError(@Nonnull StatusCode code, R result) {
            // Check retrayable status
            if (!canRetry(code)) {
                logger.debug("RetryCtx[{}] NON-RETRYABLE CODE[{}], finished after {} retries, {} ms total",
                        hashCode(), code, retryNumber.get(), ms());
                promise.complete(result);
                return;
            }

            int retry = retryNumber.incrementAndGet();
            if (retry <= maxRetries) {
                long next = backoffTimeMillis(code, retry);
                logger.debug("RetryCtx[{}] RETRYABLE CODE[{}], scheduling next retry #{} in {} ms, {} ms total",
                        hashCode(), code, retry, next, ms());
                scheduleNext(next);
            } else {
                logger.debug("RetryCtx[{}] RETRYABLE CODE[{}], finished by retries limit ({}), {} ms total",
                        hashCode(), code, maxRetries, ms());
                promise.complete(result);
            }
        }

        private void handleException(@Nonnull Throwable ex) {
            // Check retrayable execption
            if (!canRetry(ex)) {
                logger.debug("RetryCtx[{}] NON-RETRYABLE ERROR[{}], finished after {} retries, {} ms total",
                        hashCode(), errorMsg(ex), retryNumber.get(), ms());
                promise.completeExceptionally(ex);
                return;
            }

            int retry = retryNumber.incrementAndGet();
            if (retry <= maxRetries) {
                long next = backoffTimeMillis(ex, retry);
                logger.debug("RetryCtx[{}] RETRYABLE ERROR[{}], scheduling next retry #{} in {} ms, {} ms total",
                        hashCode(), errorMsg(ex), retry, next, ms());
                scheduleNext(next);
            } else {
                logger.debug("RetryCtx[{}] RETRYABLE ERROR[{}], finished by retries limit ({}), {} ms total",
                        hashCode(), errorMsg(ex), maxRetries, ms());
                promise.completeExceptionally(ex);
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
            return result.getStatus().getCode();
        }

        @Override
        Result<T> toFailedResult(Result<Session> sessionResult) {
            return sessionResult.map(null);
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
        private long backoffSlotMillis = 1000;
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
