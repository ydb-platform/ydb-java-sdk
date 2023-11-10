package tech.ydb.query.impl;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Preconditions;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryClientImpl implements  QueryClient {
    private final QuerySessionPool pool;
    private final ScheduledExecutorService scheduler;

    public QueryClientImpl(Builder builder) {
        this.pool = new QuerySessionPool(
                Clock.systemUTC(),
                new QueryServiceRpc(builder.transport),
                builder.transport.getScheduler(),
                builder.sessionPoolMinSize,
                builder.sessionPoolMaxSize,
                builder.sessionPoolIdleDuration
        );
        this.scheduler = builder.transport.getScheduler();
    }

    @Override
    public CompletableFuture<Result<QuerySession>> createSession(Duration timeout) {
        return pool.acquire(timeout);
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public void close() {
        pool.close();
    }

    public static Builder newClient(GrpcTransport transport) {
        return new Builder(transport);
    }

    public static class Builder implements QueryClient.Builder {
        private static final Duration MAX_DURATION = Duration.ofMinutes(30);
        private static final Duration MIN_DURATION = Duration.ofSeconds(1);

        private final GrpcTransport transport;
        private int sessionPoolMinSize = 0;
        private int sessionPoolMaxSize = 50;
        private Duration sessionPoolIdleDuration = Duration.ofMinutes(5);

        Builder(GrpcTransport transport) {
            Preconditions.checkArgument(transport != null, "transport is null");
            this.transport = transport;
        }

        private static String prettyDuration(Duration duration) {
            // convert ISO-8601 format to more readable, etc PT2S will be printed as 2s
            return duration.toString().substring(2).toLowerCase();
        }

        @Override
        public Builder sessionPoolMinSize(int minSize) {
            Preconditions.checkArgument(minSize >= 0, "sessionPoolMinSize(%s) is negative", minSize);
            Preconditions.checkArgument(
                minSize <= sessionPoolMaxSize,
                "sessionPoolMinSize(%s) is greater than sessionPoolMaxSize(%s)",
                minSize, sessionPoolMaxSize);
            this.sessionPoolMinSize = minSize;
            return this;
        }

        @Override
        public Builder sessionPoolMaxSize(int maxSize) {
            Preconditions.checkArgument(maxSize > 0, "sessionPoolMaxSize(%s) is negative or zero", maxSize);
            Preconditions.checkArgument(
                sessionPoolMinSize <= maxSize,
                "sessionPoolMinSize(%s) is greater than sessionPoolMaxSize(%s)",
                sessionPoolMinSize, maxSize);
            this.sessionPoolMaxSize = maxSize;
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

            this.sessionPoolIdleDuration = duration;
            return this;
        }

        @Override
        public QueryClientImpl build() {
            return new QueryClientImpl(this);
        }
    }
}
