package tech.ydb.table;

/**
 * @author Sergey Polovko
 */
public interface SessionPoolStats {
    /**
     * @return Min number of sessions that should remain in session pool after idle session cleanup.
     * This cleanup removes idle sessions that have idle time more than maxIdleTimeMillis.
     * Session pool does not create sessions at startup, so IdleCount can be less than MinSize
     */
    int getMinSize();

    /**
     * @return Max number of sessions in pool.
     * If session pool is full and timeout for acquire is set, acquire task will be put in a queue.
     * If session pool is full and timeout is not set, "too many acquired objects" error will be received.
     */
    int getMaxSize();

    /**
     * @return Number of sessions that were released after use and waiting to be acquired again
     * or to be removed from pool by idle timeout.
     */
    int getIdleCount();

    /**
     * @return Number of sessions currently acquired from pool and not yet released.
     */
    int getAcquiredCount();

    /**
     * @return Number of sessions pending acquire due to pool overflow.
     */
    int getPendingAcquireCount();

    /**
     * @return Total count of sessions received by the client from the pool.
     */
    long getAcquiredTotal();

    /**
     * @return Total count of sessions returned by the client to the pool.
     */
    long getReleasedTotal();

    /**
     * @return Total count of createSession calls, made by the pool.
     */
    long getRequestedTotal();

    /**
     * @return Total count of successful createSession calls, made by the pool
     */
    long getCreatedTotal();

    /**
     * @return Total count of failed createSession calls, made by the pool
     */
    long getFailedTotal();

    /**
     * @return Total count of deleteSession calls, made by the pool.
     */
    long getDeletedTotal();
}
