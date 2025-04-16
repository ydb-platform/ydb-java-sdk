package tech.ydb.common.retry;

import java.util.concurrent.ThreadLocalRandom;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class ExponentialBackoffRetry implements RetryPolicy {
    private final long backoffMs;
    private final int backoffCeiling;

    public ExponentialBackoffRetry(long backoffMs, int backoffCeiling) {
        this.backoffMs = backoffMs;
        this.backoffCeiling = backoffCeiling;
    }

    protected long backoffTimeMillis(int retryNumber) {
        int slots = 1 << Math.min(retryNumber, backoffCeiling);
        long delay = backoffMs * slots;
        return delay + ThreadLocalRandom.current().nextLong(delay);
    }

    @Override
    public long nextRetryMs(int retryCount, long elapsedTimeMs) {
        return backoffTimeMillis(retryCount);
    }

    /**
     * Return current base of backoff delays
     * @return backoff base duration in milliseconds
     */
    public long getBackoffMillis() {
        return backoffMs;
    }

    /**
     * Return current maximal level of backoff exponent
     * @return maximal level of backoff exponent
     */
    public int getBackoffCeiling() {
        return backoffCeiling;
    }

}
