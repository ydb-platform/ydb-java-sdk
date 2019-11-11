package tech.ydb.table.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import tech.ydb.table.SessionStatus;
import tech.ydb.table.impl.SessionImpl.State;
import tech.ydb.table.impl.pool.FixedAsyncPool;
import tech.ydb.table.impl.pool.PooledObjectHandler;
import tech.ydb.table.impl.pool.SettlersPool;
import tech.ydb.table.settings.CreateSessionSettings;
import tech.ydb.table.stats.SessionPoolStats;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultThreadFactory;


/**
 * @author Sergey Polovko
 */
final class SessionPool implements PooledObjectHandler<SessionImpl> {
    private static final Logger logger = Logger.getLogger(SessionPool.class.getName());

    private final TableClientImpl tableClient;

    /**
     * Pool to store sessions which are ready but ide right now
     */
    private final FixedAsyncPool<SessionImpl> idlePool;

    /**
     * Pool to store sessions with unknown status due to some transport errors.
     */
    private final SettlersPool<SessionImpl> settlersPool;

    private final Timer timer;
    private final int minSize;
    private final int maxSize;

    SessionPool(TableClientImpl tableClient, SessionPoolOptions options) {
        this.tableClient = tableClient;
        this.minSize = options.getMinSize();
        this.maxSize = options.getMaxSize();
        this.timer = new HashedWheelTimer(new DefaultThreadFactory("SessionPoolTimer"));
        this.idlePool = new FixedAsyncPool<>(
            this,
            timer,
            minSize,
            maxSize,
            maxSize * 2,
            options.getKeepAliveTimeMillis(),
            options.getMaxIdleTimeMillis());
        this.settlersPool = new SettlersPool<>(this, idlePool, timer, 10, 5_000);
    }

    @Override
    public CompletableFuture<SessionImpl> create(long deadlineAfter) {
        return tableClient.createSessionImpl(new CreateSessionSettings().setDeadlineAfter(deadlineAfter), this)
            .thenApply(r -> (SessionImpl) r.expect("cannot create session"));
    }

    @Override
    public CompletableFuture<Void> destroy(SessionImpl s) {
        return s.close()
            .thenAccept(r -> r.expect("cannot close session: " + s.getId()));
    }

    @Override
    public boolean isValid(SessionImpl s) {
        return s.switchState(State.ACTIVE, State.IDLE);
    }

    @Override
    public CompletableFuture<Boolean> keepAlive(SessionImpl s) {
        return s.keepAlive()
            .thenApply(r -> {
                if (!r.isSuccess()) {
                    return Boolean.FALSE;
                }
                SessionStatus status = r.expect("cannot keep alive session: " + s.getId());
                return status == SessionStatus.READY;
            });
    }

    CompletableFuture<SessionImpl> acquire(Duration timeout) {
        return idlePool.acquire(timeout)
            .thenApply(s -> {
                s.setState(State.ACTIVE);
                return s;
            });
    }

    void release(SessionImpl session) {
        if (session.switchState(State.DISCONNECTED, State.IDLE)) {
            if (!settlersPool.offerIfHaveSpace(session)) {
                logger.log(Level.FINE, "Destroy {0} because settlers pool overflow", session);
                session.close(); // do not await session to be closed
            }
        } else {
            idlePool.release(session);
        }
    }

    void close() {
        try {
            idlePool.close();
            settlersPool.close();
        } finally {
            timer.stop();
        }
    }

    public SessionPoolStats getStats() {
        return new SessionPoolStats(
            minSize,
            maxSize,
            idlePool.getIdleCount(),
            settlersPool.size(),
            idlePool.getAcquiredCount(),
            idlePool.getPendingAcquireCount());
    }
}
