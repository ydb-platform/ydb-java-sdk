package tech.ydb.table.impl.pool;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.utils.Async;
import tech.ydb.table.Session;
import tech.ydb.table.SessionPoolStats;
import tech.ydb.table.impl.BaseSession;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.CreateSessionSettings;
import tech.ydb.table.settings.DeleteSessionSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SessionPool implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SessionPool.class);

    private static final CreateSessionSettings CREATE_SETTINGS = new CreateSessionSettings()
            .setTimeout(Duration.ofSeconds(300))
            .setOperationTimeout(Duration.ofSeconds(299));

    private final int minSize;
    private final Clock clock;
    private final ScheduledExecutorService scheduler;
    private final WaitingQueue<ClosableSession> queue;
    private final ScheduledFuture<?> keepAliveFuture;

    private final StatsImpl stats = new StatsImpl();

    public SessionPool(Clock clock, TableRpc rpc, boolean keepQueryText, SessionPoolOptions options) {
        this.minSize = options.getMinSize();

        this.clock = clock;
        this.scheduler = rpc.getScheduler();
        this.queue = new WaitingQueue<>(new Handler(rpc, keepQueryText), options.getMaxSize());

        KeepAliveTask keepAlive = new KeepAliveTask(options);
        this.keepAliveFuture = scheduler.scheduleAtFixedRate(
                keepAlive,
                keepAlive.periodMillis / 2,
                keepAlive.periodMillis,
                TimeUnit.MILLISECONDS);
        logger.info("init session pool, min size = {}, max size = {}, keep alive period = {}",
                options.getMinSize(),
                options.getMaxSize(),
                keepAlive.periodMillis);
    }

    public void updateMaxSize(int maxSize) {
        this.queue.updateLimits(maxSize);
    }

    @Override
    public void close() {
        logger.info("closing session pool");

        keepAliveFuture.cancel(false);
        queue.close();
    }

    public SessionPoolStats stats() {
        return stats;
    }

    public CompletableFuture<Result<Session>> acquire(Duration timeout) {
        logger.debug("acquire session with timeout {}", timeout);

        CompletableFuture<Result<Session>> future = new CompletableFuture<>();

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

    private boolean pollNext(CompletableFuture<Result<Session>> future) {
        CompletableFuture<ClosableSession> nextSession = new CompletableFuture<>();
        queue.acquire(nextSession);

        if (nextSession.isDone() && !nextSession.isCompletedExceptionally()) {
            return validateSession(nextSession.join(), future);
        }

        nextSession.whenComplete((session, th) -> {
            if (th != null) {
                Throwable ex = Async.unwrapCompletionException(th);
                Result<Session> fail = (ex instanceof UnexpectedResultException)
                        ? Result.fail((UnexpectedResultException) ex)
                        : Result.error("can't create session", ex);

                if (!future.complete(fail)) {
                    logger.warn("session acquisition failed with status {}", fail);
                    return;
                }

            }
            if (session != null) {
                validateSession(session, future);
            }
        });
        return false;
    }

    private boolean validateSession(ClosableSession session, CompletableFuture<Result<Session>> future) {
        if (session.state().switchToActive(clock.instant())) {
            logger.debug("session {} accepted", session.getId());
            if (future.complete(Result.success(session))) {
                stats.acquired.increment();
            } else {
                // Future is already completed
                logger.debug("session future already canceled, return session to the pool");
                session.state().switchToIdle(clock.instant());
                queue.release(session);
            }
            return true;
        }

        // If session can't switch to active state, delete it and try again
        queue.delete(session);
        return pollNext(future);
    }

    private class ClosableSession extends StatefulSession {
        ClosableSession(String id, TableRpc rpc, boolean keepQueryText) {
            super(id, clock, rpc, keepQueryText);
            logger.debug("session {} successful created", id);
            stats.created.increment();
        }

        @Override
        public void close() {
            stats.released.increment();
            if (state().switchToIdle(clock.instant())) {
                logger.debug("session {} release", getId());
                queue.release(this);
            } else {
                logger.debug("session {} shutdown", getId());
                queue.delete(this);
            }
        }
    }

    private class Handler implements WaitingQueue.Handler<ClosableSession> {
        private final TableRpc tableRpc;
        private final boolean keepQueryText;

        Handler(TableRpc tableRpc, boolean keepQueryText) {
            this.tableRpc = tableRpc;
            this.keepQueryText = keepQueryText;
        }

        @Override
        public CompletableFuture<ClosableSession> create() {
            stats.requested.increment();
            return BaseSession
                    .createSessionId(tableRpc, CREATE_SETTINGS, true)
                    .thenApply(response -> {
                        if (!response.isSuccess()) {
                            stats.failed.increment();
                            throw new UnexpectedResultException("create session problem", response.getStatus());
                        }
                        return new ClosableSession(response.getValue(), tableRpc, keepQueryText);
                    });
        }

        @Override
        public void destroy(ClosableSession session) {
            stats.deleted.increment();
            session.delete(new DeleteSessionSettings()).whenComplete((status, th) -> {
                if (th != null) {
                    logger.warn("session {} destoryed with exception {}", session.getId(), th.getMessage());
                }
                if (status != null) {
                    if (status.isSuccess()) {
                        logger.debug("session {} successful destoryed", session.getId());
                    } else {
                        logger.warn("session {} destoryed with status {}", session.getId(), status.toString());
                    }
                }
            });
        }
    }

    private class KeepAliveTask implements Runnable {
        private final long maxIdleTimeMillis;
        private final long keepAliveTimeMillis;

        private final int maxKeepAliveCount;
        private final long periodMillis;

        private final AtomicInteger keepAliveCount = new AtomicInteger(0);

        KeepAliveTask(SessionPoolOptions options) {
            this.maxIdleTimeMillis = options.getMaxIdleTimeMillis();
            this.keepAliveTimeMillis = options.getKeepAliveTimeMillis();

            // Simple heuristics to limit task inflight and frequency
            // KeepAlive task inflight limit - not more than 20 percent but not less than two
            this.maxKeepAliveCount = Math.max(2, options.getMaxSize() / 5);
            // KeepAlive task execution frequency limit - must be executed at least 5 times
            // for keepAlive and at least 2 times for idle, but no more than once every 100 ms
            this.periodMillis = Math.max(100, Math.min(keepAliveTimeMillis / 5, maxIdleTimeMillis / 2));
        }

        @Override
        public void run() {
            Iterator<ClosableSession> coldIterator = queue.coldIterator();
            Instant now = clock.instant();
            Instant idleToRemove = now.minusMillis(maxIdleTimeMillis);
            Instant keepAlive = now.minusMillis(keepAliveTimeMillis);

            while (coldIterator.hasNext()) {
                StatefulSession session = coldIterator.next();
                StatefulSession.State state = session.state();
                if (state.needShutdown()) {
                    coldIterator.remove();
                    continue;
                }

                if (!state.lastActive().isAfter(idleToRemove) && queue.getTotalCount() > minSize) {
                    coldIterator.remove();
                    continue;
                }

                if (!state.lastUpdate().isAfter(keepAlive)) {
                    if (keepAliveCount.get() >= maxKeepAliveCount) {
                        continue;
                    }

                    if (state.switchToKeepAlive(now)) {
                        keepAliveCount.incrementAndGet();
                        logger.debug("keep alive session {}", session.getId());
                        session.keepAlive().whenComplete((res, th) -> {
                            boolean ok = th == null
                                    && res.isSuccess()
                                    && res.getValue() == Session.State.READY;
                            keepAliveCount.decrementAndGet();
                            if (ok) {
                                logger.debug("keep alive session {} ok", session.getId());
                                session.state().switchToIdle(clock.instant());
                            } else {
                                logger.debug("keep alive session {} error, change status to broken", session.getId());
                                session.state().switchToBroken(clock.instant());
                            }
                        });
                    }
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
        private static final Status EXPIRE = Status.of(StatusCode.CLIENT_DEADLINE_EXPIRED, null,
                Issue.of("session acquire deadline was expired", Issue.Severity.WARNING));

        private final CompletableFuture<Result<Session>> f;

        Timeout(CompletableFuture<Result<Session>> f) {
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

