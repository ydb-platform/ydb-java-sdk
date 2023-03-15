package tech.ydb.core.impl.priority;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import tech.ydb.core.timer.TestTicker;
import tech.ydb.discovery.DiscoveryProtos;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mockStatic;

/**
 * @author Kurdyukov Kirill
 */
public class PriorityEndpointEvaluatorTest {

    @Test
    public void randomEvaluatorTest() {
        RandomPriorityEndpointEvaluator evaluator = new RandomPriorityEndpointEvaluator();
        evaluator.prepareStatement(list());
        Assert.assertEquals(0, evaluator.evaluatePriority(endpoint("DC2")));
    }


    @Test
    public void localDCFixedTest() {
        LocalDCPriorityEndpointEvaluator evaluator = new LocalDCPriorityEndpointEvaluator("DC1");

        evaluator.prepareStatement(list(endpoint("DC1")));

        Assert.assertEquals(0, evaluator.evaluatePriority(endpoint("DC1")));
        Assert.assertEquals(1000, evaluator.evaluatePriority(endpoint("DC2"))); // shift
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

        DetectLocalDCPriorityEndpointEvaluator evaluator = new DetectLocalDCPriorityEndpointEvaluator(testTicker);

        try (ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(8080)) {
            evaluator.prepareStatement(
                    list(
                            endpoint("DC1"),
                            endpoint("DC1"),
                            endpoint("DC2"),
                            endpoint("DC2"),
                            endpoint("DC2"),
                            endpoint("DC3")
                    )
            );

            Assert.assertEquals(0, evaluator.evaluatePriority(
                    endpoint("DC1")
            ));

            Assert.assertEquals(18000, evaluator.evaluatePriority(
                    endpoint("DC2")
            ));

            Assert.assertEquals(39000, evaluator.evaluatePriority(
                    endpoint("DC3")
            ));

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
