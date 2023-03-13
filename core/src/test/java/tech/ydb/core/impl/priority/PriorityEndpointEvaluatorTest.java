package tech.ydb.core.impl.priority;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import tech.ydb.core.utils.Timer;
import tech.ydb.discovery.DiscoveryProtos;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;

import static org.mockito.Mockito.mockStatic;

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
        DetectLocalDCPriorityEndpointEvaluator evaluator = new DetectLocalDCPriorityEndpointEvaluator();

        MockedStatic<Timer> systemMocked = mockStatic(Timer.class);

        long delta = 10_000_000;

        systemMocked.when(Timer::nanoTime).thenReturn(delta, 2 * delta,
                5 * delta, 10 * delta, 10 * delta, 20 * delta);

        try (ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(8080)) {
            evaluator.prepareStatement(
                    list(
                            endpoint("DC1"),
                            endpoint("DC2"),
                            endpoint("DC3")
                    )
            );

            Assert.assertEquals(4000, evaluator.evaluatePriority(
                    endpoint("DC1")
            ));

            Assert.assertEquals(0, evaluator.evaluatePriority(
                    endpoint("DC2")
            ));

            Assert.assertEquals(9000, evaluator.evaluatePriority(
                    endpoint("DC3")
            ));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        systemMocked.close();
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
