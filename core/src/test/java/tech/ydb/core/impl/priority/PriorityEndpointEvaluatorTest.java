package tech.ydb.core.impl.priority;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import tech.ydb.core.utils.Timer;
import tech.ydb.discovery.DiscoveryProtos;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;

import static org.mockito.Mockito.mockStatic;

public class PriorityEndpointEvaluatorTest {

    @Test
    public void randomEvaluatorTest() {
        RandomPriorityEndpointEvaluator evaluator = new RandomPriorityEndpointEvaluator();

        Assert.assertEquals(0, evaluator.evaluatePriority(
                "DC1",
                endpoint("DC2")
        ));
    }


    @Test
    public void localDCFixedTest() {
        LocalDCPriorityEndpointEvaluator evaluator = new LocalDCPriorityEndpointEvaluator("DC1");

        Assert.assertEquals(0, evaluator.evaluatePriority(
                "DC1",
                endpoint("DC1")
        ));
        Assert.assertEquals(1000, evaluator.evaluatePriority(
                "DC1",
                endpoint("DC2")
        )); // shift
    }

    @Test
    public void detectLocalDCTest() {
        DetectLocalDCPriorityEndpointEvaluator evaluator = new DetectLocalDCPriorityEndpointEvaluator();

        MockedStatic<Timer> systemMocked = mockStatic(Timer.class);

        systemMocked.when(Timer::nanoTime).thenReturn(1L, 5L);

        try(ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(8080)) {
            Assert.assertEquals(4, evaluator.evaluatePriority(
                    "DC1",
                    endpoint("DC2")
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
}
