package tech.ydb.table;

/**
 * @author Sergey Polovko
 */
public class SessionPoolStats {
    private final int minSize;
    private final int maxSize;
    private final int idleCount;
    private final int acquiredCount;
    private final int pendingAcquireCount;

    public SessionPoolStats(
        int minSize,
        int maxSize,
        int idleCount,
        int acquiredCount,
        int pendingAcquireCount) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.idleCount = idleCount;
        this.acquiredCount = acquiredCount;
        this.pendingAcquireCount = pendingAcquireCount;
    }

    /**
     * @return Min number of sessions that should remain in session pool after idle session cleanup.
     * This cleanup removes idle sessions that have idle time more than maxIdleTimeMillis.
     * Session pool does not create sessions at startup, so IdleCount can be less than MinSize
     */
    public int getMinSize() {
        return minSize;
    }

    /**
     * @return Max number of sessions in pool.
     * If session pool is full and timeout for acquire is set, acquire task will be put in a queue.
     * If session pool is full and timeout is not set, "too many acquired objects" error will be received.
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * @return Number of sessions that were released after use and waiting to be acquired again
     * or to be removed from pool by idle timeout.
     */
    public int getIdleCount() {
        return idleCount;
    }

    /**
     * @return Number of sessions currently acquired from pool and not yet released.
     */
    public int getAcquiredCount() {
        return acquiredCount;
    }

    /**
     * @return Number of sessions pending acquire due to pool overflow.
     */
    public int getPendingAcquireCount() {
        return pendingAcquireCount;
    }

    @Override
    public String toString() {
        return "SessionPoolStats{minSize=" + minSize +
            ", maxSize=" + maxSize +
            ", idleCount=" + idleCount +
            ", acquiredCount=" + acquiredCount +
            ", pendingAcquireCount=" + pendingAcquireCount +
            '}';
    }
}
