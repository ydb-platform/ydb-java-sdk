package tech.ydb.core.retry;

import tech.ydb.core.RetryPolicy;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class RetryForever implements RetryPolicy {
    private final long intervalMs;

    public RetryForever(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    @Override
    public long nextRetryMs(int retryCount, long elapsedTimeMs) {
        return intervalMs;
    }

    /**
     * Return current interval of retries
     * @return retry interval in milliseconds
     */
    public long getIntervalMillis() {
        return intervalMs;
    }

    /**
     * Create new retry policy with specified retry interval
     * @param ms new interval in milliseconds
     * @return updated retry policy */
    public RetryForever withIntervalMs(long ms) {
        return new RetryForever(ms);
    }
}
