package tech.ydb.core;

/**
 * Abstracts the policy to use when retrying some actions
 *
 * @author Aleksandr Gorshenin
 */
public interface RetryPolicy {
    /**
     * Called when an operation has failed for some reason. This method may return next values
     * positive number N : operation must be retried in N milliseconds
     * zero : operation must be retried immediately
     * negative number : retry is not allowed, operation must be failed
     *
     * @param retryCount the number of times retried so far (0 the first time)
     * @param elapsedTimeMs the elapsed time in ms since the operation was attempted
     * @return number of milliseconds for next retry
     */
    long nextRetryMs(int retryCount, long elapsedTimeMs);
}
