package tech.ydb.table.stats;

/**
 * @author Sergey Polovko
 */
public class SessionPoolStats {

    private final int minSize;
    private final int maxSize;
    private final int idleCount;
    private final int acquiredCount;
    private final int pendingAcquireCount;

    public SessionPoolStats(int minSize, int maxSize, int idleCount, int acquiredCount, int pendingAcquireCount) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.idleCount = idleCount;
        this.acquiredCount = acquiredCount;
        this.pendingAcquireCount = pendingAcquireCount;
    }

    public int getMinSize() {
        return minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getIdleCount() {
        return idleCount;
    }

    public int getAcquiredCount() {
        return acquiredCount;
    }

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
