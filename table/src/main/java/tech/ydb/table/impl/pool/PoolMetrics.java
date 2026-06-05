package tech.ydb.table.impl.pool;

import tech.ydb.core.Status;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;

public final class PoolMetrics {
    private static final String UNIT = "{session}";
    private static final double NANOS_IN_SECOND = 1_000_000_000.0;

    private final Attr poolNameAttr;
    private final Attr[] poolAttrs;
    private final Attr[] idleAttrs;
    private final Attr[] inUseAttrs;
    private final String statusKey;

    private final LongCounter created;
    private final LongCounter deleted;
    private final LongCounter acquired;
    private final LongCounter released;
    private final LongCounter requested;
    private final LongCounter failed;
    private final DoubleHistogram createTime;

    public PoolMetrics(Meter meter, String name, String poolName, WaitingQueue<?> queue, int minSize) {
        String prefix = "ydb." + name + ".session.";
        this.statusKey = prefix + "status_code";
        this.poolNameAttr = Attr.of(prefix + "pool.name", poolName);

        this.poolAttrs = new Attr[]{poolNameAttr};
        this.idleAttrs = new Attr[]{poolNameAttr, Attr.of(prefix + "state", "idle")};
        this.inUseAttrs = new Attr[]{poolNameAttr, Attr.of(prefix + "state", "in_use")};

        this.created = meter.createCounter(prefix + "created", UNIT, "Total successful session creations.");
        this.deleted = meter.createCounter(prefix + "deleted", UNIT, "Total session deletions.");
        this.acquired = meter.createCounter(prefix + "acquired", UNIT, "Total session acquires from the pool.");
        this.released = meter.createCounter(prefix + "released", UNIT, "Total session releases back to the pool.");
        this.requested = meter.createCounter(prefix + "requested", UNIT, "Total CreateSession calls.");
        this.failed = meter.createCounter(prefix + "failed", UNIT, "Total failed session creations.");
        this.createTime = meter.createHistogram(prefix + "create_time", "s", "Session creation cost.");

        meter.createLongGauge(prefix + "max", UNIT, "Configured MaxPoolSize",
                m -> m.record(queue.getTotalLimit(), poolAttrs));
        meter.createLongGauge(prefix + "min", UNIT, "Configured MinPoolSize",
                m -> m.record(minSize, poolAttrs));
        meter.createLongGauge(prefix + "count", UNIT, "Current pool session counts", m -> {
            int total = queue.getTotalCount();
            int idle = queue.getIdleCount();
            m.record(idle, idleAttrs);
            m.record(total - idle, inUseAttrs);
        });
        meter.createLongGauge(prefix + "pending_requests", UNIT, "Requests waiting for a session.",
                m -> m.record(queue.getWaitingCount() + queue.getPendingCount(), poolAttrs));
    }

    public void onSessionRequested() {
        requested.add(1L, poolAttrs);
    }

    public void onCreateTime(long createTimeNanos) {
        createTime.record(createTimeNanos / NANOS_IN_SECOND, poolAttrs);
    }

    public void onSessionCreated() {
        created.add(1L, poolAttrs);
    }

    public void onSessionDeleted() {
        deleted.add(1L, poolAttrs);
    }

    public void onSessionAcquired() {
        acquired.add(1L, poolAttrs);
    }

    public void onSessionReleased() {
        released.add(1L, poolAttrs);
    }

    public void onSessionFailed(Status status) {
        failed.add(1L, new Attr[]{poolNameAttr, Attr.of(statusKey, status.getCode().name())});
    }
}
