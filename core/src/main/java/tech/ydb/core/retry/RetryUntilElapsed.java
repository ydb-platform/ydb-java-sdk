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
     * Return maximal count of elapsed milliseconds
     * @return maximal count of elapsed milliseconds
     */
    public long getMaxElapsedMillis() {
        return maxElapsedMs;
    }

    /**
     * Create new retry policy with specified count of elapsed milliseconds
     * @param maxElapsedMs new value of max elapsed milliseconds
     * @return updated retry policy */
    public RetryUntilElapsed withMaxElapsedMs(long maxElapsedMs) {
        return new RetryUntilElapsed(maxElapsedMs, getBackoffMillis(), getBackoffCeiling());
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
