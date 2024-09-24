package tech.ydb.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.utils.FutureTools;

/**
 * Session retry helper interface to support the application-level monitoring of session operation
 * retries.
 *
 * @author mzinal
 */
public interface SessionRetryHandler {

    Logger LOGGER = LoggerFactory.getLogger(SessionRetryHandler.class);
    SessionRetryHandler DEFAULT = new SessionRetryHandler() {
    };

    /**
     * Called on operation success.
     *
     * @param context Session retry object
     * @param retryNumber Current number of retries
     * @param millis Elapsed time
     */
    default void onSuccess(SessionRetryContext context, int retryNumber, long millis) {
        LOGGER.debug("RetryCtx[{}] OK, finished after {} retries, {} ms total",
                context.hashCode(), retryNumber, millis);
    }

    /**
     * Called on async operation cancellation.
     *
     * @param context Session retry object
     * @param retryNumber Current number of retries
     * @param millis Elapsed time
     */
    default void onCancel(SessionRetryContext context, int retryNumber, long millis) {
        LOGGER.debug("RetryCtx[{}] cancelled, {} retries, {} ms", context.hashCode(),
                retryNumber, millis);
    }

    /**
     * Called on retryable status code.
     *
     * @param context Session retry object
     * @param code Status code
     * @param retryNumber Current number of retries
     * @param next Delay before the operation will be retried.
     * @param millis Elapsed time
     */
    default void onRetry(SessionRetryContext context, StatusCode code,
            int retryNumber, long next, long millis) {
        LOGGER.debug("RetryCtx[{}] RETRYABLE CODE[{}], scheduling next retry #{} in {} ms, {} ms total",
                context.hashCode(), code, retryNumber, next, millis);
    }

    /**
     * Called on retryable exception.
     *
     * @param context Session retry object
     * @param issue Retryable exception
     * @param retryNumber Current number of retries
     * @param next Delay before the operation will be retried.
     * @param millis Elapsed time
     */
    default void onRetry(SessionRetryContext context, Throwable issue,
            int retryNumber, long next, long millis) {
        LOGGER.debug("RetryCtx[{}] RETRYABLE ERROR[{}], scheduling next retry #{} in {} ms, {} ms total",
                context.hashCode(), errorMsg(issue), retryNumber, next, millis);
    }

    /**
     * Called on retryable error when the limit is reached.
     *
     * @param context Session retry object
     * @param code Status code
     * @param retryLimit Maximum number of retries
     * @param millis Elapsed time
     */
    default void onLimit(SessionRetryContext context, StatusCode code,
            int retryLimit, long millis) {
        LOGGER.debug("RetryCtx[{}] RETRYABLE CODE[{}], finished by retries limit ({}), {} ms total",
                context.hashCode(), code, retryLimit, millis);
    }

    /**
     * Called on non-retryable error.
     *
     * @param context Session retry object
     * @param code Status code
     * @param retryNumber Current number of retries
     * @param millis Elapsed time
     */
    default void onError(SessionRetryContext context, StatusCode code,
            int retryNumber, long millis) {
        LOGGER.debug("RetryCtx[{}] NON-RETRYABLE CODE[{}], finished after {} retries, {} ms total",
                context.hashCode(), code, retryNumber, millis);
    }

    /**
     * Called on non-retryable error as Java exception.
     *
     * @param context Session retry object
     * @param issue Exception thrown
     * @param retryNumber Current number of retries
     * @param millis Elapsed time
     */
    default void onError(SessionRetryContext context, Throwable issue,
            int retryNumber, long millis) {
        LOGGER.debug("RetryCtx[{}] NON-RETRYABLE ERROR[{}], finished after {} retries, {} ms total",
                context.hashCode(), errorMsg(issue), retryNumber, millis);
    }

    /**
     * Obtain the error message text from an exception
     * @param t Exception
     * @return Error message
     */
    default String errorMsg(Throwable t) {
        if (!LOGGER.isDebugEnabled()) {
            return "unknown";
        }
        Throwable cause = FutureTools.unwrapCompletionException(t);
        if (cause instanceof UnexpectedResultException) {
            StatusCode statusCode = ((UnexpectedResultException) cause).getStatus().getCode();
            return statusCode.name();
        }
        return t.getMessage();
    }

}
