package tech.ydb.test.integration;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.test.integration.docker.ProxedDockerHelperFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ProxyDockerTest {
    private static final YdbHelperFactory factory = YdbHelperFactory.getInstance();

    @BeforeClass
    public static void initTest() {
        Assume.assumeTrue(factory instanceof ProxedDockerHelperFactory);
    }

    @Test
    public void testProxedDocker() {
        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertNotNull(helper);

            try (GrpcTransport transport = helper.createTransport()) {
//                transport.unaryCall(method, settings, helper)
            }
        }
    }

//            YdbHelperFactory factory = ;
//
//        Assert.assertNotNull("check disabled factory instance", factory);
//        Assert.assertFalse("check disabled factory instance", factory.isEnabled());
//        Assert.assertNull("empty helper for disabled factory instance", factory.createHelper());


}
