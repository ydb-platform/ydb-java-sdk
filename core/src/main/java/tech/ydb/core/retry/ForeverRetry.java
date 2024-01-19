package tech.ydb.core.retry;

import tech.ydb.core.RetryPolicy;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ForeverRetry implements RetryPolicy {
    private final long intervalMs;

    public ForeverRetry(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    @Override
    public long nextRetryMs(int retryCount, long elapsedTimeMs) {
        return intervalMs;
    }
}
