package tech.ydb.coordination.settings;

import java.time.Duration;
import java.util.concurrent.Executor;

import tech.ydb.core.RetryPolicy;
import tech.ydb.core.retry.RetryUntilElapsed;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CoordinationSessionSettings {
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    public static final RetryUntilElapsed DEFAULT_RETRY_POLICY = new RetryUntilElapsed(
            DEFAULT_CONNECT_TIMEOUT.toMillis(), 250, 5
    );

    private final Executor executor;
    private final Duration connectTimeout;
    private final RetryPolicy retryPolicy;

    private CoordinationSessionSettings(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.executor = builder.executor;
        this.retryPolicy = builder.retryPolicy;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Executor getExecutor() {
        return executor;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Executor executor = null;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private RetryPolicy retryPolicy = DEFAULT_RETRY_POLICY;

        public Builder withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder withConnectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public Builder withRetryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public CoordinationSessionSettings build() {
            return new CoordinationSessionSettings(this);
        }
    }
}
