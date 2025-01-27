package tech.ydb.common.retry;

import tech.ydb.core.StatusCode;

/**
 *
 * @author Aleksandr Gorshenin
 */
class YdbRetryConfig implements RetryConfig {
    private final boolean idempotent;
    private final boolean retryNotFound;
    private final RetryPolicy immediatelly;
    private final RetryPolicy fast;
    private final RetryPolicy slow;

    YdbRetryConfig(boolean idempotent, boolean notFound, RetryPolicy immediatelly, RetryPolicy fast, RetryPolicy slow) {
        this.idempotent = idempotent;
        this.retryNotFound = notFound;
        this.immediatelly = immediatelly;
        this.fast = fast;
        this.slow = slow;
    }

    @Override
    public RetryPolicy isStatusRetryable(StatusCode code) {
        if (code == null) {
            return null;
        }

        switch (code) {
            // Instant retry
            case BAD_SESSION:
            case SESSION_BUSY:
                return immediatelly;

            // Fast backoff
            case ABORTED:
            case UNDETERMINED:
                return fast;

            // Slow backoff
            case OVERLOADED:
            case CLIENT_RESOURCE_EXHAUSTED:
                return slow;

            // Conditionally retry
            case CLIENT_CANCELLED:
            case CLIENT_INTERNAL_ERROR:
            case TRANSPORT_UNAVAILABLE:
            case UNAVAILABLE:
                return idempotent ? fast : null;

            // Not found retry
            case NOT_FOUND:
                return retryNotFound ? fast : null;

            // All other codes are not retryable
            default:
                return null;
        }
    }
}
