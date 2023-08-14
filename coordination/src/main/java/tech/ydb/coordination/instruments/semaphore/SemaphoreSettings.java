package tech.ydb.coordination.instruments.semaphore;

public class SemaphoreSettings {
    private final long timeout;
    private final long count;

    protected SemaphoreSettings(long timeout, long count) {
        this.timeout = timeout;
        this.count = count;
    }

    public long getTimeout() {
        return timeout;
    }

    public long getCount() {
        return count;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private long timeout;
        private long count = 1;

        public Builder withTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder withCount(long count) {
            this.count = count;
            return this;
        }

        public SemaphoreSettings build() {
            return new SemaphoreSettings(timeout, count);
        }
    }
}
