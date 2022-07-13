package tech.ydb.table.impl.pool;

import com.google.common.base.Preconditions;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import tech.ydb.core.Result;
import tech.ydb.table.Session;
import tech.ydb.table.SessionPoolStats;
import tech.ydb.table.TableClient;
import tech.ydb.table.rpc.TableRpc;

/**
 * @author Aleksandr Gorshenin
 */
public class FixedPoolTableClient implements TableClient {
    static public TableClient.Builder newClient(TableRpc rpc) {
        return new Builder(rpc);
    }

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(
            1, (Runnable r) -> new Thread(r, "YdbTablePoolScheduler")
    );
    private final SessionPool pool;

    FixedPoolTableClient(Builder builder) {
        this.pool = new SessionPool(
                executor, 
                builder.tableRpc, 
                builder.keepQueryText, 
                builder.sessionPoolOptions
        );
    }

    @Override
    public CompletableFuture<Result<Session>> createSession(Duration timeout) {
        return pool.acquire(timeout).handle((session, tw) -> {
            if (tw != null) {
                return Result.error(tw);
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
            Preconditions.checkArgument(minSize > 0, "sessionPoolMinSize(%d) is negative", minSize);
            Preconditions.checkArgument(maxSize > 0, "sessionPoolMaxSize(%d) is negative", maxSize);
            Preconditions.checkArgument(
                minSize <= maxSize,
                "sessionPoolMinSize(%d) is greater than sessionPoolMaxSize(%d)",
                minSize, maxSize);
            this.sessionPoolOptions = sessionPoolOptions.withSize(minSize, maxSize);
            return this;
        }

        @Override
        public Builder sessionKeepAliveTime(Duration duration) {
            Preconditions.checkArgument(!duration.isNegative(), "sessionKeepAliveTime(%s) is negative", duration);
            long timeMillis = duration.toMillis();
            Preconditions.checkArgument(
                timeMillis >= TimeUnit.SECONDS.toMillis(1),
                "sessionKeepAliveTime(%s) is less than 1 second",
                duration);
            Preconditions.checkArgument(
                timeMillis <= TimeUnit.MINUTES.toMillis(30),
                "sessionKeepAliveTime(%s) is greater than 30 minutes",
                duration);
            this.sessionPoolOptions = sessionPoolOptions.withKeepAliveTimeMillis(timeMillis);
            return this;
        }

        @Override
        public Builder sessionMaxIdleTime(Duration duration) {
            Preconditions.checkArgument(!duration.isNegative(), "sessionMaxIdleTime(%s) is negative", duration);
            long timeMillis = duration.toMillis();
            Preconditions.checkArgument(
                timeMillis >= TimeUnit.SECONDS.toMillis(1),
                "sessionMaxIdleTime(%s) is less than 1 second",
                duration);
            Preconditions.checkArgument(
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
