package tech.ydb.test.integration;


import java.lang.reflect.Field;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.constraint.WaitConstraint;

import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.scheme.SchemeOperationProtos;
import tech.ydb.proto.scheme.v1.SchemeServiceGrpc;
import tech.ydb.test.integration.docker.ProxedDockerHelperFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ProxyDockerTest {
    private static YdbHelperFactory factory;

    @BeforeClass
    public static void initTest() {
        try {
            Field f = Awaitility.class.getDeclaredField("defaultWaitConstraint");
            f.setAccessible(true);
            WaitConstraint constraint = (WaitConstraint) f.get(null);
            Assume.assumeFalse(constraint.getMaxWaitTime().isZero());
            System.out.println("BEFORE TIMEOUT = " + constraint.getMaxWaitTime());
        } catch (Exception ex) {
            // ignore
        }

        factory = YdbHelperFactory.getInstance();
        Assume.assumeTrue(factory instanceof ProxedDockerHelperFactory);
    }

    @AfterClass
    public static void closeTest() {
        try {
            Field f = Awaitility.class.getDeclaredField("defaultWaitConstraint");
            f.setAccessible(true);
            WaitConstraint constraint = (WaitConstraint) f.get(null);
            Assume.assumeFalse(constraint.getMaxWaitTime().isZero());
            System.out.println("AFTER TIMEOUT = " + constraint.getMaxWaitTime());
        } catch (Exception ex) {
            // ignore
        }
    }

    @Test
    public void testProxedDocker() throws InvalidProtocolBufferException {
        try (YdbHelper helper = factory.createHelper()) {
            Assert.assertNotNull(helper);

            try (GrpcTransport transport = helper.createTransport()) {
                GrpcRequestSettings settings = GrpcRequestSettings.newBuilder().build();
                SchemeOperationProtos.DescribePathRequest request = SchemeOperationProtos.DescribePathRequest
                        .newBuilder()
                        .setPath(helper.database())
                        .build();

                SchemeOperationProtos.DescribePathResponse response = transport.unaryCall(
                        SchemeServiceGrpc.getDescribePathMethod(), settings, request
                ).join().getValue();

                Assert.assertTrue(response.getOperation().getReady());

                SchemeOperationProtos.DescribePathResult result = response.getOperation().getResult()
                        .unpack(SchemeOperationProtos.DescribePathResult.class);

                Assert.assertEquals(helper.database(), "/" + result.getSelf().getName());
                Assert.assertNull(helper.authToken());
                Assert.assertNull(helper.pemCert());
                Assert.assertFalse(helper.useTls());
            }
        }
    }
}
