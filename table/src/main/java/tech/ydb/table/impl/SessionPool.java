package tech.ydb.table.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.table.SessionStatus;
import tech.ydb.table.impl.SessionImpl.State;
import tech.ydb.table.impl.pool.FixedAsyncPool;
import tech.ydb.table.impl.pool.PooledObjectHandler;
import tech.ydb.table.impl.pool.SettlersPool;
import tech.ydb.table.settings.CloseSessionSettings;
import tech.ydb.table.settings.CreateSessionSettings;
import tech.ydb.table.stats.SessionPoolStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Polovko
 */
final class SessionPool implements PooledObjectHandler<SessionImpl> {
    private static final Logger logger = LoggerFactory.getLogger(SessionPool.class);

    private final TableClientImpl tableClient;

    /**
     * Pool to store sessions which are ready but idle right now
     */
    private final FixedAsyncPool<SessionImpl> idlePool;

    /**
     * Pool to store sessions with unknown status due to some transport errors.
     */
    private final SettlersPool<SessionImpl> settlersPool;

    private final int minSize;
    private final int maxSize;

    SessionPool(TableClientImpl tableClient, SessionPoolOptions options) {
        this.tableClient = tableClient;
        this.minSize = options.getMinSize();
        this.maxSize = options.getMaxSize();
        this.idlePool = new FixedAsyncPool<>(
            this,
            minSize,
            maxSize,
            maxSize * 2,
            options.getKeepAliveTimeMillis(),
            options.getMaxIdleTimeMillis());
        this.settlersPool = new SettlersPool<>(this, idlePool, 10, 5_000);
    }

    @Override
    public CompletableFuture<SessionImpl> create(long deadlineAfter) {
        return tableClient.createSessionImpl(new CreateSessionSettings().setDeadlineAfter(deadlineAfter), this)
            .thenApply(r -> {
                SessionImpl session = (SessionImpl) r.expect("cannot create session");
                session.setState(State.IDLE);
                return session;
            });
    }

    @Override
    public CompletableFuture<Void> destroy(SessionImpl s) {
        return s.delete(new CloseSessionSettings())
            .thenAccept(r -> r.expect("cannot close session: " + s.getId()));
    }

    @Override
    public boolean isValid(SessionImpl s) {
        return s.switchState(State.ACTIVE, State.IDLE);
    }

    @Override
    public CompletableFuture<Result<SessionStatus>> keepAlive(SessionImpl s) {
        return s.keepAlive();
    }

    CompletableFuture<SessionImpl> acquire(Duration timeout) {
        final Instant startTime = Instant.now();
        return idlePool.acquire(timeout)
            .thenCompose(s -> {
                if (s.switchState(State.IDLE, State.ACTIVE)) {
                    logger.debug("session `{}' acquired", s);
                    return CompletableFuture.completedFuture(s);
                } else {
                    release(s);
                    Duration duration = Duration.between(startTime, Instant.now());
                    return acquire(timeout.minus(Duration.ZERO.compareTo(duration) < 0
                        ? duration
                        : Duration.ZERO));
                }
            });
    }

    void release(SessionImpl session) {
        if (session.switchState(State.DISCONNECTED, State.IDLE)) {
            if (!settlersPool.offerIfHaveSpace(session)) {
                logger.debug("Destroy {} because settlers pool overflow", session);
                session.close(); // do not await session to be closed
                idlePool.release(session);
            }
        } else {
            idlePool.release(session);
            logger.debug("session `{}' released", session);
        }
    }

    void delete(SessionImpl session) {
        idlePool.delete(session);
    }

    void close() {
        idlePool.close();
        settlersPool.close();
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
