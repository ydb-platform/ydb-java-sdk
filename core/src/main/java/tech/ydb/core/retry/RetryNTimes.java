package tech.ydb.core.retry;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class RetryNTimes extends ExponentialBackoffRetry {
    private final int maxRetries;

    public RetryNTimes(int maxRetries, long backoffMs, int backoffCeiling) {
        super(backoffMs, backoffCeiling);
        this.maxRetries = maxRetries;
    }

    @Override
    public long nextRetryMs(int retryCount, long elapsedTimeMs) {
        if (retryCount >= maxRetries) {
            return -1;
        }
        return backoffTimeMillis(retryCount);
    }

    /**
     * Create new retry policy with specified backoff duration
     * @param ms new backoff duration in milliseconds
     * @return updated retry policy */
    public RetryNTimes withBackoffMs(long ms) {
        return new RetryNTimes(maxRetries, ms, getBackoffCeiling());
    }

    /**
     * Create new retry policy with specified backoff ceiling
     * @param ceiling new backoff ceiling
     * @return updated retry policy */
    public RetryNTimes withBackoffCeiling(int ceiling) {
        return new RetryNTimes(maxRetries, getBackoffMillis(), ceiling);
    }
}
