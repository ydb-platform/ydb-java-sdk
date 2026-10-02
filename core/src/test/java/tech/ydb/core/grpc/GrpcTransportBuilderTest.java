package tech.ydb.core.grpc;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcTransportBuilderTest {
    private final static String YDB = "grpcs://some-host:3456/database";

    @Test
    public void defaultValuesTest() {
        GrpcTransportBuilder builder = GrpcTransport.forConnectionString(YDB);

        Assert.assertEquals("/database", builder.getDatabase());
        Assert.assertEquals("some-host:3456", builder.getEndpoint());
        Assert.assertNull(builder.getCert());
        Assert.assertEquals(GrpcTransportBuilder.InitMode.SYNC, builder.getInitMode());
        Assert.assertNotNull(builder.getManagedChannelFactory());
    }

    @Test
    public void balancingSettingsTest() {
        BalancingSettings def = GrpcTransport.forConnectionString(YDB).getBalancingSettings();
        Assert.assertNotNull(def);
        Assert.assertEquals(BalancingSettings.Policy.USE_ALL_NODES, def.getPolicy());
        Assert.assertNull(def.getPreferableLocation());

        @SuppressWarnings("deprecation")
        BalancingSettings localDc = GrpcTransport.forConnectionString(YDB)
                .withLocalDataCenter("ca").getBalancingSettings();
        Assert.assertNotNull(localDc);
        Assert.assertEquals(BalancingSettings.Policy.USE_PREFERABLE_LOCATION, localDc.getPolicy());
        Assert.assertEquals("ca", localDc.getPreferableLocation());

        BalancingSettings detect = GrpcTransport.forConnectionString(YDB)
                .withBalancingSettings(BalancingSettings.detectLocalDs()).getBalancingSettings();
        Assert.assertNotNull(detect);
        Assert.assertEquals(BalancingSettings.Policy.USE_DETECT_LOCAL_DC, detect.getPolicy());
        Assert.assertNull(detect.getPreferableLocation());

        @SuppressWarnings("deprecation")
        BalancingSettings mixed = GrpcTransport.forConnectionString(YDB)
                .withBalancingSettings(BalancingSettings.detectLocalDs())
                .withLocalDataCenter("ca")
                .getBalancingSettings();

        Assert.assertNotNull(mixed);
        Assert.assertEquals(BalancingSettings.Policy.USE_DETECT_LOCAL_DC, mixed.getPolicy());
        Assert.assertNull(mixed.getPreferableLocation());
    }
}
