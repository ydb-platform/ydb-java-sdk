package tech.ydb.table.impl;

import java.util.concurrent.TimeUnit;


/**
 * @author Sergey Polovko
 */
final class SessionPoolOptions {

    static final SessionPoolOptions DEFAULT = new SessionPoolOptions(
        10,                           // minSize
        50,                           // maxSize
        TimeUnit.MINUTES.toMillis(5), // keepAliveTimeMillis
        TimeUnit.MINUTES.toMillis(1), // maxIdleTimeMillis
        5);                           // creationMaxRetries

    private final int minSize;
    private final int maxSize;
    private final long keepAliveTimeMillis;
    private final long maxIdleTimeMillis;
    private final int creationMaxRetries;

    SessionPoolOptions(
        int minSize,
        int maxSize,
        long keepAliveTimeMillis,
        long maxIdleTimeMillis,
        int creationMaxRetries)
    {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.keepAliveTimeMillis = keepAliveTimeMillis;
        this.maxIdleTimeMillis = maxIdleTimeMillis;
        this.creationMaxRetries = creationMaxRetries;
    }

    int getMinSize() {
        return minSize;
    }

    int getMaxSize() {
        return maxSize;
    }

    long getKeepAliveTimeMillis() {
        return keepAliveTimeMillis;
    }

    long getMaxIdleTimeMillis() {
        return maxIdleTimeMillis;
    }

    int getCreationMaxRetries() {
        return creationMaxRetries;
    }

    SessionPoolOptions withSize(int minSize, int maxSize) {
        return new SessionPoolOptions(minSize, maxSize, keepAliveTimeMillis, maxIdleTimeMillis, creationMaxRetries);
    }

    SessionPoolOptions withKeepAliveTimeMillis(long timeMillis) {
        return new SessionPoolOptions(minSize, maxSize, timeMillis, maxIdleTimeMillis, creationMaxRetries);
    }

    SessionPoolOptions withMaxIdleTimeMillis(long timeMillis) {
        return new SessionPoolOptions(minSize, maxSize, keepAliveTimeMillis, timeMillis, creationMaxRetries);
    }

    SessionPoolOptions withCreationMaxRetries(int maxRetries) {
        return new SessionPoolOptions(minSize, maxSize, keepAliveTimeMillis, maxIdleTimeMillis, maxRetries);
    }
}
