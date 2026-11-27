package tech.ydb.table.impl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.Meter;

public final class RetryMetrics {
    private static final String OPERATION_NAME_ATTR = "operation.name";
    private static final Attr[] NO_ATTRS = new Attr[0];
    private static final double NANOS_IN_SECOND = 1_000_000_000.0;
    private static final List<Double> DURATION_BUCKETS = Arrays.asList(
            0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1d, 2d, 5d, 10d, 30d);
    private static final List<Double> ATTEMPTS_BUCKETS = Arrays.asList(1d, 2d, 3d, 4d, 5d, 7d, 10d, 20d);

    private final DoubleHistogram duration;
    private final DoubleHistogram attempts;

    public RetryMetrics(Meter meter) {
        this.duration = meter.createHistogram("ydb.client.retry.duration", "s",
                "Total user-visible duration of a logical operation executed through the retry policy, "
                        + "including all attempts and back-off delays.",
                DURATION_BUCKETS);
        this.attempts = meter.createHistogram("ydb.client.retry.attempts", "{attempt}",
                "Total number of attempts performed by the retry policy for one logical operation. "
                        + "A value of 1 means the operation succeeded on the first try.",
                ATTEMPTS_BUCKETS);
    }

    public Operation startOperation(@Nullable String operationName) {
        return new Operation(operationName);
    }

    public final class Operation {
        private final long startNanos = System.nanoTime();
        private final AtomicInteger attemptCount = new AtomicInteger();
        private final Attr[] attrs;

        private Operation(@Nullable String operationName) {
            this.attrs = operationName != null
                    ? new Attr[]{Attr.of(OPERATION_NAME_ATTR, operationName)}
                    : NO_ATTRS;
        }

        public void onAttempt() {
            attemptCount.incrementAndGet();
        }

        public void finish() {
            duration.record((System.nanoTime() - startNanos) / NANOS_IN_SECOND, attrs);
            attempts.record(attemptCount.get(), attrs);
        }
    }
}
