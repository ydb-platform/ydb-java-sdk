package tech.ydb.core.retry;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class RetryNTimes extends ExponentialBackoffRetryPolicy {
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
}
