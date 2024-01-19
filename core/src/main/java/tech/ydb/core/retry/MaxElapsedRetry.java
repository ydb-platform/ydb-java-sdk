package tech.ydb.core.retry;

import java.util.Random;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class MaxElapsedRetry extends ExponentialBackoffRetry {
    private final long maxElapsedMs;

    public MaxElapsedRetry(long maxElapsedMs, int backoffMs, int backoffCeiling) {
        super(backoffMs, backoffCeiling);
        this.maxElapsedMs = maxElapsedMs;
    }

    @Override
    long nextRetryMs(int retryCount, long elapsedTimeMs, Random random) {
        if (elapsedTimeMs > maxElapsedMs) {
            return -1;
        }
        long backoff = backoffTimeMillis(retryCount, random);
        return (elapsedTimeMs + backoff < maxElapsedMs) ? backoff : maxElapsedMs - elapsedTimeMs;
    }
}
