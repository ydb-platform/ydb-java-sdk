package tech.ydb.core.retry;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class RetryUntilElapsed extends ExponentialBackoffRetry {
    private final long maxElapsedMs;

    public RetryUntilElapsed(long maxElapsedMs, long backoffMs, int backoffCeiling) {
        super(backoffMs, backoffCeiling);
        this.maxElapsedMs = maxElapsedMs;
    }

    @Override
    public long nextRetryMs(int retryCount, long elapsedTimeMs) {
        if (maxElapsedMs <= elapsedTimeMs) {
            return -1;
        }
        long backoff = backoffTimeMillis(retryCount);
        return (elapsedTimeMs + backoff < maxElapsedMs) ? backoff : maxElapsedMs - elapsedTimeMs;
    }

    /**
     * Create new retry policy with specified backoff duration
     * @param ms new backoff duration
     * @return new retry policy */
    public RetryUntilElapsed withBackoffMs(long ms) {
        return new RetryUntilElapsed(maxElapsedMs, ms, getBackoffCeiling());
    }

    /**
     * Create new retry policy with specified backoff ceiling
     * @param ceiling new backoff ceiling
     * @return new retry policy */
    public RetryUntilElapsed withBackoffCeiling(int ceiling) {
        return new RetryUntilElapsed(maxElapsedMs, getBackoffMillis(), ceiling);
    }
}
