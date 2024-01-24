package tech.ydb.coordination.settings;

import java.time.Duration;
import java.util.concurrent.Executor;

import tech.ydb.core.RetryPolicy;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CoordinationSessionSettings {
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
        private Duration connectTimeout = Duration.ofSeconds(5);
        private RetryPolicy retryPolicy = null;

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
