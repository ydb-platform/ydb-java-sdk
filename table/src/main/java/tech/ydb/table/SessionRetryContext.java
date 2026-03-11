package tech.ydb.table;

import java.time.Duration;
import java.time.Instant;
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
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.tracing.Span;
import tech.ydb.core.tracing.SpanKind;
import tech.ydb.core.tracing.SpanScope;
import tech.ydb.core.tracing.Tracer;
import tech.ydb.core.utils.FutureTools;
import tech.ydb.proto.ValueProtos;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.description.TableOptionDescription;
import tech.ydb.table.query.BulkUpsertData;
import tech.ydb.table.query.DataQuery;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.ExplainDataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.query.ReadRowsResult;
import tech.ydb.table.query.ReadTablePart;
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.BeginTxSettings;
import tech.ydb.table.settings.BulkUpsertSettings;
import tech.ydb.table.settings.CommitTxSettings;
import tech.ydb.table.settings.CopyTableSettings;
import tech.ydb.table.settings.CopyTablesSettings;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.settings.DescribeTableOptionsSettings;
import tech.ydb.table.settings.DescribeTableSettings;
import tech.ydb.table.settings.DropTableSettings;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.settings.ExecuteScanQuerySettings;
import tech.ydb.table.settings.ExecuteSchemeQuerySettings;
import tech.ydb.table.settings.ExplainDataQuerySettings;
import tech.ydb.table.settings.KeepAliveSessionSettings;
import tech.ydb.table.settings.PrepareDataQuerySettings;
import tech.ydb.table.settings.ReadRowsSettings;
import tech.ydb.table.settings.ReadTableSettings;
import tech.ydb.table.settings.RenameTablesSettings;
import tech.ydb.table.settings.RollbackTxSettings;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.table.transaction.Transaction;
import tech.ydb.table.transaction.TxControl;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class SessionRetryContext {
    private static final String EXECUTE_WITH_RETRY_SPAN_NAME = "ydb.ExecuteWithRetry";
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
        private final AtomicBoolean spanFinished = new AtomicBoolean(true);
        private final AtomicInteger retryNumber = new AtomicInteger();
        private final Function<Session, CompletableFuture<R>> fn;
        private final long createTimestamp = Instant.now().toEpochMilli();
        private final SessionRetryHandler handler;
        private final Tracer tracer;
        private final Span parentSpan;
        private Span retrySpan = Span.NOOP;

        BaseRetryableTask(SessionRetryHandler h, Function<Session, CompletableFuture<R>> fn) {
            this.fn = fn;
            this.handler = h;
            this.tracer = sessionSupplier.getTracer();
            this.parentSpan = tracer.currentSpan();
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
            startRetrySpan();
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
            final Session tracedSession = retrySpan.isValid()
                    ? new TracedSession(session, retrySpan) : session;
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
                            handler.onSuccess(SessionRetryContext.this, retryNumber.get(), ms());
                            promise.complete(fnResult);
                            finishRetrySpan(status, null);
                        } else {
                            handleError(status, fnResult);
                        }
                    } catch (Throwable unexpected) {
                        handler.onError(SessionRetryContext.this, unexpected, retryNumber.get(), ms());
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
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return sessionSupplier.createSession(sessionCreationTimeout);
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

    /**
     * Wraps Session to propagate retry span as parent for all RPC spans within a retry attempt.
     */
    private static final class TracedSession implements Session {
        private final Session delegate;
        private final Span retrySpan;

        TracedSession(Session delegate, Span retrySpan) {
            this.delegate = delegate;
            this.retrySpan = retrySpan;
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public CompletableFuture<Status> createTable(String path, TableDescription tableDescriptions,
                CreateTableSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.createTable(path, tableDescriptions, settings);
            }
        }

        @Override
        public CompletableFuture<Status> dropTable(String path, DropTableSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.dropTable(path, settings);
            }
        }

        @Override
        public CompletableFuture<Status> alterTable(String path, AlterTableSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.alterTable(path, settings);
            }
        }

        @Override
        public CompletableFuture<Status> copyTable(String src, String dst, CopyTableSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.copyTable(src, dst, settings);
            }
        }

        @Override
        public CompletableFuture<Status> copyTables(CopyTablesSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.copyTables(settings);
            }
        }

        @Override
        public CompletableFuture<Status> renameTables(RenameTablesSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.renameTables(settings);
            }
        }

        @Override
        public CompletableFuture<Result<TableDescription>> describeTable(String path,
                DescribeTableSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.describeTable(path, settings);
            }
        }

        @Override
        public CompletableFuture<Result<DataQuery>> prepareDataQuery(String query,
                PrepareDataQuerySettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.prepareDataQuery(query, settings);
            }
        }

        @Override
        public CompletableFuture<Result<DataQueryResult>> executeDataQuery(String query, TxControl<?> txControl,
                Params params, ExecuteDataQuerySettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.executeDataQuery(query, txControl, params, settings);
            }
        }

        @Override
        public CompletableFuture<Result<ReadRowsResult>> readRows(String pathToTable, ReadRowsSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.readRows(pathToTable, settings);
            }
        }

        @Override
        public CompletableFuture<Status> executeSchemeQuery(String query, ExecuteSchemeQuerySettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.executeSchemeQuery(query, settings);
            }
        }

        @Override
        public CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(String query,
                ExplainDataQuerySettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.explainDataQuery(query, settings);
            }
        }

        @Override
        public CompletableFuture<Result<TableOptionDescription>> describeTableOptions(
                DescribeTableOptionsSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.describeTableOptions(settings);
            }
        }

        @Override
        public CompletableFuture<Result<Transaction>> beginTransaction(Transaction.Mode transactionMode,
                BeginTxSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.beginTransaction(transactionMode, settings)
                        .thenApply(r -> r.map(tx -> new TracedTransaction(tx, retrySpan)));
            }
        }

        @Override
        public TableTransaction createNewTransaction(TxMode txMode) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return new TracedTableTransaction(delegate.createNewTransaction(txMode), retrySpan);
            }
        }

        @Override
        public CompletableFuture<Result<TableTransaction>> beginTransaction(TxMode txMode,
                BeginTxSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.beginTransaction(txMode, settings)
                        .thenApply(r -> r.map(tx -> new TracedTableTransaction(tx, retrySpan)));
            }
        }

        @Override
        public CompletableFuture<Status> commitTransaction(String txId, CommitTxSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.commitTransaction(txId, settings);
            }
        }

        @Override
        public CompletableFuture<Status> rollbackTransaction(String txId, RollbackTxSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.rollbackTransaction(txId, settings);
            }
        }

        @Override
        public GrpcReadStream<ReadTablePart> executeReadTable(String tablePath, ReadTableSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.executeReadTable(tablePath, settings);
            }
        }

        @Override
        public GrpcReadStream<ValueProtos.ResultSet> executeScanQueryRaw(String query, Params params,
                ExecuteScanQuerySettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.executeScanQueryRaw(query, params, settings);
            }
        }

        @Override
        public CompletableFuture<Result<State>> keepAlive(KeepAliveSessionSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.keepAlive(settings);
            }
        }

        @Override
        public CompletableFuture<Status> executeBulkUpsert(String tablePath, BulkUpsertData data,
                BulkUpsertSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.executeBulkUpsert(tablePath, data, settings);
            }
        }
    }

    /**
     * Wraps TableTransaction to propagate retry span as parent for commit/rollback/query spans.
     */
    private static final class TracedTableTransaction implements TableTransaction {
        private final TableTransaction delegate;
        private final Span retrySpan;

        TracedTableTransaction(TableTransaction delegate, Span retrySpan) {
            this.delegate = delegate;
            this.retrySpan = retrySpan;
        }

        @Override
        public Session getSession() {
            return delegate.getSession();
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
        public CompletableFuture<Result<DataQueryResult>> executeDataQuery(String query, boolean commitAtEnd,
                Params params, ExecuteDataQuerySettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.executeDataQuery(query, commitAtEnd, params, settings);
            }
        }

        @Override
        public CompletableFuture<Status> commit(CommitTxSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.commit(settings);
            }
        }

        @Override
        public CompletableFuture<Status> rollback(RollbackTxSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.rollback(settings);
            }
        }
    }

    /**
     * Wraps legacy Transaction to propagate retry span as parent for commit/rollback spans.
     */
    private static final class TracedTransaction implements Transaction {
        private final Transaction delegate;
        private final Span retrySpan;

        TracedTransaction(Transaction delegate, Span retrySpan) {
            this.delegate = delegate;
            this.retrySpan = retrySpan;
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public CompletableFuture<Status> commit(CommitTxSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.commit(settings);
            }
        }

        @Override
        public CompletableFuture<Status> rollback(RollbackTxSettings settings) {
            try (SpanScope ignored = retrySpan.makeCurrent()) {
                return delegate.rollback(settings);
            }
        }
    }
}
