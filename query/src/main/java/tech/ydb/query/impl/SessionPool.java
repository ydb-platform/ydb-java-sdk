package tech.ydb.query.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;

import io.grpc.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.tracing.DbSpanFactory;
import tech.ydb.core.utils.FutureTools;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.QuerySession;
import tech.ydb.query.settings.AttachSessionSettings;
import tech.ydb.query.settings.CreateSessionSettings;
import tech.ydb.query.settings.DeleteSessionSettings;
import tech.ydb.table.SessionPoolStats;
import tech.ydb.table.impl.pool.WaitingQueue;


/**
 *
 * @author Aleksandr Gorshenin
 */
class SessionPool implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SessionPool.class);

    private static final CreateSessionSettings CREATE_SETTINGS = CreateSessionSettings.newBuilder()
            .withRequestTimeout(Duration.ofSeconds(300))
            .withOperationTimeout(Duration.ofSeconds(299))
            .build();

    private static final DeleteSessionSettings DELETE_SETTINGS = DeleteSessionSettings.newBuilder()
            .withRequestTimeout(Duration.ofSeconds(5))
            .withOperationTimeout(Duration.ofSeconds(4))
            .build();

    private static final AttachSessionSettings ATTACH_SETTINGS = AttachSessionSettings.newBuilder()
            .build();

    private final int minSize;
    private final Clock clock;
    private final ScheduledExecutorService scheduler;
    private final WaitingQueue<PooledQuerySession> queue;
    private final ScheduledFuture<?> cleanerFuture;
    private final StatsImpl stats = new StatsImpl();

    SessionPool(Clock clock, QueryServiceRpc rpc, ScheduledExecutorService scheduler, int minSize, int maxSize,
            Duration idleDuration) {
        this.minSize = minSize;

        this.clock = clock;
        this.scheduler = scheduler;
        this.queue = new WaitingQueue<>(new Handler(rpc), maxSize);

        CleanerTask cleaner = new CleanerTask(idleDuration);
        this.cleanerFuture = scheduler.scheduleAtFixedRate(
                cleaner,
                cleaner.periodMillis / 2,
                cleaner.periodMillis,
                TimeUnit.MILLISECONDS);
        logger.info("init QuerySession pool, min size = {}, max size = {}, keep alive period = {}",
                minSize,
                maxSize,
                cleaner.periodMillis);
    }

    public void updateMaxSize(int maxSize) {
        this.queue.updateLimits(maxSize);
    }

    SessionPoolStats getStats() {
        return stats;
    }

    @Override
    public void close() {
        logger.info("closing QuerySession pool");
        cleanerFuture.cancel(false);
        queue.close();
    }

    public CompletableFuture<Result<QuerySession>> acquire(Duration timeout) {
        logger.trace("acquire QuerySession with timeout {}", timeout);

        CompletableFuture<Result<QuerySession>> future = new CompletableFuture<>();

        // If next session is not ready - add timeout canceler
        if (!pollNext(future)) {
            future.whenComplete(new Canceller(scheduler.schedule(
                    new Timeout(future),
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS)
            ));
        }

        return future;
    }

    private boolean pollNext(CompletableFuture<Result<QuerySession>> future) {
        CompletableFuture<PooledQuerySession> nextSession = new CompletableFuture<>();
        queue.acquire(nextSession);

        if (nextSession.isDone() && !nextSession.isCompletedExceptionally()) {
            return tryComplete(future, nextSession.join());
        }

        nextSession.whenComplete((session, th) -> {
            if (th != null) {
                if (future.isDone()) {
                    logger.warn("can't get QuerySession, future is already canceled", th);
                    return;
                }

                Throwable ex = FutureTools.unwrapCompletionException(th);
                if (ex instanceof UnexpectedResultException) {
                    future.complete(Result.fail((UnexpectedResultException) ex));
                } else {
                    future.complete(Result.error("can't create QuerySession", ex));
                }
            }
            if (session != null) {
                tryComplete(future, session);
            }
        });
        return false;
    }

    private boolean tryComplete(CompletableFuture<Result<QuerySession>> future, PooledQuerySession session) {
        logger.trace("QuerySession[{}] tries to complete acquire", session.getId());
        if (!future.complete(Result.success(session))) {
            logger.debug("QuerySession[{}] future already done, return session to the pool", session.getId());
            queue.release(session);
            return false;
        }

        stats.acquired.increment();
        return true;
    }

    private class PooledQuerySession extends SessionImpl {
        private final GrpcReadStream<Status> attachStream;
        private final DbSpanFactory.OperationSpan createSpan;

        private volatile Instant lastActive;
        private volatile boolean isStarted = false;
        private volatile boolean isBroken = false;
        private volatile boolean isStopped = false;

        PooledQuerySession(
                QueryServiceRpc rpc,
                YdbQuery.CreateSessionResponse response,
                DbSpanFactory.OperationSpan createSpan
        ) {
            super(rpc, response);
            this.lastActive = clock.instant();
            this.attachStream = attach(ATTACH_SETTINGS);
            this.createSpan = createSpan;
            stats.created.increment();
        }

        @Override
        public void updateSessionState(Status status) {
            this.lastActive = clock.instant();
            boolean isStatusBroken =
                    status.getCode() == StatusCode.BAD_SESSION ||
                    status.getCode() == StatusCode.SESSION_BUSY ||
                    status.getCode() == StatusCode.INTERNAL_ERROR ||
                    status.getCode() == StatusCode.CLIENT_DEADLINE_EXCEEDED ||
                    status.getCode() == StatusCode.CLIENT_DEADLINE_EXPIRED ||
                    status.getCode() == StatusCode.CLIENT_CANCELLED ||
                    status.getCode() == StatusCode.TRANSPORT_UNAVAILABLE;
            if (isStatusBroken) {
                logger.warn("QuerySession[{}] broken with status {}", getId(), status);
            }
            isBroken = isBroken || isStatusBroken;
        }

        public Instant getLastActive() {
            return lastActive;
        }

        public CompletableFuture<Result<PooledQuerySession>> start() {
            final CompletableFuture<Result<PooledQuerySession>> future = new CompletableFuture<>();
            final Result<PooledQuerySession> ok = Result.success(this);

            this.attachStream.start(status -> {
                if (!status.isSuccess()) {
                    logger.warn("QuerySession[{}] attach message {}", getId(), status);
                    future.complete(Result.fail(status));
                    createSpan.finishStatus(status);
                    clean();
                    return;
                }

                if (future.complete(ok)) {
                    logger.debug("QuerySession[{}] attach message {}", getId(), status);
                    isStarted = true;
                    createSpan.finishStatus(status);
                    return;
                }

                logger.trace("QuerySession[{}] attach message {}", getId(), status);
            }).whenComplete((status, th) -> {
                if (th != null) {
                    logger.debug("QuerySession[{}] finished with exception", getId(), th);
                    createSpan.finishError(th);
                }

                if (status != null) {
                    if (status.isSuccess()) {
                        logger.debug("QuerySession[{}] finished with status {}", getId(), status);
                    } else {
                        logger.warn("QuerySession[{}] finished with status {}", getId(), status);
                    }
                    createSpan.finishStatus(status);
                }
            }).thenRun(this::clean);

            return future;
        }

        private void clean() {
            logger.debug("QuerySession[{}] attach stream is stopped", getId());
            isStopped = true;
            if (!isStarted) {
                destroy();
            }
        }

        public void destroy() {
            logger.debug("QuerySession[{}] destroy", getId());

            delete(DELETE_SETTINGS).whenComplete((status, th) -> {
                if (th != null) {
                    logger.warn("QuerySession[{}] removed with exception {}", getId(), th.getMessage());
                }
                if (status != null) {
                    if (status.isSuccess()) {
                        logger.debug("QuerySession[{}] successful removed", getId());
                    } else {
                        logger.warn("QuerySession[{}] removed with status {}", getId(), status);
                    }
                }
            });
        }

        @Override
        public void close() {
            logger.trace("QuerySession[{}] closed with broke status {}", getId(), isBroken);

            stats.released.increment();
            if (isBroken || isStopped) {
                queue.delete(this);
            } else {
                queue.release(this);
            }
        }
    }

    private class Handler implements WaitingQueue.Handler<PooledQuerySession> {
        private final QueryServiceRpc rpc;

        Handler(QueryServiceRpc rpc) {
            this.rpc = rpc;
        }

        @Override
        @SuppressWarnings("resource")
        public CompletableFuture<PooledQuerySession> create() {
            // Execute createSession call outside current context to avoid cancellation and deadline propogation
            Context ctx = Context.ROOT.fork();
            Context previous = ctx.attach();
            try {
                DbSpanFactory.OperationSpan createSpan = rpc.startSpan(
                        "CreateSession",
                        CREATE_SETTINGS.getTraceId(),
                        null
                );
                stats.requested.increment();
                return SessionImpl
                        .createSession(rpc, CREATE_SETTINGS, true, createSpan)
                        .thenCompose(r -> {
                            if (!r.isSuccess()) {
                                stats.failed.increment();
                                throw new UnexpectedResultException("create session problem", r.getStatus());
                            }
                            PooledQuerySession session = new PooledQuerySession(rpc, r.getValue(), createSpan);
                            return session.start();
                        })
                        .thenApply(Result::getValue)
                        .whenComplete((v, th) -> {
                            if (th != null) {
                                createSpan.finishError(FutureTools.unwrapCompletionException(th));
                            }
                        });
            } finally {
                ctx.detach(previous);
            }
        }

        @Override
        public void destroy(PooledQuerySession session) {
            stats.deleted.increment();

            // Execute deleteSession call outside current context to avoid cancellation and deadline propogation
            Context ctx = Context.ROOT.fork();
            Context previous = ctx.attach();
            try {
                session.destroy();
            } finally {
                ctx.detach(previous);
            }
        }
    }

    private class CleanerTask implements Runnable {
        private final long maxIdleTimeMillis;
        private final long periodMillis;

        CleanerTask(Duration idleDuration) {
            this.maxIdleTimeMillis = idleDuration.toMillis();
            // Cleaner task execution frequency limit - must be executed at least 3 times
            // for idle, but no more than once every 500 ms
            this.periodMillis = Math.max(500, maxIdleTimeMillis / 3);
        }

        @Override
        public void run() {
            Iterator<PooledQuerySession> coldIterator = queue.coldIterator();
            Instant now = clock.instant();
            Instant idleToRemove = now.minusMillis(maxIdleTimeMillis);

            while (coldIterator.hasNext()) {
                PooledQuerySession session = coldIterator.next();
                if (!session.getLastActive().isAfter(idleToRemove) && queue.getTotalCount() > minSize) {
                    coldIterator.remove();
                }
            }
        }
    }

    private class StatsImpl implements SessionPoolStats {
        private final LongAdder acquired = new LongAdder();
        private final LongAdder released = new LongAdder();

        private final LongAdder requested = new LongAdder();
        private final LongAdder failed = new LongAdder();
        private final LongAdder created = new LongAdder();
        private final LongAdder deleted = new LongAdder();

        @Override
        public int getMinSize() {
            return minSize;
        }

        @Override
        public int getMaxSize() {
            return queue.getTotalLimit();
        }

        @Override
        public int getIdleCount() {
            return queue.getIdleCount();
        }

        @Override
        public int getAcquiredCount() {
            return queue.getUsedCount();
        }

        @Override
        public int getPendingAcquireCount() {
            return queue.getWaitingCount() + queue.getPendingCount();
        }

        @Override
        public long getAcquiredTotal() {
            return acquired.sum();
        }

        @Override
        public long getReleasedTotal() {
            return released.sum();
        }

        @Override
        public long getRequestedTotal() {
            return requested.sum();
        }

        @Override
        public long getCreatedTotal() {
            return created.sum();
        }

        @Override
        public long getFailedTotal() {
            return failed.sum();
        }

        @Override
        public long getDeletedTotal() {
            return deleted.sum();
        }

        @Override
        public String toString() {
            return "SessionPoolStats{minSize=" + getMinSize()
                    + ", maxSize=" + getMaxSize()
                    + ", idleCount=" + getIdleCount()
                    + ", acquiredCount=" + getAcquiredCount()
                    + ", pendingAcquireCount=" + getPendingAcquireCount()
                    + ", acquiredTotal=" + getAcquiredTotal()
                    + ", releasedTotal=" + getReleasedTotal()
                    + ", requestsTotal=" + getRequestedTotal()
                    + ", createdTotal=" + getCreatedTotal()
                    + ", failedTotal=" + getFailedTotal()
                    + ", deletedTotal=" + getDeletedTotal()
                    + "}";
        }
    }

    /**
     * This is the part based on the code written by Doug Lea with assistance from members
     * of JCP JSR-166 Expert Group and released to the public domain, as explained at
     * http://creativecommons.org/publicdomain/zero/1.0/
     */

    /** Action to cancel unneeded timeouts */
    static final class Canceller implements BiConsumer<Object, Throwable> {
        private final Future<?> f;

        Canceller(Future<?> f) {
            this.f = f;
        }

        @Override
        public void accept(Object ignore, Throwable ex) {
            if (f != null && !f.isDone()) {
                f.cancel(false);
            }
        }
    }

    /** Action to completeExceptionally on timeout */
    static final class Timeout implements Runnable {
        private static final Status EXPIRE = Status.of(
                StatusCode.CLIENT_DEADLINE_EXPIRED,
                Issue.of("query session acquire deadline was expired", Issue.Severity.WARNING)
        );

        private final CompletableFuture<Result<QuerySession>> f;

        Timeout(CompletableFuture<Result<QuerySession>> f) {
            this.f = f;
        }

        @Override
        public void run() {
            if (f != null && !f.isDone()) {
                f.complete(Result.fail(EXPIRE));
            }
        }
    }
}
