package tech.ydb.core.impl.pool;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ServerSocketFactory;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.timer.TestTicker;

/**
 * @author Kirill
 */
public class PriorityPickerTest {

    @Test
    public void randomEvaluatorTest() {
        PriorityPicker picker = PriorityPicker.from(BalancingSettings.defaultInstance(), null, null);
        Assert.assertEquals(0, picker.getEndpointPriority("DC1"));
        Assert.assertEquals(0, picker.getEndpointPriority("DC2"));
        Assert.assertEquals(0, picker.getEndpointPriority("DC3"));
    }


    @Test
    public void fixedLocalDcTest() {
        PriorityPicker ignoreSelftLocation = PriorityPicker.from(BalancingSettings.defaultInstance(), "DC1", null);
        Assert.assertEquals(0, ignoreSelftLocation.getEndpointPriority("dC1"));
        Assert.assertEquals(0, ignoreSelftLocation.getEndpointPriority("Dc2"));
        Assert.assertEquals(0, ignoreSelftLocation.getEndpointPriority("Dc3"));

        PriorityPicker useSelfLocation = PriorityPicker.from(BalancingSettings.fromLocation(""), "DC1", null);
        Assert.assertEquals(0, useSelfLocation.getEndpointPriority("dC1"));
        Assert.assertEquals(1000, useSelfLocation.getEndpointPriority("Dc2"));
        Assert.assertEquals(1000, useSelfLocation.getEndpointPriority("Dc3"));

        PriorityPicker useLocalDC = PriorityPicker.from(BalancingSettings.fromLocation("DC2"), "DC1", null);
        Assert.assertEquals(1000, useLocalDC.getEndpointPriority("dC1"));
        Assert.assertEquals(0, useLocalDC.getEndpointPriority("Dc2"));
        Assert.assertEquals(1000, useLocalDC.getEndpointPriority("Dc3"));
    }

    @Test
    public void detectLocalDCfallbackTest() {
        List<EndpointRecord> single = Collections.singletonList(new EndpointRecord("localhost", 8080, 0, "DC1", null));
        PriorityPicker ignoreSelftLocation = PriorityPicker.from(BalancingSettings.detectLocalDs(), "DC1", single);

        Assert.assertEquals(0, ignoreSelftLocation.getEndpointPriority("DC1"));
        Assert.assertEquals(0, ignoreSelftLocation.getEndpointPriority("DC2"));
        Assert.assertEquals(0, ignoreSelftLocation.getEndpointPriority("DC3"));
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

        try (ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0)) {
            Assert.assertFalse(serverSocket.isClosed());
            final int port = serverSocket.getLocalPort();

            List<EndpointRecord> records = Arrays.asList("DC1", "DC1", "DC2", "DC2", "DC2", "DC3")
                    .stream().map(location -> new EndpointRecord("localhost", port, 1, location, null))
                    .collect(Collectors.toList());

            String localDC = PriorityPicker.detectLocalDC(records, testTicker);
            Assert.assertEquals("DC1", localDC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
