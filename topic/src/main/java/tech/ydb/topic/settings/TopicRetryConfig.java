package tech.ydb.topic.settings;

import java.util.EnumSet;

import tech.ydb.common.retry.ExponentialBackoffRetry;
import tech.ydb.common.retry.RetryConfig;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 * Predefined {@link RetryConfig} instances for topic writers and readers.
 * <p>
 * Pass one of these constants (or a custom {@link RetryConfig}) to
 * {@link WriterSettings.Builder#setRetryConfig} to control how the writer
 * behaves when its underlying stream is interrupted.
 *
 * @author Aleksandr Gorshenin
 */
public class TopicRetryConfig {
    // Max backoff will be random delay from 32.768s to 65.536s
    private static final RetryPolicy DEFAULT_BACKOFF = new ExponentialBackoffRetry(32, 10);

    /**
     * Retry any stream disconnection indefinitely with exponential backoff.
     * <p>
     * Every status code, including {@link Status#SUCCESS}, is treated as retryable.
     * The delay between reconnection attempts grows exponentially and is capped at a
     * random value between 32 and 65 seconds.
     * <p>
     * This is the default retry configuration for topic writers and readers.
     */
    public static final RetryConfig FOREVER = status -> DEFAULT_BACKOFF;

    /**
     * Disable retries entirely.
     * <p>
     * Any stream disconnection is reported immediately as a terminal error through
     * the errors handler configured via
     * {@link WriterSettings.Builder#setErrorsHandler}.
     * Use this when you need full control over reconnection logic in application code.
     */
    public static final RetryConfig NEVER = status -> null;

    private static final EnumSet<StatusCode> NON_RETRYABLE = EnumSet.of(
            StatusCode.SCHEME_ERROR,
            StatusCode.BAD_REQUEST,
            StatusCode.UNAUTHORIZED,
            StatusCode.PRECONDITION_FAILED,
            StatusCode.UNSUPPORTED,
            StatusCode.ALREADY_EXISTS,
            StatusCode.NOT_FOUND,
            StatusCode.EXTERNAL_ERROR,
            StatusCode.CLIENT_UNAUTHENTICATED,
            StatusCode.CLIENT_CALL_UNIMPLEMENTED
    );

    /**
     * Retry transient stream disconnections indefinitely with exponential backoff,
     * but treat permanent errors as terminal.
     */
    public static final RetryConfig STANDARD = st -> NON_RETRYABLE.contains(st.getCode()) ? null : DEFAULT_BACKOFF;


    private TopicRetryConfig() { }
}
