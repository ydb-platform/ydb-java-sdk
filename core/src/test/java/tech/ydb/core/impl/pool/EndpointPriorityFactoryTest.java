package tech.ydb.core.impl.pool;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;

import javax.net.ServerSocketFactory;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.timer.TestTicker;
import tech.ydb.proto.discovery.DiscoveryProtos;

/**
 * @author Kirill
 */
public class EndpointPriorityFactoryTest {

    @Test
    public void randomEvaluatorTest() {
        EndpointPriorityFactory priorityFactory = new EndpointPriorityFactory(
                BalancingSettings.defaultInstance(),
                list(endpoint("DC1"))
        );

        Assert.assertEquals(
                0,
                priorityFactory
                        .createEndpoint(endpoint("DC1"))
                        .getPriority()
        );
    }


    @Test
    public void localDCFixedTest() {
        EndpointPriorityFactory priorityFactory = new EndpointPriorityFactory(
                BalancingSettings.fromLocation("DC1"),
                list(
                        endpoint("DC1"),
                        endpoint("DC2")
                )
        );

        Assert.assertEquals(
                0,
                priorityFactory
                        .createEndpoint(endpoint("DC1"))
                        .getPriority()
        );

        Assert.assertEquals(
                1000,
                priorityFactory
                        .createEndpoint(endpoint("DC2"))
                        .getPriority()
        ); // shift
    }

    @Test
    public void detectLocalDCTest() {
        TestTicker testTicker = new TestTicker(
                9, 15,
                16, 50,
                51, 74,
                75, 77,
                78, 82,
                83, 125
        );

        try (ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(8080)) {
            Assert.assertFalse(serverSocket.isClosed());

            EndpointPriorityFactory priorityFactory = new EndpointPriorityFactory(
                    BalancingSettings.detectLocalDs(),
                    list(
                            endpoint("DC1"),
                            endpoint("DC1"),
                            endpoint("DC2"),
                            endpoint("DC2"),
                            endpoint("DC2"),
                            endpoint("DC3")
                    ),
                    testTicker
            );

            Assert.assertEquals(
                    0,
                    priorityFactory
                            .createEndpoint(endpoint("DC1"))
                            .getPriority()
            );

            Assert.assertEquals(
                    1000,
                    priorityFactory
                            .createEndpoint(endpoint("DC2"))
                            .getPriority()
            );

            Assert.assertEquals(
                    1000,
                    priorityFactory
                            .createEndpoint(endpoint("DC3"))
                            .getPriority()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static DiscoveryProtos.EndpointInfo endpoint(String location) {
        return DiscoveryProtos.EndpointInfo.newBuilder()
                .setAddress("localhost")
                .setPort(8080)
                .setNodeId(1)
                .setLocation(location)
                .build();
    }

    private static DiscoveryProtos.ListEndpointsResult list(DiscoveryProtos.EndpointInfo... endpoints) {
        return DiscoveryProtos.ListEndpointsResult.newBuilder()
                .setSelfLocation("DC1")
                .addAllEndpoints(Arrays.asList(endpoints))
                .build();
    }

}
