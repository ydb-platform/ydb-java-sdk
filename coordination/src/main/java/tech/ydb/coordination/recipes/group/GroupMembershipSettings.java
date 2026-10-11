package tech.ydb.coordination.recipes.group;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.common.retry.RetryUntilElapsed;

public class GroupMembershipSettings {
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    public static final RetryUntilElapsed DEFAULT_RETRY_POLICY = new RetryUntilElapsed(
            DEFAULT_CONNECT_TIMEOUT.toMillis(), 250, 5
    );

    private final ScheduledExecutorService scheduledExecutor;
    private final RetryPolicy retryPolicy;

    public GroupMembershipSettings(Builder builder) {
        this.scheduledExecutor = builder.scheduledExecutor != null ? builder.scheduledExecutor :
                Executors.newSingleThreadScheduledExecutor();
        this.retryPolicy = builder.retryPolicy;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private RetryPolicy retryPolicy = DEFAULT_RETRY_POLICY;
        private ScheduledExecutorService scheduledExecutor;

        public Builder withRetryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder withScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            return this;
        }

        public GroupMembershipSettings build() {
            return new GroupMembershipSettings(this);
        }
    }
}

