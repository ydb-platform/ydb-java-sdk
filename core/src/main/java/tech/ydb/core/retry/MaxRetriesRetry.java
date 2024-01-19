package tech.ydb.core.retry;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class MaxRetriesRetry extends ExponentialBackoffRetry {
    private final int maxRetries;

    public MaxRetriesRetry(int maxRetries, long backoffMs, int backoffCeiling) {
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
