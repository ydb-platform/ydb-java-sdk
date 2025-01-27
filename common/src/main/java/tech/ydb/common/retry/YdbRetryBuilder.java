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
    public YdbRetryBuilder retryIdempotent(boolean retry) {
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
                (int retryCount, long elapsedTimeMs) -> 0,
                new ExponentialBackoffRetry(fastBackoff, fastCeiling),
                new ExponentialBackoffRetry(slowBackoff, slowCeiling)
        );
    }

    @Override
    public RetryConfig retryNTimes(int maxRetries) {
        return new YdbRetryConfig(idempotent, retryNotFound,
                (int retryCount, long elapsedTimeMs) -> retryCount >= maxRetries ? -1 : 0,
                new RetryNTimes(maxRetries, fastBackoff, fastCeiling),
                new RetryNTimes(maxRetries, slowBackoff, slowCeiling)
        );
    }

    @Override
    public RetryConfig retryUntilElapsed(long maxElapsedMs) {
        return new YdbRetryConfig(idempotent, retryNotFound,
                (int retryCount, long elapsedTimeMs) -> elapsedTimeMs > maxElapsedMs ? -1 : 0,
                new RetryUntilElapsed(maxElapsedMs, fastBackoff, fastCeiling),
                new RetryUntilElapsed(maxElapsedMs, slowBackoff, slowCeiling)
        );
    }
}
