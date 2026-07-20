package tech.ydb.core.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.metrics.Meter;
import tech.ydb.core.tracing.NoopTracer;
import tech.ydb.core.tracing.Span;
import tech.ydb.core.tracing.SpanKind;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class ObservabilityTest {
    private static final String BASE = "ydb-java-sdk/1.2.3";

    @After
    public void reset() {
        Observability.reset();
    }

    @Test
    public void baseTest() {
        Assert.assertEquals(BASE, Observability.getDiscoveryBuildInfo(BASE));

        // noop implementations ignored
        Observability.reportMetricsUsage(Meter.NOOP);
        Observability.reportTracingUsage(NoopTracer.getInstance());
        Assert.assertEquals(BASE, Observability.getDiscoveryBuildInfo(BASE));

        Observability.reportMetricsUsage(new Meter() { });
        Assert.assertEquals(BASE + ";ydb-sdk-metrics/0.1.0", Observability.getDiscoveryBuildInfo(BASE));

        Observability.reportTracingUsage((String spanName, SpanKind spanKind) -> Span.NOOP);
        Assert.assertEquals(
                BASE + ";ydb-sdk-tracing/0.1.0;ydb-sdk-metrics/0.1.0",
                Observability.getDiscoveryBuildInfo(BASE)
        );

        Observability.reportMetricsUsage(Meter.NOOP);
        Observability.reportTracingUsage(NoopTracer.getInstance());
        Assert.assertEquals(
                BASE + ";ydb-sdk-tracing/0.1.0;ydb-sdk-metrics/0.1.0",
                Observability.getDiscoveryBuildInfo(BASE)
        );
    }
}