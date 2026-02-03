package tech.ydb.common.retry;


import tech.ydb.core.Status;
import tech.ydb.core.UnexpectedResultException;

/**
 * Recipes should use the retry configuration to decide how to retry
 * errors like unsuccessful {@link tech.ydb.core.Status}.
 *
 * @author Aleksandr Gorshenin
 */
@FunctionalInterface
public interface RetryConfig {

    /**
     * Returns retry policy for the given {@link Status} and {@code null} if that status is not retryable
     *
     * @param status status to check
     * @return policy of retries or {@code null} if the status is not retryable
     */
    RetryPolicy getStatusRetryPolicy(Status status);

    /**
     * Returns retry policy for the given exception and {@code null} if that exception is not retryable
     *
     * @param th exception to check
     * @return policy of retries or {@code null} if the exception is not retryable
     */
    default RetryPolicy getThrowableRetryPolicy(Throwable th) {
        for (Throwable ex = th; ex != null; ex = ex.getCause()) {
            if (ex instanceof UnexpectedResultException) {
                return getStatusRetryPolicy(((UnexpectedResultException) ex).getStatus());
            }
        }
        return null;
    }

    /**
     * Infinity retries with default exponential delay.<br>This policy <b>does not</b> retry <i>conditionally</i>
     * retryable errors so it can be used for both as idempotent and non idempotent operations
     *
     * @return retry configuration object
     */
    static RetryConfig retryForever() {
        return newConfig().retryForever();
    }

    /**
     * Retries until the specified elapsed milliseconds expire.<br>This policy <b>does not</b> retry
     * <i>conditionally</i> retryable errors so it can be used for both as idempotent and non idempotent operations
     * @param maxElapsedMs maximum timeout for retries
     * @return retry configuration object
     */
    static RetryConfig retryUntilElapsed(long maxElapsedMs) {
        return newConfig().retryUntilElapsed(maxElapsedMs);
    }

    /**
     * Infinity retries with default exponential delay.<br>This policy <b>does</b> retry <i>conditionally</i>
     * retryable errors so it can be used <b>ONLY</b> for idempotent operations
     * @return retry configuration object
     */
    static RetryConfig idempotentRetryForever() {
        return newConfig().retryConditionallyRetryableErrors(true).retryForever();
    }

    /**
     * Retries until the specified elapsed milliseconds expire.<br>This policy <b>does</b> retry
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
        return (Status status) -> null;
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
