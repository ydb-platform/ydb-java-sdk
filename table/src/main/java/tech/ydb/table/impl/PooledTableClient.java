package tech.ydb.table.impl;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Preconditions;

import tech.ydb.core.Result;
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
    private final TableRpc tableRpc;
    private final SessionPool pool;

    PooledTableClient(Builder builder) {
        this.tableRpc = builder.tableRpc;
        this.pool = new SessionPool(
                Clock.systemUTC(),
                builder.tableRpc,
                builder.keepQueryText,
                builder.sessionPoolOptions
        );
    }

    @Override
    public CompletableFuture<Result<Session>> createSession(Duration timeout) {
        return pool.acquire(timeout);
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return tableRpc.getScheduler();
    }

    @Override
    public void close() {
        pool.close();
    }

    public void updatePoolMaxSize(int maxSize) {
        pool.updateMaxSize(maxSize);
    }

    @Override
    public SessionPoolStats sessionPoolStats() {
        return pool.stats();
    }

    public static TableClient.Builder newClient(TableRpc rpc) {
        return new Builder(rpc);
    }

    private static class Builder implements TableClient.Builder {
        /** Minimal duration of keep alive and idle */
        private static final Duration MIN_DURATION = Duration.ofSeconds(1);
        /** Maximal duration of keep alive and idle */
        private static final Duration MAX_DURATION = Duration.ofMinutes(30);

        private final TableRpc tableRpc;
        private boolean keepQueryText = true;
        private SessionPoolOptions sessionPoolOptions = SessionPoolOptions.DEFAULT;

        Builder(TableRpc tableRpc) {
            Preconditions.checkArgument(tableRpc != null, "table rpc is null");
            this.tableRpc = tableRpc;
        }

        private static String prettyDuration(Duration duration) {
            // convert ISO-8601 format to more readable, etc PT2S will be printed as 2s
            return duration.toString().substring(2).toLowerCase();
        }

        @Override
        public Builder keepQueryText(boolean keep) {
            this.keepQueryText = keep;
            return this;
        }

        @Override
        public Builder sessionPoolSize(int minSize, int maxSize) {
            Preconditions.checkArgument(minSize >= 0, "sessionPoolMinSize(%s) is negative", minSize);
            Preconditions.checkArgument(maxSize > 0, "sessionPoolMaxSize(%s) is negative or zero", maxSize);
            Preconditions.checkArgument(
                minSize <= maxSize,
                "sessionPoolMinSize(%s) is greater than sessionPoolMaxSize(%s)",
                minSize, maxSize);
            this.sessionPoolOptions = sessionPoolOptions.withSize(minSize, maxSize);
            return this;
        }

        @Override
        public Builder sessionKeepAliveTime(Duration duration) {
            Preconditions.checkArgument(!duration.isNegative(),
                    "sessionKeepAliveTime(%s) is negative", prettyDuration(duration));

            Preconditions.checkArgument(duration.compareTo(MIN_DURATION) >= 0,
                "sessionKeepAliveTime(%s) is less than minimal duration %s",
                prettyDuration(duration), prettyDuration(MIN_DURATION));
            Preconditions.checkArgument(duration.compareTo(MAX_DURATION) <= 0,
                "sessionKeepAliveTime(%s) is greater than maximal duration %s",
                prettyDuration(duration), prettyDuration(MAX_DURATION));

            this.sessionPoolOptions = sessionPoolOptions.withKeepAliveTimeMillis(duration.toMillis());
            return this;
        }

        @Override
        public Builder sessionMaxIdleTime(Duration duration) {
            Preconditions.checkArgument(!duration.isNegative(),
                    "sessionMaxIdleTime(%s) is negative", prettyDuration(duration));

            Preconditions.checkArgument(duration.compareTo(MIN_DURATION) >= 0,
                "sessionMaxIdleTime(%s) is less than minimal duration %s",
                prettyDuration(duration), prettyDuration(MIN_DURATION));
            Preconditions.checkArgument(duration.compareTo(MAX_DURATION) <= 0,
                "sessionMaxIdleTime(%s) is greater than maximal duration %s",
                prettyDuration(duration), prettyDuration(MAX_DURATION));

            this.sessionPoolOptions = sessionPoolOptions.withMaxIdleTimeMillis(duration.toMillis());
            return this;
        }

        @Override
        public TableClient build() {
            return new PooledTableClient(this);
        }
    }
}
