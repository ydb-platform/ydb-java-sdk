package tech.ydb.core.metrics;

public interface SessionPoolObserver {
    int getIdleCount();
    int getUsedCount();
    int getPendingCount();
}
