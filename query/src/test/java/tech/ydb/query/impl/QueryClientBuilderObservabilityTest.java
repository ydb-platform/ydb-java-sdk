package tech.ydb.query.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.BuildInfoChainSupport;
import tech.ydb.core.metrics.Meter;

public class QueryClientBuilderObservabilityTest {

    @Test
    public void noopMeterDoesNotEnableMetricsBuildInfoChain() {
        GrpcTransport transport = Mockito.mock(GrpcTransport.class);
        ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
        Mockito.when(transport.getScheduler()).thenReturn(scheduler);
        Mockito.when(transport.getDatabase()).thenReturn("/local");

        try (MockedStatic<BuildInfoChainSupport> helper = Mockito.mockStatic(BuildInfoChainSupport.class)) {
            QueryClientImpl.Builder builder = new QueryClientImpl.Builder(transport);
            builder.withMeter(Meter.NOOP, "default");

            helper.verifyNoInteractions();
        }
    }

    @Test
    public void openTelemetryMeterEnablesMetricsBuildInfoChain() {
        GrpcTransport transport = Mockito.mock(GrpcTransport.class);
        ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
        Mockito.when(transport.getScheduler()).thenReturn(scheduler);
        Mockito.when(transport.getDatabase()).thenReturn("/local");

        try (MockedStatic<BuildInfoChainSupport> helper = Mockito.mockStatic(BuildInfoChainSupport.class)) {
            QueryClientImpl.Builder builder = new QueryClientImpl.Builder(transport);
            builder.withMeter(Mockito.mock(Meter.class), "default");

            helper.verify(() -> BuildInfoChainSupport.enableMetricsChain(transport), Mockito.times(1));
        }
    }
}
