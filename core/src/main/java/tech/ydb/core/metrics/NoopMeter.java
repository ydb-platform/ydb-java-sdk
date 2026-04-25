package tech.ydb.core.metrics;

import tech.ydb.core.Status;

public final class NoopMeter implements Meter {
    public static final NoopMeter INSTANCE = new NoopMeter();

    private NoopMeter() {
        // No operations.
    }

    public static NoopMeter getInstance() {
        return INSTANCE;
    }

    @Override
    public void recordOperation(String name, long durationNanos, Status status) {

    }

    @Override
    public void registerSessionPool(String poolName, SessionPoolObserver observer) {

    }

    @Override
    public void recordSessionCreateTime(String poolName, long durationNanos) {

    }
}
