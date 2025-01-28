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
    RetryPolicy getStatusCodeRetryPolicy(StatusCode code);

    /**
     * Returns retry policy for the given exception and {@code null} if that exception is not retryable
     *
     * @param th exception to check
     * @return policy of retries or {@code null} if the exception is not retryable
     */
    default RetryPolicy getThrowableRetryPolicy(Throwable th) {
        for (Throwable ex = th; ex != null; ex = ex.getCause()) {
            if (ex instanceof UnexpectedResultException) {
                return getStatusCodeRetryPolicy(((UnexpectedResultException) ex).getStatus().getCode());
            }
        }
        return null;
    }

    /**
     * Infinity retries with default exponential delay.<br>That policy <b>does not</b> retries <i>conditionally</i>
     * retryable errors so it can be used for both as idempotent and non idempotent operations
     *
     * @return retry configuration object
     */
    static RetryConfig retryForever() {
        return newConfig().retryForever();
    }

    /**
     * Retries until the specified elapsed milliseconds expire.<br>That policy <b>does not</b> retries
     * <i>conditionally</i> retryable errors so it can be used for both as idempotent and non idempotent operations
     * @param maxElapsedMs maximum timeout for retries
     * @return retry configuration object
     */
    static RetryConfig retryUntilElapsed(long maxElapsedMs) {
        return newConfig().retryUntilElapsed(maxElapsedMs);
    }

    /**
     * Infinity retries with default exponential delay.<br>That policy <b>does</b> retries <i>conditionally</i>
     * retryable errors so it can be used <b>ONLY</b> for idempotent operations
     * @return retry configuration object
     */
    static RetryConfig idempotentRetryForever() {
        return newConfig().retryConditionallyRetryableErrors(true).retryForever();
    }

    /**
     * Retries until the specified elapsed milliseconds expire.<br>That policy <b>does</b> retries
     * <i>conditionally</i> retryable errors so it can be used <b>ONLY</b> for idempotent operations
     * @param maxElapsedMs maximum timeout for retries
     * @return retry configuration object
     */
    static RetryConfig idempotentRetryUntilElapsed(long maxElapsedMs) {
        return newConfig().retryConditionallyRetryableErrors(true).retryUntilElapsed(maxElapsedMs);
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
        Builder retryConditionallyRetryableErrors(boolean retry);
        Builder retryNotFound(boolean retry);
        Builder withSlowBackoff(long backoff, int ceiling);
        Builder withFastBackoff(long backoff, int ceiling);

        RetryConfig retryForever();
        RetryConfig retryNTimes(int maxRetries);
        RetryConfig retryUntilElapsed(long maxElapsedMs);
    }
}
