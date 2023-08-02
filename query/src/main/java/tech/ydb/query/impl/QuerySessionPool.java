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
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.utils.Async;
import tech.ydb.proto.draft.query.YdbQuery;
import tech.ydb.query.QuerySession;
import tech.ydb.query.settings.CreateSessionSettings;
import tech.ydb.query.settings.DeleteSessionSettings;
import tech.ydb.table.impl.pool.WaitingQueue;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class QuerySessionPool implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(QuerySessionPool.class);

    private static final CreateSessionSettings CREATE_SETTINGS = CreateSessionSettings.newBuilder()
            .withRequestTimeout(Duration.ofSeconds(300))
            .withOperationTimeout(Duration.ofSeconds(299))
            .build();

    private static final DeleteSessionSettings DELETE_SETTINGS = DeleteSessionSettings.newBuilder()
            .withRequestTimeout(Duration.ofSeconds(5))
            .withOperationTimeout(Duration.ofSeconds(4))
            .build();

    private final int minSize;
    private final Clock clock;
    private final ScheduledExecutorService scheduler;
    private final WaitingQueue<QuerySessionImpl> queue;
    private final ScheduledFuture<?> cleanerFuture;

    public QuerySessionPool(
            Clock clock,
            QueryServiceRpc rpc,
            ScheduledExecutorService scheduler,
            int minSize,
            int maxSize,
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
        logger.info("init query session pool, min size = {}, max size = {}, keep alive period = {}",
                minSize,
                maxSize,
                cleaner.periodMillis);
    }

    public void updateMaxSize(int maxSize) {
        this.queue.updateLimits(maxSize);
    }

    @Override
    public void close() {
        logger.info("closing session pool");
        cleanerFuture.cancel(false);
        queue.close();
    }

    public CompletableFuture<Result<QuerySession>> acquire(Duration timeout) {
        logger.debug("acquire session with timeout {}", timeout);

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
        CompletableFuture<QuerySessionImpl> nextSession = new CompletableFuture<>();
        queue.acquire(nextSession);

        if (nextSession.isDone() && !nextSession.isCompletedExceptionally()) {
            return tryComplete(future, nextSession.join());
        }

        nextSession.whenComplete((session, th) -> {
            if (th != null) {
                if (future.isDone()) {
                    logger.warn("can't get session, future is already canceled", th);
                    return;
                }

                Throwable ex = Async.unwrapCompletionException(th);
                if (ex instanceof UnexpectedResultException) {
                    future.complete(Result.fail((UnexpectedResultException) ex));
                } else {
                    future.complete(Result.error("can't create session", ex));
                }
            }
            if (session != null) {
                tryComplete(future, session);
            }
        });
        return false;
    }

    private boolean tryComplete(CompletableFuture<Result<QuerySession>> future, QuerySessionImpl session) {
        if (!future.complete(Result.success(session))) {
            logger.debug("session future already done, return session to the pool");
            queue.release(session);
            return false;
        }

        return true;
    }

    private class PooledQuerySession extends QuerySessionImpl {
        PooledQuerySession(QueryServiceRpc rpc, YdbQuery.CreateSessionResponse response) {
            super(clock, rpc, response);
        }

        @Override
        public void close() {
            queue.release(this);
        }
    }

    private class Handler implements WaitingQueue.Handler<QuerySessionImpl> {
        private final QueryServiceRpc rpc;

        Handler(QueryServiceRpc rpc) {
            this.rpc = rpc;
        }

        @Override
        public CompletableFuture<QuerySessionImpl> create() {
            return QuerySessionImpl
                    .createSession(rpc, CREATE_SETTINGS, true)
                    .thenApply(result -> new PooledQuerySession(rpc, result.getValue()));
        }

        @Override
        public void destroy(QuerySessionImpl session) {
            session.delete(DELETE_SETTINGS).whenComplete((status, th) -> {
                if (th != null) {
                    logger.warn("session {} removed with exception {}", session.getId(), th.getMessage());
                }
                if (status != null) {
                    if (status.isSuccess()) {
                        logger.debug("session {} successful removed", session.getId());
                    } else {
                        logger.warn("session {} removed with status {}", session.getId(), status.toString());
                    }
                }
            }).join();
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
            Iterator<QuerySessionImpl> coldIterator = queue.coldIterator();
            Instant now = clock.instant();
            Instant idleToRemove = now.minusMillis(maxIdleTimeMillis);

            while (coldIterator.hasNext()) {
                QuerySessionImpl session = coldIterator.next();

                if (!session.getLastActive().isAfter(idleToRemove) && queue.getTotalCount() > minSize) {
                    coldIterator.remove();
                }
            }
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
                Issue.of("query session acquire deadline was expired", Issue.Severity.WARNING));

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
