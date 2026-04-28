package tech.ydb.core.metrics;

import tech.ydb.core.Status;

public interface Meter {
    void recordOperation(String name, long durationNanos, Status status);
    void registerSessionPool(String poolName, SessionPoolObserver observer);
    void recordSessionCreateTime(String poolName, long durationNanos);
}
