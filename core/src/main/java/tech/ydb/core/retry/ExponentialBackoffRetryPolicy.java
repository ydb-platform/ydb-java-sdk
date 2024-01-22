package tech.ydb.core.retry;

import java.util.concurrent.ThreadLocalRandom;

import tech.ydb.core.RetryPolicy;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class ExponentialBackoffRetryPolicy implements RetryPolicy {
    private final long backoffMs;
    private final int backoffCeiling;

    protected ExponentialBackoffRetryPolicy(long backoffMs, int backoffCeiling) {
        this.backoffMs = backoffMs;
        this.backoffCeiling = backoffCeiling;
    }

    protected long backoffTimeMillis(int retryNumber) {
        int slots = 1 << Math.min(retryNumber, backoffCeiling);
        long delay = backoffMs * slots;
        return delay + ThreadLocalRandom.current().nextLong(delay);
    }
}
