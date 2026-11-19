package tech.ydb.core.impl.pool;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ServerSocketFactory;

import com.google.common.base.Ticker;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.proto.bridge.YdbBridgeCommon;
import tech.ydb.proto.bridge.YdbBridgeCommon.PileState.State;

/**
 * @author Kirill
 */
public class PriorityPickerTest {
    private final static Piles TEST = new Piles(Arrays.asList(
            YdbBridgeCommon.PileState.newBuilder().setPileName("Pile1").setState(State.PRIMARY).build(),
            YdbBridgeCommon.PileState.newBuilder().setPileName("Pile2").setState(State.SYNCHRONIZED).build(),
            YdbBridgeCommon.PileState.newBuilder().setPileName("Pile3").setState(State.SUSPENDED).build(),
            YdbBridgeCommon.PileState.newBuilder().setPileName("Pile4").setState(State.PROMOTED).build()
    ));

    @Test
    public void primaryPilePrioritiesTest() {
        PriorityPicker picker = PriorityPicker.from(BalancingSettings.preferPrimaryPile(), "DC1", TEST, null);

        Assert.assertEquals(0, picker.getPriority(new EndpointRecord("ordinary", 2135)));

        Assert.assertEquals(0, picker.getPriority(new EndpointRecord("pil1", 2135, 1, "DC1", "Pile1", null)));
        Assert.assertEquals(1000, picker.getPriority(new EndpointRecord("pil2", 2135, 1, "DC1", "Pile2", null)));
        Assert.assertEquals(2000, picker.getPriority(new EndpointRecord("pil3", 2135, 1, "DC1", "Pile3", null)));
        Assert.assertEquals(0, picker.getPriority(new EndpointRecord("pil4", 2135, 1, "DC1", "Pile4", null)));
        Assert.assertEquals(2000, picker.getPriority(new EndpointRecord("pil5", 2135, 1, "DC1", "Pile5", null)));
    }

    @Test
    public void randomEvaluatorTest() {
        PriorityPicker picker = PriorityPicker.from(BalancingSettings.defaultInstance(), null, TEST, null);
        Assert.assertEquals(0, picker.getPriority(new EndpointRecord("localhost", 8080, 0, "DC1")));
        Assert.assertEquals(0, picker.getPriority(new EndpointRecord("localhost", 8080, 0, "DC2")));
        Assert.assertEquals(0, picker.getPriority(new EndpointRecord("localhost", 8080, 0, "DC3")));
    }

    @Test
    public void fixedLocalDcTest() {
        EndpointRecord dc1 = new EndpointRecord("localhost", 8080, 0, "DC1");
        EndpointRecord dc2 = new EndpointRecord("localhost", 8080, 0, "Dc2");
        EndpointRecord dc3 = new EndpointRecord("localhost", 8080, 0, "dc3");

        PriorityPicker def = PriorityPicker.from(BalancingSettings.defaultInstance(), "DC1", TEST, null);
        Assert.assertEquals(0, def.getPriority(dc1));
        Assert.assertEquals(0, def.getPriority(dc2));
        Assert.assertEquals(0, def.getPriority(dc3));

        PriorityPicker selfLocation = PriorityPicker.from(BalancingSettings.fromLocation(""), "DC1", TEST, null);
        Assert.assertEquals(0, selfLocation.getPriority(dc1));
        Assert.assertEquals(1000, selfLocation.getPriority(dc2));
        Assert.assertEquals(1000, selfLocation.getPriority(dc3));

        PriorityPicker localDc = PriorityPicker.from(BalancingSettings.fromLocation("DC2"), "DC1", TEST, null);
        Assert.assertEquals(1000, localDc.getPriority(dc1));
        Assert.assertEquals(0, localDc.getPriority(dc2));
        Assert.assertEquals(1000, localDc.getPriority(dc3));
    }

    @Test
    public void detectLocalDCFallbackTest() {
        List<EndpointRecord> single = Arrays.asList(new EndpointRecord("localhost", 8080, 0, "DC1"));
        PriorityPicker detect = PriorityPicker.from(BalancingSettings.detectLocalDs(), "DC1", TEST, single);

        Assert.assertEquals(0, detect.getPriority(new EndpointRecord("localhost", 8080, 0, "DC1")));
        Assert.assertEquals(0, detect.getPriority(new EndpointRecord("localhost", 8080, 0, "DC2")));
        Assert.assertEquals(0, detect.getPriority(new EndpointRecord("localhost", 8080, 0, "DC3")));
    }

    @Test
    public void detectLocalDCWithEmptyLocationTest() {
        List<EndpointRecord> bootstrap = Arrays.asList(new EndpointRecord("localhost", 2136));
        PriorityPicker picker = PriorityPicker.from(BalancingSettings.detectLocalDs(), null, Piles.EMPTY, bootstrap);

        Assert.assertEquals(0, picker.getPriority(new EndpointRecord("localhost", 2136, 0, "DC1")));
        Assert.assertEquals(0, picker.getPriority(new EndpointRecord("localhost", 2136, 0)));

        Assert.assertNull(LocationPicker.detectLocalDC(bootstrap, Ticker.systemTicker()));
        Assert.assertNull(LocationPicker.detectLocalDC(Collections.emptyList(), Ticker.systemTicker()));
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

            List<EndpointRecord> records = Stream.of("DC1", "DC1", "DC2", "DC2", "DC2", "DC3")
                    .map(location -> new EndpointRecord("localhost", port, 1, location))
                    .collect(Collectors.toList());

            String localDC = LocationPicker.detectLocalDC(records, testTicker);
            Assert.assertEquals("DC1", localDC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void detectLocalDCWithUnreachableNodesTest() throws IOException {
        int closedPort;

        try (ServerSocket socket = ServerSocketFactory.getDefault().createServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        // every successful probe takes 10 nanos
        Ticker ticker = new Ticker() {
            private long now = 0;

            @Override
            public long read() {
                now += 10;
                return now;
            }
        };

        try (ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0)) {
            int openPort = serverSocket.getLocalPort();

            List<EndpointRecord> records = Arrays.asList(
                    new EndpointRecord("localhost", closedPort, 1, "DOWN"),
                    new EndpointRecord("localhost", closedPort, 2, "DOWN"),
                    new EndpointRecord("localhost", openPort, 3, "UP")
            );

            // failed probes of DOWN must not overflow into a negative, i.e. the best, ping
            Assert.assertEquals("UP", LocationPicker.detectLocalDC(records, ticker));
        }
    }
}
