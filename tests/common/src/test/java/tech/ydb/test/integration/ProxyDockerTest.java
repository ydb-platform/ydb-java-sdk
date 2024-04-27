package tech.ydb.test.integration;


import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

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
        factory = YdbHelperFactory.getInstance();
        Assume.assumeTrue(factory instanceof ProxedDockerHelperFactory);
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
            }
        }
    }

}
