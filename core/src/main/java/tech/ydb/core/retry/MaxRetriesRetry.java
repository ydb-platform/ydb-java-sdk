package tech.ydb.core.retry;

import java.util.Random;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class MaxRetriesRetry extends ExponentialBackoffRetry {
    private final int maxRetries;

    public MaxRetriesRetry(int maxRetries, int backoffMs, int backoffCeiling) {
        super(backoffMs, backoffCeiling);
        this.maxRetries = maxRetries;
    }

    @Override
    long nextRetryMs(int retryCount, long elapsedTimeMs, Random random) {
        if (retryCount < maxRetries) {
            return -1;
        }
        return backoffTimeMillis(retryCount, random);
    }
}
