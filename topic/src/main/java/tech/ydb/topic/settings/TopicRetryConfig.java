package tech.ydb.topic.settings;

import tech.ydb.common.retry.ExponentialBackoffRetry;
import tech.ydb.common.retry.RetryConfig;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Status;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TopicRetryConfig {
    // Max backoff will be random delay from 32.768s to 65.536s
    private static final RetryPolicy DEFAULT_BACKOFF = new ExponentialBackoffRetry(32, 10);

    /**
     * Default retry configuration for the topic writers and readers. Any status (even {@link Status#SUCCESS}) will be
     * retried with exponential backoff
     */
    public static final RetryConfig FOREVER = status -> DEFAULT_BACKOFF;

    private TopicRetryConfig() { }
}
