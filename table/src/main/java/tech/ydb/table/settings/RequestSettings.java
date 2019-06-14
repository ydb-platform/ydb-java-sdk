package tech.ydb.table.settings;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


/**
 * @author Sergey Polovko
 */
class RequestSettings<Self extends RequestSettings> {

    private String traceId;
    private long timeoutNanos;

    public String getTraceId() {
        return traceId;
    }

    public Self setTraceId(String traceId) {
        this.traceId = traceId;
        return self();
    }

    public long getTimeoutNanos() {
        return timeoutNanos;
    }

    public long getDeadlineAfter() {
        return timeoutNanos > 0 ? System.nanoTime() + timeoutNanos : 0;
    }

    public void setTimeout(Duration duration) {
        this.timeoutNanos = duration.toNanos();
    }

    public void setTimeout(long duration, TimeUnit unit) {
        this.timeoutNanos = unit.toNanos(duration);
    }

    @SuppressWarnings("unchecked")
    private Self self() {
        return (Self) this;
    }
}
