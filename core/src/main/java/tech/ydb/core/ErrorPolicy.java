package tech.ydb.core;

/**
 * Recipes should use the configured error policy to decide how to retry
 * errors like unsuccessful {@link tech.ydb.core.StatusCode}.
 *
 * @author Aleksandr Gorshenin
 * @param <T> Type of errors to check
 */
public interface ErrorPolicy<T> {

    /**
     * Returns true if the given value should be retried
     *
     * @param value value to check
     * @return true if value is retryable
     */
    boolean isRetryable(T value);

    /**
     * Returns true if the given exception should be retried
     * Usually exceptions are never retried, but some policies can implement more difficult logic
     *
     * @param ex exception to check
     * @return true if exception is retryable
     */
    default boolean isRetryable(Exception ex) {
        return false;
    }
}
