package tech.ydb.coordination.recipes.election;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.common.retry.RetryUntilElapsed;

public class LeaderElectionSettings {
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    public static final RetryUntilElapsed DEFAULT_RETRY_POLICY = new RetryUntilElapsed(
            DEFAULT_CONNECT_TIMEOUT.toMillis(), 250, 5
    );

    private final ScheduledExecutorService scheduledExecutor;
    private final RetryPolicy retryPolicy;

    public LeaderElectionSettings(Builder builder) {
        this.scheduledExecutor = builder.scheduledExecutor != null ? builder.scheduledExecutor :
                Executors.newSingleThreadScheduledExecutor();
        this.retryPolicy = builder.retryPolicy;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private ScheduledExecutorService scheduledExecutor;
        private RetryPolicy retryPolicy = DEFAULT_RETRY_POLICY;

        public Builder withScheduledExecutor(ScheduledExecutorService executorService) {
            this.scheduledExecutor = executorService;
            return this;
        }

        public Builder withRetryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public LeaderElectionSettings build() {
            return new LeaderElectionSettings(this);
        }
    }
}
