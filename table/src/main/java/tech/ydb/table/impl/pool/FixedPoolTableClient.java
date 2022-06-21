package tech.ydb.table.impl.pool;

import static com.google.common.base.Preconditions.checkArgument;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import tech.ydb.core.Result;
import tech.ydb.table.settings.CreateSessionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.table.Session;
import tech.ydb.table.impl.BaseSession;
import tech.ydb.table.SessionPoolStats;
import tech.ydb.table.TableClient;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.DeleteSessionSettings;

/**
 * @author Aleksandr Gorshenin
 */
public class FixedPoolTableClient implements TableClient {
    private static final Logger logger = LoggerFactory.getLogger(FixedPoolTableClient.class);

    static public TableClient.Builder newClient(TableRpc rpc) {
        return new Builder(rpc);
    }

    private final SessionHandler handler;

    /**
     * Pool to store sessions which are ready but idle right now
     */
    private final FixedAsyncPool<PooledSession> idlePool;

    /**
     * Pool to store sessions with unknown status due to some transport errors.
     */
    private final SettlersPool<PooledSession> settlersPool;
    
    private final Consumer<PooledSession> sessionDestroyer = this::release;

    private final int minSize;
    private final int maxSize;

    FixedPoolTableClient(Builder builder) {
        this.minSize = builder.sessionPoolOptions.getMinSize();
        this.maxSize = builder.sessionPoolOptions.getMaxSize();
        this.handler = new SessionHandler(builder.tableRpc, builder.keepQueryText);
        this.idlePool = new FixedAsyncPool<>(
            handler,
            minSize,
            maxSize,
            maxSize * 2,
            builder.sessionPoolOptions.getKeepAliveTimeMillis(),
            builder.sessionPoolOptions.getMaxIdleTimeMillis());
        this.settlersPool = new SettlersPool<>(handler, idlePool, 10, 5_000);
    }

    @Override
    public CompletableFuture<Result<Session>> createSession(Duration timeout) {
        return acquire(timeout);
    }

    private CompletableFuture<Result<Session>> acquire(Duration timeout) {
        final Instant startTime = Instant.now();
        return idlePool.acquire(timeout)
            .thenCompose(s -> {
                if (s.tryChangeToActive()) {
                    logger.debug("session `{}' acquired", s);
                    return CompletableFuture.completedFuture(Result.success(s));
                } else {
                    release(s);
                    Duration duration = Duration.between(startTime, Instant.now());
                    return acquire(timeout.minus(Duration.ZERO.compareTo(duration) < 0
                        ? duration
                        : Duration.ZERO));
                }
            });
    }

    private void release(PooledSession session) {
        if (session.tryRestoreToIdle()) {
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

    @Override
    public void close() {
        idlePool.close();
        settlersPool.close();
    }

    @Override
    public SessionPoolStats sessionPoolStats() {
        return new SessionPoolStats(
            minSize,
            maxSize,
            idlePool.getIdleCount(),
            settlersPool.size(),
            idlePool.getAcquiredCount(),
            idlePool.getPendingAcquireCount());
    }

    private class SessionHandler implements PooledObjectHandler<PooledSession> {
        private final TableRpc rpc;
        private final boolean keepQueryText;

        public SessionHandler(TableRpc rpc, boolean keepQueryText) {
            this.rpc = rpc;
            this.keepQueryText = keepQueryText;
        }
        
        @Override
        public CompletableFuture<PooledSession> create(long deadlineAfter) {
            CreateSessionSettings settings = new CreateSessionSettings().setDeadlineAfter(deadlineAfter); 
            return BaseSession.createSessionId(rpc, settings).thenApply(response -> {
                String id = response.expect("cannot create session");
                return new PooledSession(id, rpc, keepQueryText, sessionDestroyer);
            });
        }

        @Override
        public CompletableFuture<Void> destroy(PooledSession s) {
            return s.delete(new DeleteSessionSettings())
                .thenAccept(r -> r.expect("cannot close session: " + s.getId()));
        }

        @Override
        public boolean isValid(PooledSession s) {
            return s.tryChangeToIdle();
        }

        @Override
        public CompletableFuture<Result<Session.State>> keepAlive(PooledSession s) {
            return s.keepAlive();
        }
    }

    static private class Builder implements TableClient.Builder {
        private final TableRpc tableRpc;
        private boolean keepQueryText = true;
        private SessionPoolOptions sessionPoolOptions = SessionPoolOptions.DEFAULT;

        public Builder(TableRpc tableRpc) {
            this.tableRpc = tableRpc;
        }

        @Override
        public Builder keepQueryText(boolean keep) {
            this.keepQueryText = keep;
            return this;
        }

        @Override
        public Builder sessionPoolSize(int minSize, int maxSize) {
            checkArgument(minSize > 0, "sessionPoolMinSize(%d) is negative", minSize);
            checkArgument(maxSize > 0, "sessionPoolMaxSize(%d) is negative", maxSize);
            checkArgument(
                minSize <= maxSize,
                "sessionPoolMinSize(%d) is greater than sessionPoolMaxSize(%d)",
                minSize, maxSize);
            this.sessionPoolOptions = sessionPoolOptions.withSize(minSize, maxSize);
            return this;
        }

        @Override
        public Builder sessionKeepAliveTime(Duration duration) {
            checkArgument(!duration.isNegative(), "sessionKeepAliveTime(%s) is negative", duration);
            long timeMillis = duration.toMillis();
            checkArgument(
                timeMillis >= TimeUnit.SECONDS.toMillis(1),
                "sessionKeepAliveTime(%s) is less than 1 second",
                duration);
            checkArgument(
                timeMillis <= TimeUnit.MINUTES.toMillis(30),
                "sessionKeepAliveTime(%s) is greater than 30 minutes",
                duration);
            this.sessionPoolOptions = sessionPoolOptions.withKeepAliveTimeMillis(timeMillis);
            return this;
        }

        @Override
        public Builder sessionMaxIdleTime(Duration duration) {
            checkArgument(!duration.isNegative(), "sessionMaxIdleTime(%s) is negative", duration);
            long timeMillis = duration.toMillis();
            checkArgument(
                timeMillis >= TimeUnit.SECONDS.toMillis(1),
                "sessionMaxIdleTime(%s) is less than 1 second",
                duration);
            checkArgument(
                timeMillis <= TimeUnit.MINUTES.toMillis(30),
                "sessionMaxIdleTime(%s) is greater than 30 minutes",
                duration);
            this.sessionPoolOptions = sessionPoolOptions.withMaxIdleTimeMillis(timeMillis);
            return this;
        }

        @Override
        public TableClient build() {
            return new FixedPoolTableClient(this);
        }
    }
}
