package tech.ydb.coordination.settings;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CoordinationSessionSettings {
    private final Duration createTimeout;
    private final Executor executor;
    private final int retriesCount;

    private CoordinationSessionSettings(Builder builder) {
        this.createTimeout = builder.createTimeout;
        this.executor = builder.executor;
        this.retriesCount = builder.retriesCount;
    }

    public Duration getCreateTimeout() {
        return createTimeout;
    }

    public Executor getExecutor() {
        return executor;
    }

    public int getRetriesCount() {
        return retriesCount;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Duration createTimeout = Duration.ofSeconds(5);
        private Executor executor = null;
        private int retriesCount = 3;

        public Builder withCreateTimeout(Duration duration) {
            this.createTimeout = duration;
            return this;
        }

        public Builder withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder withRetriesCount(int retriesCount) {
            this.retriesCount = retriesCount;
            return this;
        }

        public CoordinationSessionSettings build() {
            return new CoordinationSessionSettings(this);
        }
    }
}
