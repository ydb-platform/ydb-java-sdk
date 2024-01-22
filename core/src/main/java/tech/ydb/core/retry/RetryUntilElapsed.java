package tech.ydb.core.retry;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class RetryUntilElapsed extends ExponentialBackoffRetryPolicy {
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
}
