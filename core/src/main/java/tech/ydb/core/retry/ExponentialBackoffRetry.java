package tech.ydb.core.retry;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.annotations.VisibleForTesting;

import tech.ydb.core.RetryPolicy;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class ExponentialBackoffRetry implements RetryPolicy {
    private final int backoffMs;
    private final int backoffCeiling;

    protected ExponentialBackoffRetry(int backoffMs, int backoffCeiling) {
        this.backoffMs = backoffMs;
        this.backoffCeiling = backoffCeiling;
    }

    protected long backoffTimeMillis(int retryNumber, Random random) {
        int slots = 1 << Math.min(retryNumber, backoffCeiling);
        int delay = backoffMs * slots;
        return delay + random.nextInt(delay);
    }

    @Override
    public long nextRetryMs(int retryCount, long elapsedTimeMs) {
        return nextRetryMs(retryCount, elapsedTimeMs, ThreadLocalRandom.current());
    }

    @VisibleForTesting
    abstract long nextRetryMs(int retryCount, long elapsedTimeMs, Random random);
}
