package tech.ydb.topic.settings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public enum RetryMode {
    /** Never retry stream. After any error reader/writer will be stopped */
    NONE,

    /** Don't retry first attempt to establish a stream, but after successful connection retry any error until stop */
    RECOVER,

    /** Retry any error until reader/writer was stopped by client */
    ALWAYS,
}
