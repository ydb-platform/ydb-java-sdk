package tech.ydb.core.grpc;

import org.junit.Assert;
import org.junit.Test;

public class ObservabilityTest {

    private static final String BASE = "ydb-java-sdk/1.2.3";

    @Test
    public void disabledObservabilityLeavesBaseUntouched() {
        Assert.assertEquals(BASE, Observability.appendChains(BASE, false, false));
    }

    @Test
    public void tracingOnlyAppendsTracingChain() {
        Assert.assertEquals(
                BASE + ";" + Observability.TRACING_CHAIN_TOKEN,
                Observability.appendChains(BASE, true, false));
    }

    @Test
    public void metricsOnlyAppendsMetricsChain() {
        Assert.assertEquals(
                BASE + ";" + Observability.METRICS_CHAIN_TOKEN,
                Observability.appendChains(BASE, false, true));
    }

    @Test
    public void bothChainsKeepTracingBeforeMetrics() {
        Assert.assertEquals(
                BASE + ";" + Observability.TRACING_CHAIN_TOKEN + ";" + Observability.METRICS_CHAIN_TOKEN,
                Observability.appendChains(BASE, true, true));
    }

    @Test
    public void chainTokensFollowNameSlashVersionFormat() {
        Assert.assertEquals("ydb-sdk-tracing/0.1.0", Observability.TRACING_CHAIN_TOKEN);
        Assert.assertEquals("ydb-sdk-metrics/0.1.0", Observability.METRICS_CHAIN_TOKEN);
    }
}
