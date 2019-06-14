package tech.ydb.table.settings;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


/**
 * @author Sergey Polovko
 */
class RequestSettings<Self extends RequestSettings> {

    private String traceId;
    private long deadlineAfter;

    public String getTraceId() {
        return traceId;
    }

    public Self setTraceId(String traceId) {
        this.traceId = traceId;
        return self();
    }

    public long getDeadlineAfter() {
        return deadlineAfter;
    }

    public Self setTimeout(Duration duration) {
        if (duration.compareTo(Duration.ZERO) > 0) {
            this.deadlineAfter = System.nanoTime() + duration.toNanos();
        }
        return self();
    }

    public Self setTimeout(long duration, TimeUnit unit) {
        if (duration > 0) {
            this.deadlineAfter = System.nanoTime() + unit.toNanos(duration);
        }
        return self();
    }

    /**
     * Sets an instantaneous point on the time-line after which there is no reason to process request.
     *
     * @param deadlineAfter  the number of nanoseconds from the UNIX-epoch
     * @return this
     */
    public Self setDeadlineAfter(long deadlineAfter) {
        this.deadlineAfter = Math.max(0, deadlineAfter);
        return self();
    }

    @SuppressWarnings("unchecked")
    private Self self() {
        return (Self) this;
    }
}
