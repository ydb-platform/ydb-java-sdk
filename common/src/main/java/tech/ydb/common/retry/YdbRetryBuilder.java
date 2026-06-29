package tech.ydb.common.retry;

/**
 *
 * @author Aleksandr Gorshenin
 */
class YdbRetryBuilder implements RetryConfig.Builder {
    private boolean idempotent = false;
    private boolean retryNotFound = false;

    private long fastBackoff = 5;
    private int fastCeiling = 10;

    private long slowBackoff = 500;
    private int slowCeiling = 6;

    @Override
    public YdbRetryBuilder retryConditionallyRetryableErrors(boolean retry) {
        this.idempotent = retry;
        return this;
    }

    @Override
    public YdbRetryBuilder retryNotFound(boolean retry) {
        this.retryNotFound = retry;
        return this;
    }

    @Override
    public YdbRetryBuilder withSlowBackoff(long backoff, int ceiling) {
        this.slowBackoff = backoff;
        this.slowCeiling = ceiling;
        return this;
    }

    @Override
    public YdbRetryBuilder withFastBackoff(long backoff, int ceiling) {
        this.fastBackoff = backoff;
        this.fastCeiling = ceiling;
        return this;
    }

    @Override
    public RetryConfig retryForever() {
        return new YdbRetryConfig(idempotent, retryNotFound,
                new InstantRetryForever(),
                new ExponentialBackoffRetry(fastBackoff, fastCeiling),
                new ExponentialBackoffRetry(slowBackoff, slowCeiling)
        );
    }

    @Override
    public RetryConfig retryNTimes(int maxRetries) {
        return new YdbRetryConfig(idempotent, retryNotFound,
                new InstantRetryNTimes(maxRetries),
                new RetryNTimes(maxRetries, fastBackoff, fastCeiling),
                new RetryNTimes(maxRetries, slowBackoff, slowCeiling)
        );
    }

    @Override
    public RetryConfig retryUntilElapsed(long maxElapsedMs) {
        return new YdbRetryConfig(idempotent, retryNotFound,
                new InstantRetryUntil(maxElapsedMs),
                new RetryUntilElapsed(maxElapsedMs, fastBackoff, fastCeiling),
                new RetryUntilElapsed(maxElapsedMs, slowBackoff, slowCeiling)
        );
    }

    private static class InstantRetryForever implements RetryPolicy {
        @Override
        public long nextRetryMs(int retryCount, long elapsedTimeMs) {
            return 0;
        }
    }

    private static class InstantRetryNTimes implements RetryPolicy {
        private final int maxRetries;

        InstantRetryNTimes(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public long nextRetryMs(int retryCount, long elapsedTimeMs) {
            return retryCount >= maxRetries ? -1 : 0;
        }
    }

    private static class InstantRetryUntil implements RetryPolicy {
        private final long maxElapsedMs;

        InstantRetryUntil(long maxElapsedMs) {
            this.maxElapsedMs = maxElapsedMs;
        }

        @Override
        public long nextRetryMs(int retryCount, long elapsedTimeMs) {
            return elapsedTimeMs > maxElapsedMs ? -1 : 0;
        }
    }
}
