package tech.ydb.core.impl;

import com.google.common.annotations.VisibleForTesting;

import tech.ydb.core.metrics.Meter;
import tech.ydb.core.tracing.NoopTracer;
import tech.ydb.core.tracing.Tracer;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public final class Observability {
    public static final String TRACING_CHAIN = ";ydb-sdk-tracing/0.1.0";
    public static final String METRICS_CHAIN = ";ydb-sdk-metrics/0.1.0";

    private static volatile boolean isTracingEnabled = false;
    private static volatile boolean isMetricsEnabled = false;

    private Observability() {
    }

    public static void reportTracingUsage(Tracer tracer) {
        if (tracer != NoopTracer.getInstance()) {
            isTracingEnabled = true;
        }
    }

    public static void reportMetricsUsage(Meter meter) {
        if (meter != Meter.NOOP) {
            isMetricsEnabled = true;
        }
    }

    static String getDiscoveryBuildInfo(String base) {
        return base + (isTracingEnabled ? TRACING_CHAIN : "") + (isMetricsEnabled ? METRICS_CHAIN : "");
    }

    @VisibleForTesting
    static void reset() {
        isTracingEnabled = false;
        isMetricsEnabled = false;
    }
}
