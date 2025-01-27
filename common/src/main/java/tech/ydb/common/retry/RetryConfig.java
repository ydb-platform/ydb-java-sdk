package tech.ydb.common.retry;

import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;

/**
 * Recipes should use the retry configuration to decide how to retry
 * errors like unsuccessful {@link tech.ydb.core.StatusCode}.
 *
 * @author Aleksandr Gorshenin
 */
@FunctionalInterface
public interface RetryConfig {

    /**
     * Returns retry policy for the given status code and {@code null} if that status code is not retryable
     *
     * @param code status code to check
     * @return policy of retries or {@code null} if the status code is not retryable
     */
    RetryPolicy isStatusRetryable(StatusCode code);

    /**
     * Returns retry policy for the given exception and {@code null} if that exception is not retryable
     *
     * @param th exception to check
     * @return policy of retries or {@code null} if the exception is not retryable
     */
    default RetryPolicy isThrowableRetryable(Throwable th) {
        for (Throwable ex = th; ex != null; ex = ex.getCause()) {
            if (ex instanceof UnexpectedResultException) {
                return isStatusRetryable(((UnexpectedResultException) ex).getStatus().getCode());
            }
        }
        return null;
    }

    /**
     * Retries a non idempotent operation forever with default exponential delay
     * @return retry configuration object
     */
    static RetryConfig retryForever() {
        return newConfig().retryForever();
    }

    /**
     * Retries a non idempotent operation with default exponential until the specified elapsed milliseconds expire
     * @param maxElapsedMs maximum timeout for retries
     * @return retry configuration object
     */
    static RetryConfig retryUntilElapsed(long maxElapsedMs) {
        return newConfig().retryUntilElapsed(maxElapsedMs);
    }

    /**
     * Retries an idempotent operation forever with default exponential delay
     * @return retry configuration object
     */
    static RetryConfig idempotentRetryForever() {
        return newConfig().retryIdempotent(true).retryForever();
    }

    /**
     * Retries an idempotent operation with default exponential until the specified elapsed milliseconds expire
     * @param maxElapsedMs maximum timeout for retries
     * @return retry configuration object
     */
    static RetryConfig idempotentRetryUntilElapsed(long maxElapsedMs) {
        return newConfig().retryIdempotent(true).retryUntilElapsed(maxElapsedMs);
    }

    /**
     * Disabled retries configuration. Any error is considered as non retryable
     * @return retry configuration object
     */
    static RetryConfig noRetries() {
        return (StatusCode code) -> null;
    }

    /**
     * Create a new custom configuration of retries
     * @return retry configuration builder
     */
    static Builder newConfig() {
        return new YdbRetryBuilder();
    }

    interface Builder {
        Builder retryIdempotent(boolean retry);
        Builder retryNotFound(boolean retry);
        Builder withSlowBackoff(long backoff, int ceiling);
        Builder withFastBackoff(long backoff, int ceiling);

        RetryConfig retryForever();
        RetryConfig retryNTimes(int maxRetries);
        RetryConfig retryUntilElapsed(long maxElapsedMs);
    }
}
