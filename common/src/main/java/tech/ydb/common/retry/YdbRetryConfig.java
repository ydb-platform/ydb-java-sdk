package tech.ydb.common.retry;

import tech.ydb.core.Status;

/**
 *
 * @author Aleksandr Gorshenin
 */
class YdbRetryConfig implements RetryConfig {
    private final boolean retryConditionally;
    private final boolean retryNotFound;
    private final RetryPolicy immediatelly;
    private final RetryPolicy fast;
    private final RetryPolicy slow;

    YdbRetryConfig(boolean conditionally, boolean notFound, RetryPolicy instant, RetryPolicy fast, RetryPolicy slow) {
        this.retryConditionally = conditionally;
        this.retryNotFound = notFound;
        this.immediatelly = instant;
        this.fast = fast;
        this.slow = slow;
    }

    @Override
    public RetryPolicy getStatusRetryPolicy(Status status) {
        if (status == null) {
            return null;
        }

        switch (status.getCode()) {
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

            // Conditionally retryable statuses
            case CLIENT_CANCELLED:
            case CLIENT_INTERNAL_ERROR:
            case TRANSPORT_UNAVAILABLE:
            case UNAVAILABLE:
            case TIMEOUT:
                return retryConditionally ? fast : null;

            // Not found has special flag for retries
            case NOT_FOUND:
                return retryNotFound ? fast : null;

            // All other codes are not retryable
            default:
                return null;
        }
    }
}
