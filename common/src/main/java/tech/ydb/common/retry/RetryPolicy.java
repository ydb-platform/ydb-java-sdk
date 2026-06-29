package tech.ydb.common.retry;

/**
 * Abstracts the policy to use when retrying some actions
 *
 * @author Aleksandr Gorshenin
 */
@FunctionalInterface
public interface RetryPolicy {
    /**
     * Called when an operation is failed for some reason to determine if it should be retried.
     * And if so, returns the delay to make the next retry attempt after
     *
     * @param retryCount the number of times retried so far (0 the first time)
     * @param elapsedTimeMs the elapsed time in ms since the operation was attempted
     * @return delay for the next retry
     * <ul>
     * <li>Positive number N - operation must be retried in N milliseconds </li>
     * <li>Zero : operation must be retried immediately </li>
     * <li>Negative number : retry is not allowed, operation must be failed </li>
     * </ul>
     */
    long nextRetryMs(int retryCount, long elapsedTimeMs);
}
