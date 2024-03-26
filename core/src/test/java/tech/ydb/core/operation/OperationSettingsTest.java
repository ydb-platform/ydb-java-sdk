package tech.ydb.core.operation;


import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.settings.OperationSettings;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.common.CommonProtos;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OperationSettingsTest {
    private final static com.google.protobuf.Duration EMPTY = com.google.protobuf.Duration.newBuilder().build();

    private com.google.protobuf.Duration duration(int seconds) {
        return com.google.protobuf.Duration.newBuilder().setSeconds(seconds).build();
    }

    @Test
    public void defaultSettingsTest() {
        OperationSettings settings = new OperationSettings.OperationBuilder<>().build();
        OperationProtos.OperationParams proto = Operation.buildParams(settings);

        Assert.assertEquals(EMPTY, proto.getOperationTimeout());
        Assert.assertEquals(EMPTY, proto.getCancelAfter());
        Assert.assertEquals(CommonProtos.FeatureFlag.Status.STATUS_UNSPECIFIED, proto.getReportCostInfo());

        Assert.assertEquals(OperationProtos.OperationParams.OperationMode.SYNC, proto.getOperationMode());
    }

    @Test
    public void settingsTimeoutsTest() {
        OperationSettings settings = new OperationSettings.OperationBuilder<>()
                .withCancelTimeout(Duration.ofMinutes(1))
                .withOperationTimeout(10, TimeUnit.SECONDS)
                .build();
        OperationProtos.OperationParams proto = Operation.buildParams(settings);

        Assert.assertEquals(duration(10), proto.getOperationTimeout());
        Assert.assertEquals(duration(60), proto.getCancelAfter());
        Assert.assertEquals(CommonProtos.FeatureFlag.Status.STATUS_UNSPECIFIED, proto.getReportCostInfo());

        Assert.assertEquals(OperationProtos.OperationParams.OperationMode.SYNC, proto.getOperationMode());
    }

    @Test
    public void costEnabledTest() {
        OperationSettings settings = new OperationSettings.OperationBuilder<>()
                .withAsyncMode(false)
                .withReportCostInfo(Boolean.TRUE)
                .build();
        OperationProtos.OperationParams proto = Operation.buildParams(settings);

        Assert.assertEquals(EMPTY, proto.getOperationTimeout());
        Assert.assertEquals(EMPTY, proto.getCancelAfter());
        Assert.assertEquals(CommonProtos.FeatureFlag.Status.ENABLED, proto.getReportCostInfo());

        Assert.assertEquals(OperationProtos.OperationParams.OperationMode.SYNC, proto.getOperationMode());
    }

    @Test
    public void allSettingsTest() {
        OperationSettings settings = new OperationSettings.OperationBuilder<>()
                .withAsyncMode(true)
                .withCancelTimeout(3, TimeUnit.MINUTES)
                .withOperationTimeout(Duration.ofSeconds(23))
                .withReportCostInfo(Boolean.FALSE)
                .build();
        OperationProtos.OperationParams proto = Operation.buildParams(settings);

        Assert.assertEquals(duration(23), proto.getOperationTimeout());
        Assert.assertEquals(duration(180), proto.getCancelAfter());
        Assert.assertEquals(CommonProtos.FeatureFlag.Status.DISABLED, proto.getReportCostInfo());

        Assert.assertEquals(OperationProtos.OperationParams.OperationMode.ASYNC, proto.getOperationMode());
    }
}
