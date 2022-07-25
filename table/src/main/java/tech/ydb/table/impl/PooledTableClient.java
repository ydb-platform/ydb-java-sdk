package tech.ydb.table.impl;

import com.google.common.base.Preconditions;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tech.ydb.core.Result;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.utils.Async;
import tech.ydb.table.Session;
import tech.ydb.table.SessionPoolStats;
import tech.ydb.table.TableClient;
import tech.ydb.table.impl.pool.SessionPool;
import tech.ydb.table.impl.pool.SessionPoolOptions;
import tech.ydb.table.rpc.TableRpc;

/**
 * @author Aleksandr Gorshenin
 */
public class PooledTableClient implements TableClient {
    static public TableClient.Builder newClient(TableRpc rpc) {
        return new Builder(rpc);
    }

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            (Runnable r) -> new Thread(r, "YdbTablePoolScheduler")
    );
    private final SessionPool pool;

    PooledTableClient(Builder builder) {
        this.pool = new SessionPool(
                executor,
                Clock.systemUTC(),
                builder.tableRpc, 
                builder.keepQueryText, 
                builder.sessionPoolOptions
        );
    }

    @Override
    public CompletableFuture<Result<Session>> createSession(Duration timeout) {
        return pool.acquire(timeout).handle((session, tw) -> {
            if (tw != null) {
                Throwable ex = Async.unwrapCompletionException(tw);
                if (ex instanceof UnexpectedResultException) {
                    return Result.fail((UnexpectedResultException)ex);
                } else {
                    return Result.error(ex);
                }
            }
            return Result.success(session);
        });
    }

    @Override
    public void close() {
        pool.close();
        executor.shutdown();
    }

    @Override
    public SessionPoolStats sessionPoolStats() {
        return pool.stats();
    }

    static private class Builder implements TableClient.Builder {
        /** Minimal duration of keep alive and idle */
        private final static long MIN_DURATION = TimeUnit.SECONDS.toMillis(1);
        /** Maximal duration of keep alive and idle */
        private final static long MAX_DURATION = TimeUnit.MINUTES.toMillis(30);

        private final TableRpc tableRpc;
        private boolean keepQueryText = true;
        private SessionPoolOptions sessionPoolOptions = SessionPoolOptions.DEFAULT;

        public Builder(TableRpc tableRpc) {
            Preconditions.checkArgument(tableRpc != null, "table rpc is null");
            this.tableRpc = tableRpc;
        }

        @Override
        public Builder keepQueryText(boolean keep) {
            this.keepQueryText = keep;
            return this;
        }

        @Override
        public Builder sessionPoolSize(int minSize, int maxSize) {
            Preconditions.checkArgument(minSize >= 0, "sessionPoolMinSize(%d) is negative", minSize);
            Preconditions.checkArgument(maxSize > 0, "sessionPoolMaxSize(%d) is negative or zero", maxSize);
            Preconditions.checkArgument(
                minSize <= maxSize,
                "sessionPoolMinSize(%d) is greater than sessionPoolMaxSize(%d)",
                minSize, maxSize);
            this.sessionPoolOptions = sessionPoolOptions.withSize(minSize, maxSize);
            return this;
        }

        @Override
        public Builder sessionKeepAliveTime(Duration duration) {
            Preconditions.checkArgument(!duration.isNegative(),
                    "sessionKeepAliveTime(%s) is negative", duration);

            long timeMillis = duration.toMillis();
            Preconditions.checkArgument(timeMillis >= MIN_DURATION,
                "sessionKeepAliveTime(%s) is less than 1 second", duration);
            Preconditions.checkArgument(timeMillis <= MAX_DURATION,
                "sessionKeepAliveTime(%s) is greater than 30 minutes", duration);

            this.sessionPoolOptions = sessionPoolOptions.withKeepAliveTimeMillis(timeMillis);
            return this;
        }

        @Override
        public Builder sessionMaxIdleTime(Duration duration) {
            Preconditions.checkArgument(!duration.isNegative(), "sessionMaxIdleTime(%s) is negative", duration);

            long timeMillis = duration.toMillis();
            Preconditions.checkArgument(timeMillis >= MIN_DURATION,
                "sessionMaxIdleTime(%s) is less than 1 second", duration);
            Preconditions.checkArgument(timeMillis <= MAX_DURATION,
                "sessionMaxIdleTime(%s) is greater than 30 minutes", duration);

            this.sessionPoolOptions = sessionPoolOptions.withMaxIdleTimeMillis(timeMillis);
            return this;
        }

        @Override
        public TableClient build() {
            return new PooledTableClient(this);
        }
    }
}
