package tech.ydb.table.impl.pool;

import java.util.concurrent.TimeUnit;


/**
 * @author Sergey Polovko
 */
final class SessionPoolOptions {

    static final SessionPoolOptions DEFAULT = new SessionPoolOptions(
        10,                            // minSize
        50,                            // maxSize
        TimeUnit.MINUTES.toMillis(5),  // keepAliveTimeMillis
        TimeUnit.MINUTES.toMillis(1)); // maxIdleTimeMillis

    private final int minSize;
    private final int maxSize;
    private final long keepAliveTimeMillis;
    private final long maxIdleTimeMillis;

    SessionPoolOptions(
        int minSize,
        int maxSize,
        long keepAliveTimeMillis,
        long maxIdleTimeMillis)
    {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.keepAliveTimeMillis = keepAliveTimeMillis;
        this.maxIdleTimeMillis = maxIdleTimeMillis;
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

    SessionPoolOptions withSize(int minSize, int maxSize) {
        return new SessionPoolOptions(minSize, maxSize, keepAliveTimeMillis, maxIdleTimeMillis);
    }

    SessionPoolOptions withKeepAliveTimeMillis(long timeMillis) {
        return new SessionPoolOptions(minSize, maxSize, timeMillis, maxIdleTimeMillis);
    }

    SessionPoolOptions withMaxIdleTimeMillis(long timeMillis) {
        return new SessionPoolOptions(minSize, maxSize, keepAliveTimeMillis, timeMillis);
    }
}
