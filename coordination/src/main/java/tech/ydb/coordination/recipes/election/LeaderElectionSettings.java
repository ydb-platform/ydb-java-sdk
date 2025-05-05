package tech.ydb.coordination.recipes.election;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.common.retry.RetryUntilElapsed;

public class LeaderElectionSettings {
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    public static final RetryUntilElapsed DEFAULT_RETRY_POLICY = new RetryUntilElapsed(
            DEFAULT_CONNECT_TIMEOUT.toMillis(), 250, 5
    );

    private final ExecutorService executorService;
    private final RetryPolicy retryPolicy;

    public LeaderElectionSettings(Builder builder) {
        this.executorService = builder.executorService;
        this.retryPolicy = builder.retryPolicy;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private ExecutorService executorService = Executors.newSingleThreadExecutor();
        private RetryPolicy retryPolicy = DEFAULT_RETRY_POLICY;

        public Builder withExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
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
