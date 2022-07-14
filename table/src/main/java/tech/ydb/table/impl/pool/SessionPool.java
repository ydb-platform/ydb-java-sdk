package tech.ydb.table.impl.pool;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final static Logger logger = LoggerFactory.getLogger(SessionPool.class);
    private final int minSize;
    private final ScheduledExecutorService scheduler;
    private final WaitingQueue<ClosableSession> queue;
    private final ScheduledFuture<?> keepAliveFuture;
    
    public SessionPool(ScheduledExecutorService scheduler, TableRpc rpc, boolean keepQueryText, SessionPoolOptions options) {
        this.scheduler = scheduler;
        this.queue = new WaitingQueue<>(new Handler(rpc, keepQueryText), options.getMaxSize());
        this.minSize = options.getMinSize();

        KeepAliveTask keepAlive = new KeepAliveTask(options);
        this.keepAliveFuture = scheduler.scheduleAtFixedRate(
                keepAlive,
                keepAlive.period / 2,
                keepAlive.period,
                TimeUnit.MILLISECONDS);
        logger.info("init session pool, min size = {}, max size = {}", options.getMinSize(), options.getMaxSize());
    }

    @Override
    public void close() {
        logger.info("closing session pool");

        keepAliveFuture.cancel(false);
        queue.close();
    }

    public SessionPoolStats stats() {
        return new SessionPoolStats(
                minSize,
                queue.queueLimit(),
                queue.idleSize(),
                queue.queueSize() - queue.idleSize(),
                queue.waitingsSize());
    }
    
    public CompletableFuture<? extends Session> acquire(Duration timeout) {
        return pollNext(timeout != null ? timeout : Duration.ZERO);
    }

    private CompletableFuture<ClosableSession> pollNext(Duration timeout) {
        Instant deadline = Instant.now().plusMillis(timeout.toMillis());
        CompletableFuture<ClosableSession> future = new CompletableFuture<>();
        queue.acquire(future);
        
        // On JDK9 may be replaced 
        // with future.onTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
        if (!future.isDone()) {
            future.whenComplete(new Canceller(scheduler.schedule(
                    new Timeout(future),
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS)
            ));
        }

        return future.thenApply(new SessionValidator(deadline));
    }
    
    private class ClosableSession extends StatefulSession {
        public ClosableSession(String id, TableRpc rpc, boolean keepQueryText) {
            super(id, rpc, keepQueryText);
            logger.debug("new session {} is created", id);
        }
        
        @Override
        public void close() {
            if (state().switchToIdle(Instant.now())) {
                logger.debug("session {} release", getId());
                queue.release(this);
            } else {
                if (state().needShutdown()) {
                    logger.debug("session {} shutdown", getId());
                    queue.delete(this);
                }
            }
        }
    }
    
    private class Handler implements WaitingQueue.Handler<ClosableSession> {
        private final TableRpc tableRpc;
        private final boolean keepQueryText;

        public Handler(TableRpc tableRpc, boolean keepQueryText) {
            this.tableRpc = tableRpc;
            this.keepQueryText = keepQueryText;
        }
        
        @Override
        public CompletableFuture<ClosableSession> create() {
            return BaseSession
                    .createSessionId(tableRpc, new CreateSessionSettings())
                    .thenApply(response -> {
                        String id = response.expect("cannot create session");
                        return new ClosableSession(id, tableRpc, keepQueryText);
                    });
        }

        @Override
        public void destroy(ClosableSession session) {
            logger.debug("remove session[{}]", session.getId());
            session.delete(new DeleteSessionSettings()).whenComplete((status, tw) -> {
                if (tw != null) {
                    logger.warn("session[{}] removed with exception {}", session.getId(), tw.getMessage());
                }
                if (status != null && !status.isSuccess()) {
                    logger.warn("session[{}] removed with status {}", session.getId(), status.toString());
                }
            });
        }
    }
    
    private class KeepAliveTask implements Runnable {
        private final long maxIdleTimeMillis;
        private final long keepAliveTimeMillis;

        private final int maxKeepAliveCount;
        private final long period;

        private final AtomicInteger keepAliveCount = new AtomicInteger(0);

        public KeepAliveTask(SessionPoolOptions options) {
            this.maxIdleTimeMillis = options.getMaxIdleTimeMillis();
            this.keepAliveTimeMillis = options.getKeepAliveTimeMillis();
            
            this.maxKeepAliveCount = Math.max(2, options.getMaxSize() / 5);
            this.period = Math.max(1_000, Math.min(keepAliveTimeMillis / 5, maxIdleTimeMillis / 2));
        }
        
        @Override
        public void run() {
            Iterator<ClosableSession> coldIterator = queue.coldIterator();
            Instant now = Instant.now();
            Instant idleToRemove = now.minusMillis(maxIdleTimeMillis);
            Instant keepAlive = now.minusMillis(keepAliveTimeMillis);

            while (coldIterator.hasNext()) {
                StatefulSession session = coldIterator.next();
                StatefulSession.State state = session.state();
                if (state.needShutdown()) {
                    coldIterator.remove();
                    continue;
                }
                
                if (state.lastActive().isBefore(idleToRemove) && queue.queueSize() > minSize) {
                    coldIterator.remove();
                    continue;
                }

                if (state.lastUpdate().isBefore(keepAlive)) {
                    if (keepAliveCount.get() >= maxKeepAliveCount) {
                        continue;
                    }
                    
                    if (state.switchToActive(now)) {
                        StatefulSession.State keepAliveState = session.state();
                        keepAliveCount.incrementAndGet();
                        logger.debug("keep alive session {}", session.getId());
                        session.keepAlive().whenComplete((res, tw) -> {
                            boolean ok = tw == null && res.isSuccess();
                            keepAliveCount.decrementAndGet();
                            if (ok) {
                                logger.debug("keep alive session {} ok", session.getId());
                                keepAliveState.switchToIdle(Instant.now());
                            } else {
                                logger.debug("keep alive session {} error, change status to broken", session.getId());
                                keepAliveState.switchToBroken(Instant.now());
                            }
                        });
                    }
                }
            }
        }
    }
    
    private class SessionValidator implements Function<ClosableSession, ClosableSession> {
        private final Instant deadline;
        
        public SessionValidator(Instant deadline) {
            this.deadline = deadline;
        }

        @Override
        public ClosableSession apply(ClosableSession session) {
            Instant now = Instant.now();
            if (session.state().switchToActive(now)) {
                return session;
            }

            if (now.isAfter(deadline)) {
                queue.release(session);
                throw new CompletionException(new TimeoutException("deadline was expired"));
            }

            CompletableFuture<ClosableSession> next = pollNext(Duration.between(now, deadline));
            queue.release(session);
            return next.join();
        }
    }

    /** Action to cancel unneeded timeouts */
    static final class Canceller implements BiConsumer<Object, Throwable> {
        final Future<?> f;
        Canceller(Future<?> f) { this.f = f; }
        @Override
        public void accept(Object ignore, Throwable ex) {
            if (ex == null && f != null && !f.isDone())
                f.cancel(false);
        }
    }

    /** Action to completeExceptionally on timeout */
    static final class Timeout implements Runnable {
        final CompletableFuture<?> f;
        Timeout(CompletableFuture<?> f) { this.f = f; }
        @Override
        public void run() {
            if (f != null && !f.isDone())
                f.completeExceptionally(new TimeoutException());
        }
    }
    
}

