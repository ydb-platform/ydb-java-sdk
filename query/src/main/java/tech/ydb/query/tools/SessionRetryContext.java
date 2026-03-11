package tech.ydb.query.tools;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.tracing.Span;
import tech.ydb.core.tracing.SpanKind;
import tech.ydb.core.tracing.SpanScope;
import tech.ydb.core.tracing.Tracer;
import tech.ydb.core.utils.FutureTools;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryStream;
import tech.ydb.query.QueryTransaction;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.settings.BeginTransactionSettings;
import tech.ydb.query.settings.CommitTransactionSettings;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.settings.RollbackTransactionSettings;
import tech.ydb.table.query.Params;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class SessionRetryContext {
    private static final String EXECUTE_WITH_RETRY_SPAN_NAME = "ydb.ExecuteWithRetry";
    private static final String RETRY_ATTEMPT_ATTR = "ydb.retry.attempt";
    private static final String RETRY_SLEEP_MS_ATTR = "ydb.retry.sleep_ms";

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
        private final AtomicBoolean spanFinished = new AtomicBoolean(true);
        private final AtomicInteger retryNumber = new AtomicInteger();
        private final Function<QuerySession, CompletableFuture<R>> fn;
        private final Tracer tracer;
        private final Span parentSpan;
        private Span retrySpan = Span.NOOP;

        BaseRetryableTask(Function<QuerySession, CompletableFuture<R>> fn) {
            this.fn = fn;
            this.tracer = queryClient.getTracer();
            this.parentSpan = tracer.currentSpan();
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
                finishRetrySpan(null, null);
                return;
            }
            executor.execute(this::requestSession);
        }

        public void requestSession() {
            startRetrySpan();
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
            final QuerySession tracedSession = retrySpan.isValid()
                    ? new TracedQuerySession(session, retrySpan) : session;
            try {
                fn.apply(tracedSession).whenComplete((fnResult, fnException) -> {
                    try {
                        session.close();

                        if (fnException != null) {
                            handleException(fnException);
                            return;
                        }

                        Status status = toStatus(fnResult);
                        if (status.isSuccess()) {
                            promise.complete(fnResult);
                            finishRetrySpan(status, null);
                        } else {
                            handleError(status, fnResult);
                        }
                    } catch (Throwable unexpected) {
                        finishRetrySpan(null, unexpected);
                        promise.completeExceptionally(unexpected);
                    }
                });
            } catch (RuntimeException ex) {
                session.close();
                handleException(ex);
            }
        }

        private void scheduleNext(long delayMillis) {
            if (promise.isCancelled()) {
                return;
            }
            queryClient.getScheduler().schedule(this, delayMillis, TimeUnit.MILLISECONDS);
        }

        private void handleError(@Nonnull Status status, R result) {
            // Check retrayable status
            if (!canRetry(status.getCode())) {
                finishRetrySpan(status, null);
                promise.complete(result);
                return;
            }

            int failedAttempt = retryNumber.get();
            int retry = retryNumber.incrementAndGet();
            if (retry <= maxRetries) {
                long next = backoffTimeMillis(status.getCode(), retry);
                recordRetrySchedule(failedAttempt, next);
                finishRetrySpan(status, null);
                scheduleNext(next);
            } else {
                finishRetrySpan(status, null);
                promise.complete(result);
            }
        }

        private void handleException(@Nonnull Throwable ex) {
            // Check retrayable execption
            if (!canRetry(ex)) {
                finishRetrySpan(null, ex);
                promise.completeExceptionally(ex);
                return;
            }

            int failedAttempt = retryNumber.get();
            int retry = retryNumber.incrementAndGet();
            if (retry <= maxRetries) {
                long next = backoffTimeMillis(ex, retry);
                recordRetrySchedule(failedAttempt, next);
                finishRetrySpan(null, ex);
                scheduleNext(next);
            } else {
                finishRetrySpan(null, ex);
                promise.completeExceptionally(ex);
            }
        }

        private CompletableFuture<Result<QuerySession>> createSessionWithRetrySpanParent() {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return queryClient.createSession(sessionCreationTimeout);
            }
        }

        private void startRetrySpan() {
            if (!spanFinished.get()) {
                return;
            }
            try (SpanScope ignored = parentSpan.makeCurrent()) {
                retrySpan = tracer.startSpan(EXECUTE_WITH_RETRY_SPAN_NAME, SpanKind.INTERNAL);
            }
            spanFinished.set(false);
            retrySpan.setAttribute(RETRY_ATTEMPT_ATTR, retryNumber.get());
        }

        private void recordRetrySchedule(int failedAttempt, long nextDelayMillis) {
            retrySpan.setAttribute(RETRY_ATTEMPT_ATTR, failedAttempt);
            retrySpan.setAttribute(RETRY_SLEEP_MS_ATTR, nextDelayMillis);
        }

        private void finishRetrySpan(Status status, Throwable throwable) {
            if (!spanFinished.compareAndSet(false, true)) {
                return;
            }

            retrySpan.setStatus(status, throwable);
            retrySpan.end();
            retrySpan = Span.NOOP;
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

    /**
     * Wraps QuerySession to propagate retry span as parent for all RPC spans within a retry attempt.
     */
    private static final class TracedQuerySession implements QuerySession {
        private final QuerySession delegate;
        private final Span retrySpan;

        TracedQuerySession(QuerySession delegate, Span retrySpan) {
            this.delegate = delegate;
            this.retrySpan = retrySpan;
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public QueryTransaction currentTransaction() {
            QueryTransaction tx = delegate.currentTransaction();
            return tx != null ? new TracedQueryTransaction(tx, retrySpan) : null;
        }

        @Override
        public QueryTransaction createNewTransaction(TxMode txMode) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return new TracedQueryTransaction(delegate.createNewTransaction(txMode), retrySpan);
            }
        }

        @Override
        public CompletableFuture<Result<QueryTransaction>> beginTransaction(
                TxMode txMode, BeginTransactionSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.beginTransaction(txMode, settings)
                        .thenApply(r -> r.map(tx -> new TracedQueryTransaction(tx, retrySpan)));
            }
        }

        @Override
        public QueryStream createQuery(String query, TxMode tx, Params params, ExecuteQuerySettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.createQuery(query, tx, params, settings);
            }
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    /**
     * Wraps QueryTransaction to propagate retry span as parent for commit/rollback/query spans.
     */
    private static final class TracedQueryTransaction implements QueryTransaction {
        private final QueryTransaction delegate;
        private final Span retrySpan;

        TracedQueryTransaction(QueryTransaction delegate, Span retrySpan) {
            this.delegate = delegate;
            this.retrySpan = retrySpan;
        }

        @Override
        public QuerySession getSession() {
            return new TracedQuerySession(delegate.getSession(), retrySpan);
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public TxMode getTxMode() {
            return delegate.getTxMode();
        }

        @Override
        public String getSessionId() {
            return delegate.getSessionId();
        }

        @Override
        public CompletableFuture<Status> getStatusFuture() {
            return delegate.getStatusFuture();
        }

        @Override
        public CompletableFuture<Result<QueryInfo>> commit(CommitTransactionSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.commit(settings);
            }
        }

        @Override
        public CompletableFuture<Status> rollback(RollbackTransactionSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.rollback(settings);
            }
        }

        @Override
        public QueryStream createQuery(String query, boolean commitAtEnd, Params params,
                ExecuteQuerySettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.createQuery(query, commitAtEnd, params, settings);
            }
        }
    }
}
