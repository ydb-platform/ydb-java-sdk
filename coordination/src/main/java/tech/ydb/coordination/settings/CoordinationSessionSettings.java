package tech.ydb.coordination.settings;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CoordinationSessionSettings {
    private final Executor executor;
    private final Duration connectTimeout;
    private final Duration reconnectBackoffDelay;

    private CoordinationSessionSettings(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.executor = builder.executor;
        this.reconnectBackoffDelay = builder.reconnectBackoffDelay;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Duration getReconnectBackoffDelay() {
        return reconnectBackoffDelay;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Executor executor = null;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration reconnectBackoffDelay = Duration.ofMillis(250);

        public Builder withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder withConnectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public CoordinationSessionSettings build() {
            return new CoordinationSessionSettings(this);
        }
    }
}
