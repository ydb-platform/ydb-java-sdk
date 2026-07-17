package tech.ydb.table.impl;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import tech.ydb.core.metrics.Meter;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;

public class PooledTableClientObservabilityTest {

    @Test
    public void noopMeterDoesNotEnableMetricsBuildInfoChain() {
        TableRpc rpc = Mockito.mock(TableRpc.class);

        try (MockedStatic<GrpcTableRpc> helper = Mockito.mockStatic(GrpcTableRpc.class)) {
            PooledTableClient.newClient(rpc).withMeter(Meter.NOOP, "default");

            helper.verifyNoInteractions();
        }
    }

    @Test
    public void realMeterEnablesMetricsBuildInfoChain() {
        TableRpc rpc = Mockito.mock(TableRpc.class);
        Meter meter = Mockito.mock(Meter.class);

        try (MockedStatic<GrpcTableRpc> helper = Mockito.mockStatic(GrpcTableRpc.class)) {
            PooledTableClient.newClient(rpc).withMeter(meter, "default");

            helper.verify(() -> GrpcTableRpc.enableMetricsChain(rpc), Mockito.times(1));
        }
    }
}
