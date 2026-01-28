package tech.ydb.core.impl.pool;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import javax.net.SocketFactory;

import com.google.common.base.Ticker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.timer.TestTicker;

/**
 * @author Aleksandr Gorshenin
 * @author Kirill Kurdyukov
 */
public class EndpointPoolTest {
    private final static Set<String> EMPTY = Collections.emptySet();

    private AutoCloseable mocks;
    private final MockedStatic<ThreadLocalRandom> threadLocalStaticMock = Mockito.mockStatic(ThreadLocalRandom.class);
    private final MockedStatic<Ticker> tickerStaticMock = Mockito.mockStatic(Ticker.class);
    private final MockedStatic<SocketFactory> socketFactoryStaticMock = Mockito.mockStatic(SocketFactory.class);

    private final Socket socket = Mockito.mock(Socket.class);
    private final SocketFactory socketFactory = Mockito.mock(SocketFactory.class);
    private final ThreadLocalRandom random = Mockito.mock(ThreadLocalRandom.class);

    @Before
    public void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        threadLocalStaticMock.when(ThreadLocalRandom::current).thenReturn(random);
        socketFactoryStaticMock.when(SocketFactory::getDefault).thenReturn(socketFactory);
        Mockito.doNothing().when(socket).connect(Mockito.any(SocketAddress.class));
        Mockito.when(socketFactory.createSocket()).thenReturn(socket);
    }

    @After
    public void tearDown() throws Exception {
        socketFactoryStaticMock.close();
        tickerStaticMock.close();
        threadLocalStaticMock.close();
        mocks.close();
    }

    private GrpcRequestSettings empty() {
        return GrpcRequestSettings.newBuilder().build();
    }

    private GrpcRequestSettings nodeId(int nodeId) {
        return GrpcRequestSettings.newBuilder().withPreferredNodeID(nodeId).build();
    }

    private GrpcRequestSettings direct(int nodeId) {
        return GrpcRequestSettings.newBuilder().withPreferredNodeID(nodeId).withDirectMode(true).build();
    }

    private GrpcRequestSettings preferReady() {
        return GrpcRequestSettings.newBuilder().withPreferReadyChannel(true).build();
    }

    @Test
    public void directWithoutNodeIdTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());

        GrpcRequestSettings noNodeId = GrpcRequestSettings.newBuilder()
                .withDirectMode(true)
                .build();

        UnexpectedResultException ex = Assert.assertThrows(
                UnexpectedResultException.class, () -> pool.getEndpoint(EMPTY, noNodeId)
        );

        Assert.assertEquals("Cannot use direct mode without NodeId, code: CLIENT_INTERNAL_ERROR", ex.getMessage());
    }

    @Test
    public void uninitializedTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        check(pool.getEndpoint(EMPTY, empty())).isNull();
        check(pool.getEndpoint(EMPTY, nodeId(0))).isNull();
        check(pool.getEndpoint(EMPTY, nodeId(1))).isNull();

        pool.setNewState("DC1", list());

        check(pool.getEndpoint(EMPTY, empty())).isNull();
        check(pool.getEndpoint(EMPTY, nodeId(0))).isNull();
        check(pool.getEndpoint(EMPTY, nodeId(1))).isNull();
    }

    @Test
    public void useAllNodesTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        pool.setNewState("DC1", list(
                endpoint(1, "n1.ydb.tech", 12345, "DC1"),
                endpoint(2, "n2.ydb.tech", 12345, "DC2"),
                endpoint(3, "n3.ydb.tech", 12345, "DC3")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(3);

        Mockito.when(random.nextInt(3)).thenReturn(2, 0, 2, 1);

        check(pool.getEndpoint(EMPTY, empty())).hostname("n3.ydb.tech").nodeID(3).port(12345); // random choice
        check(pool.getEndpoint(EMPTY, nodeId(0))).hostname("n1.ydb.tech").nodeID(1).port(12345); // random choose
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12345);
        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("n2.ydb.tech").nodeID(2).port(12345);
        check(pool.getEndpoint(EMPTY, nodeId(3))).hostname("n3.ydb.tech").nodeID(3).port(12345);
        check(pool.getEndpoint(EMPTY, nodeId(4))).hostname("n3.ydb.tech").nodeID(3).port(12345); // random choose
        check(pool.getEndpoint(EMPTY, nodeId(5))).hostname("n2.ydb.tech").nodeID(2).port(12345); // random choose

        Mockito.verify(random, Mockito.times(4)).nextInt(3);
    }

    @Test
    public void prefferReadyTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        EndpointRecord e1 = endpoint(1, "n1.ydb.tech", 12345, "DC1");
        EndpointRecord e2 = endpoint(2, "n2.ydb.tech", 12345, "DC2");
        EndpointRecord e3 = endpoint(3, "n3.ydb.tech", 12345, "DC3");
        EndpointRecord e4 = endpoint(4, "n4.ydb.tech", 12345, "DC4");

        pool.setNewState("DC1", list(e1, e2, e3));

        Set<String> unknown = new TreeSet<>(Arrays.asList(e4.getHostAndPort()));
        Set<String> ready = new TreeSet<>(Arrays.asList(e3.getHostAndPort(), e1.getHostAndPort()));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(3);

        Mockito.when(random.nextInt(3)).thenReturn(2, 0, 1, 2);
        Mockito.when(random.nextInt(2)).thenReturn(1, 1, 0);
        Mockito.when(random.nextInt(1)).thenReturn(0, 0, 0, 0);

        check(pool.getEndpoint(EMPTY, preferReady())).hostname("n3.ydb.tech").nodeID(3).port(12345); // random choice
        check(pool.getEndpoint(unknown, preferReady())).hostname("n1.ydb.tech").nodeID(1).port(12345); // random choice
        Mockito.verify(random, Mockito.times(2)).nextInt(3);

        check(pool.getEndpoint(ready, preferReady())).hostname("n3.ydb.tech").nodeID(3).port(12345); // ready random
        check(pool.getEndpoint(ready, preferReady())).hostname("n3.ydb.tech").nodeID(3).port(12345); // ready random
        check(pool.getEndpoint(ready, preferReady())).hostname("n1.ydb.tech").nodeID(1).port(12345); // ready random
        Mockito.verify(random, Mockito.times(3)).nextInt(2);

        pool.pessimizeEndpoint(e3, "test");
        check(pool.getEndpoint(ready, preferReady())).hostname("n1.ydb.tech").nodeID(1).port(12345); // ready random
        check(pool.getEndpoint(ready, preferReady())).hostname("n1.ydb.tech").nodeID(1).port(12345); // ready random
        Mockito.verify(random, Mockito.times(2)).nextInt(1);

        pool.pessimizeEndpoint(e1, "test");
        check(pool.getEndpoint(ready, preferReady())).hostname("n2.ydb.tech").nodeID(2).port(12345);
        check(pool.getEndpoint(ready, preferReady())).hostname("n2.ydb.tech").nodeID(2).port(12345);
        Mockito.verify(random, Mockito.times(4)).nextInt(1);

        pool.pessimizeEndpoint(e2, "test");
        check(pool.getEndpoint(ready, preferReady())).hostname("n2.ydb.tech").nodeID(2).port(12345); // random choice
        check(pool.getEndpoint(ready, preferReady())).hostname("n3.ydb.tech").nodeID(3).port(12345); // random choice
        Mockito.verify(random, Mockito.times(4)).nextInt(3);
    }

    @Test
    public void localDcTest() {
        EndpointPool pool = new EndpointPool(preferredNode(null));
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        pool.setNewState("DC2", list(
                endpoint(1, "n1.ydb.tech", 12345, "DC1"),
                endpoint(2, "n2.ydb.tech", 12345, "DC2"),
                endpoint(3, "n3.ydb.tech", 12345, "DC3")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(1);

        Mockito.when(random.nextInt(1)).thenReturn(0, 0, 0);

        check(pool.getEndpoint(EMPTY, empty())).hostname("n2.ydb.tech").nodeID(2).port(12345); // random from local DC

        check(pool.getEndpoint(EMPTY, nodeId(0))).hostname("n2.ydb.tech").nodeID(2).port(12345); // random from local DC
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12345); // preferred
        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("n2.ydb.tech").nodeID(2).port(12345); // preferred
        check(pool.getEndpoint(EMPTY, nodeId(3))).hostname("n3.ydb.tech").nodeID(3).port(12345); // preferred
        check(pool.getEndpoint(EMPTY, nodeId(4))).hostname("n2.ydb.tech").nodeID(2).port(12345); // random from local DC

        check(pool.getEndpoint(EMPTY, direct(1))).hostname("n1.ydb.tech").nodeID(1).port(12345); // direct
        check(pool.getEndpoint(EMPTY, direct(2))).hostname("n2.ydb.tech").nodeID(2).port(12345); // direct
        check(pool.getEndpoint(EMPTY, direct(3))).hostname("n3.ydb.tech").nodeID(3).port(12345); // direct
        Assert.assertEquals("Node 4 not found, code: TRANSPORT_UNAVAILABLE",
                Assert.assertThrows(UnexpectedResultException.class,
                        () -> pool.getEndpoint(EMPTY, direct(4))).getMessage());

        Mockito.verify(random, Mockito.times(3)).nextInt(1);
    }

//    @Test
//    public void preferrReadyTest() {
//        EndpointPool pool = new EndpointPool(preferredNode("DC1"));
//        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);
//
//        pool.setNewState("DC3", list(
//                endpoint(1, "n1.ydb.tech", 12345, "DC1"),
//                endpoint(2, "n2.ydb.tech", 12345, "DC2"),
//                endpoint(3, "n3.ydb.tech", 12345, "DC3")
//        ));
//
//        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(1);
//
//        Mockito.when(random.nextInt(1)).thenReturn(0, 0, 0);
//
//        check(pool.getEndpoint(EMPTY, empty())).hostname("n1.ydb.tech").nodeID(1).port(12345); // random from DC1
//        check(pool.getEndpoint(EMPTY, nodeId(0))).hostname("n1.ydb.tech").nodeID(1).port(12345); // random from DC1
//        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12345); // preferred
//        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("n2.ydb.tech").nodeID(2).port(12345); // preferred
//        check(pool.getEndpoint(EMPTY, nodeId(3))).hostname("n3.ydb.tech").nodeID(3).port(12345); // preferred
//        check(pool.getEndpoint(EMPTY, nodeId(4))).hostname("n1.ydb.tech").nodeID(1).port(12345); // random from DC1
//
//        Mockito.verify(random, Mockito.times(3)).nextInt(1);
//    }

    @Test
    public void preferredDcTest() {
        EndpointPool pool = new EndpointPool(preferredNode("DC1"));
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        pool.setNewState("DC3", list(
                endpoint(1, "n1.ydb.tech", 12345, "DC1"),
                endpoint(2, "n2.ydb.tech", 12345, "DC2"),
                endpoint(3, "n3.ydb.tech", 12345, "DC3")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(1);

        Mockito.when(random.nextInt(1)).thenReturn(0, 0, 0);

        check(pool.getEndpoint(EMPTY, empty())).hostname("n1.ydb.tech").nodeID(1).port(12345); // random from DC1
        check(pool.getEndpoint(EMPTY, nodeId(0))).hostname("n1.ydb.tech").nodeID(1).port(12345); // random from DC1
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12345); // preferred
        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("n2.ydb.tech").nodeID(2).port(12345); // preferred
        check(pool.getEndpoint(EMPTY, nodeId(3))).hostname("n3.ydb.tech").nodeID(3).port(12345); // preferred
        check(pool.getEndpoint(EMPTY, nodeId(4))).hostname("n1.ydb.tech").nodeID(1).port(12345); // random from DC1

        Mockito.verify(random, Mockito.times(3)).nextInt(1);
    }

    @Test
    public void preferredEndpointsTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        pool.setNewState("DC3", list(
                endpoint(1, "n1.ydb.tech", 12341, "DC1"),
                endpoint(2, "n2.ydb.tech", 12342, "DC2"),
                endpoint(3, "n3.ydb.tech", 12343, "DC3")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(3);

        Mockito.when(random.nextInt(3)).thenReturn(2, 0, 2, 1);

        // If node is known
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12341);

        // If node is unknown - use default random choice
        check(pool.getEndpoint(EMPTY, nodeId(4))).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(EMPTY, nodeId(5))).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, nodeId(6))).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(EMPTY, nodeId(7))).hostname("n2.ydb.tech").nodeID(2).port(12342);

        Mockito.verify(random, Mockito.times(4)).nextInt(3);
    }

    @Test
    public void nodePessimizationTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        pool.setNewState("DC3", list(
                endpoint(1, "n1.ydb.tech", 12341, "DC1"),
                endpoint(2, "n2.ydb.tech", 12342, "DC2"),
                endpoint(3, "n3.ydb.tech", 12343, "DC3"),
                endpoint(4, "n4.ydb.tech", 12344, "DC4"),
                endpoint(5, "n5.ydb.tech", 12345, "DC5")
        ));

        check(pool).records(5).knownNodes(5).needToReDiscovery(false).bestEndpointsCount(5);

        Mockito.when(random.nextInt(5)).thenReturn(0, 1, 3, 2, 4);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n5.ydb.tech").nodeID(5).port(12345);
        Mockito.verify(random, Mockito.times(5)).nextInt(5);

        // Pessimize one node - four left in use
        pool.pessimizeEndpoint(pool.getEndpoint(EMPTY, nodeId(2)), "test");
        check(pool).records(5).knownNodes(5).needToReDiscovery(false).bestEndpointsCount(4);

        Mockito.when(random.nextInt(4)).thenReturn(0, 2, 1, 3);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n5.ydb.tech").nodeID(5).port(12345);
        Mockito.verify(random, Mockito.times(4)).nextInt(4);

        // but we can use pessimized node if specify it as preferred
        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("n2.ydb.tech").nodeID(2).port(12342);

        // Pessimize unknown nodes - nothing is changed
        pool.pessimizeEndpoint(new EndpointRecord("n2.ydb.tech", 12341, 2, null, null), "test 2");
        pool.pessimizeEndpoint(new EndpointRecord("n2.ydb.tech", 12342, 2, null, null), "test 3");
        pool.pessimizeEndpoint(null, "null");
        check(pool).records(5).knownNodes(5).needToReDiscovery(false).bestEndpointsCount(4);

        // Repeat node pessimization - nothing is change
        pool.pessimizeEndpoint(pool.getEndpoint(EMPTY, nodeId(2)), "");
        check(pool).records(5).knownNodes(5).needToReDiscovery(false).bestEndpointsCount(4);

        Mockito.when(random.nextInt(4)).thenReturn(3, 1, 2, 0);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n5.ydb.tech").nodeID(5).port(12345);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n1.ydb.tech").nodeID(1).port(12341);
        Mockito.verify(random, Mockito.times(8)).nextInt(4); // Mockito counts also previous 4

        // Pessimize two nodes - then we need to discovery
        pool.pessimizeEndpoint(pool.getEndpoint(EMPTY, nodeId(3)), "");
        check(pool).records(5).knownNodes(5).needToReDiscovery(false).bestEndpointsCount(3);
        pool.pessimizeEndpoint(pool.getEndpoint(EMPTY, nodeId(5)), "");
        check(pool).records(5).knownNodes(5).needToReDiscovery(true).bestEndpointsCount(2);

        Mockito.when(random.nextInt(2)).thenReturn(1, 1, 0, 0);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n1.ydb.tech").nodeID(1).port(12341);
        Mockito.verify(random, Mockito.times(4)).nextInt(2);
    }

    @Test
    public void nodePessimizationFallbackTest() {
        EndpointPool pool = new EndpointPool(preferredNode("DC1"));
        check(pool).records(0).knownNodes(0).needToReDiscovery(false);

        pool.setNewState("DC3", list(
                endpoint(1, "n1.ydb.tech", 12341, "DC1"),
                endpoint(2, "n2.ydb.tech", 12342, "DC1"),
                endpoint(3, "n3.ydb.tech", 12343, "DC2"),
                endpoint(4, "n4.ydb.tech", 12344, "DC2")
        ));

        check(pool).records(4).knownNodes(4).needToReDiscovery(false).bestEndpointsCount(2);

        // Only local nodes are used
        Mockito.when(random.nextInt(2)).thenReturn(0, 1);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n2.ydb.tech").nodeID(2).port(12342);
        Mockito.verify(random, Mockito.times(2)).nextInt(2);

        // Pessimize first local node - use second
        pool.pessimizeEndpoint(pool.getEndpoint(EMPTY, nodeId(1)), "");
        check(pool).records(4).knownNodes(4).needToReDiscovery(false).bestEndpointsCount(1);

        Mockito.when(random.nextInt(1)).thenReturn(0);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n2.ydb.tech").nodeID(2).port(12342);
        Mockito.verify(random, Mockito.times(1)).nextInt(1);

        // Pessimize second local node - use unlocal nodes
        pool.pessimizeEndpoint(pool.getEndpoint(EMPTY, nodeId(2)), "");
        check(pool).records(4).knownNodes(4).needToReDiscovery(false).bestEndpointsCount(2);

        Mockito.when(random.nextInt(2)).thenReturn(1, 0);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n3.ydb.tech").nodeID(3).port(12343);
        Mockito.verify(random, Mockito.times(4)).nextInt(2);

        // Pessimize all - fallback to use all nodes
        pool.pessimizeEndpoint(pool.getEndpoint(EMPTY, nodeId(3)), "");
        pool.pessimizeEndpoint(pool.getEndpoint(EMPTY, nodeId(4)), "");
        check(pool).records(4).knownNodes(4).needToReDiscovery(true).bestEndpointsCount(4);

        Mockito.when(random.nextInt(4)).thenReturn(3, 2, 1, 0);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool.getEndpoint(EMPTY, empty())).hostname("n1.ydb.tech").nodeID(1).port(12341);
        Mockito.verify(random, Mockito.times(4)).nextInt(4);

        // setNewState reset all
        pool.setNewState("DC3", list(
                endpoint(1, "n1.ydb.tech", 12341, "DC1"),
                endpoint(2, "n2.ydb.tech", 12342, "DC1"),
                endpoint(3, "n3.ydb.tech", 12343, "DC2"),
                endpoint(4, "n4.ydb.tech", 12344, "DC2")
        ));
        check(pool).records(4).knownNodes(4).needToReDiscovery(false).bestEndpointsCount(2);
    }

    @Test
    public void duplicateEndpointsTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false);

        pool.setNewState("DC", list(
                endpoint(1, "n1.ydb.tech", 12341, "DC"),
                endpoint(2, "n2.ydb.tech", 12342, "DC"),
                endpoint(3, "n3.ydb.tech", 12343, "DC"),
                endpoint(4, "n3.ydb.tech", 12343, "DC"), // duplicate
                endpoint(5, "n3.ydb.tech", 12343, "DC"), // duplicate
                endpoint(6, "n3.ydb.tech", 12344, "DC")  // not duplicate
        ));

        check(pool).records(4).knownNodes(4).needToReDiscovery(false).bestEndpointsCount(4);

        check(pool).record(0).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool).record(1).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool).record(2).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool).record(3).hostname("n3.ydb.tech").nodeID(6).port(12344);

        Mockito.when(random.nextInt(4)).thenReturn(2, 0, 3, 1);

        check(pool.getEndpoint(EMPTY, empty())).hostname("n3.ydb.tech").nodeID(3).port(12343); // random
        check(pool.getEndpoint(EMPTY, nodeId(0))).hostname("n1.ydb.tech").nodeID(1).port(12341); // random
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool.getEndpoint(EMPTY, nodeId(3))).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(EMPTY, nodeId(4))).hostname("n3.ydb.tech").nodeID(6).port(12344); // random
        check(pool.getEndpoint(EMPTY, nodeId(5))).hostname("n2.ydb.tech").nodeID(2).port(12342); // random
        check(pool.getEndpoint(EMPTY, nodeId(6))).hostname("n3.ydb.tech").nodeID(6).port(12344);

        Mockito.verify(random, Mockito.times(4)).nextInt(4);
    }

    @Test
    public void duplicateNodesTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false);

        pool.setNewState("DC", list(
                endpoint(1, "n1.ydb.tech", 12341, "DC"),
                endpoint(2, "n2.ydb.tech", 12342, "DC"),
                endpoint(2, "n3.ydb.tech", 12343, "DC")
        ));

        check(pool).records(3).knownNodes(2).needToReDiscovery(false).bestEndpointsCount(3);

        check(pool).record(0).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool).record(1).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool).record(2).hostname("n3.ydb.tech").nodeID(2).port(12343);

        Mockito.when(random.nextInt(3)).thenReturn(1, 0, 2);

        check(pool.getEndpoint(EMPTY, empty())).hostname("n2.ydb.tech").nodeID(2).port(12342); // random
        check(pool.getEndpoint(EMPTY, nodeId(0))).hostname("n1.ydb.tech").nodeID(1).port(12341); // random
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("n3.ydb.tech").nodeID(2).port(12343);
        check(pool.getEndpoint(EMPTY, nodeId(3))).hostname("n3.ydb.tech").nodeID(2).port(12343); // random

        Mockito.verify(random, Mockito.times(3)).nextInt(3);
    }

    @Test
    public void removeEndpointsTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false);

        pool.setNewState("DC", list(
                endpoint(1, "n1.ydb.tech", 12341, "DC"),
                endpoint(2, "n2.ydb.tech", 12342, "DC"),
                endpoint(3, "n3.ydb.tech", 12343, "DC")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(3);

        check(pool).record(0).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool).record(1).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool).record(2).hostname("n3.ydb.tech").nodeID(3).port(12343);

        Mockito.when(random.nextInt(3)).thenReturn(1, 0, 2);

        check(pool.getEndpoint(EMPTY, empty())).hostname("n2.ydb.tech").nodeID(2).port(12342); // random
        check(pool.getEndpoint(EMPTY, nodeId(0))).hostname("n1.ydb.tech").nodeID(1).port(12341); // random
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool.getEndpoint(EMPTY, nodeId(3))).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(EMPTY, nodeId(4))).hostname("n3.ydb.tech").nodeID(3).port(12343); // random

        Mockito.verify(random, Mockito.times(3)).nextInt(3);

        pool.setNewState("DC", list(
                endpoint(2, "n2.ydb.tech", 12342, "DC"),
                endpoint(4, "n4.ydb.tech", 12344, "DC"),
                endpoint(5, "n5.ydb.tech", 12345, "DC"),
                endpoint(6, "n6.ydb.tech", 12346, "DC")
        ));

        check(pool).records(4).knownNodes(4).needToReDiscovery(false).bestEndpointsCount(4);

        check(pool).record(0).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool).record(1).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool).record(2).hostname("n5.ydb.tech").nodeID(5).port(12345);
        check(pool).record(3).hostname("n6.ydb.tech").nodeID(6).port(12346);

        Mockito.when(random.nextInt(4)).thenReturn(3, 1, 2, 0);

        check(pool.getEndpoint(EMPTY, empty())).hostname("n6.ydb.tech").nodeID(6).port(12346); // random
        check(pool.getEndpoint(EMPTY, nodeId(0))).hostname("n4.ydb.tech").nodeID(4).port(12344); // random
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("n5.ydb.tech").nodeID(5).port(12345); // random
        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool.getEndpoint(EMPTY, nodeId(3))).hostname("n2.ydb.tech").nodeID(2).port(12342); // random
        check(pool.getEndpoint(EMPTY, nodeId(4))).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(EMPTY, nodeId(5))).hostname("n5.ydb.tech").nodeID(5).port(12345);
        check(pool.getEndpoint(EMPTY, nodeId(6))).hostname("n6.ydb.tech").nodeID(6).port(12346);

        Mockito.verify(random, Mockito.times(4)).nextInt(4);
    }


    @Test
    public void detectLocalDCTest() {
        final TestTicker testTicker = new TestTicker(
                1, 4,
                5, 26,
                83, 125
        );

        tickerStaticMock.when(Ticker::systemTicker).thenReturn(testTicker);

        EndpointPool pool = new EndpointPool(detectLocalDC());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false);

        int p1 = 1234;
        int p2 = 1235;
        int p3 = 1236;

        pool.setNewState("DC", list(
                endpoint(1, "127.0.0.1", p1, "DC1"),
                endpoint(2, "127.0.0.2", p2, "DC2"),
                endpoint(3, "127.0.0.3", p3, "DC3")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(1);

        check(pool.getEndpoint(EMPTY, empty())).hostname("127.0.0.2").nodeID(2).port(p2); // detect local dc
        check(pool.getEndpoint(EMPTY, nodeId(0))).hostname("127.0.0.2").nodeID(2).port(p2); // random from local dc
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("127.0.0.1").nodeID(1).port(p1);
        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("127.0.0.2").nodeID(2).port(p2); // local dc
        check(pool.getEndpoint(EMPTY, nodeId(3))).hostname("127.0.0.3").nodeID(3).port(p3);
        check(pool.getEndpoint(EMPTY, nodeId(4))).hostname("127.0.0.2").nodeID(2).port(p2); // random from local dc

        pool.pessimizeEndpoint(pool.getEndpoint(EMPTY, nodeId(2)), "");
        check(pool.getEndpoint(EMPTY, empty())).hostname("127.0.0.1").nodeID(1).port(p1); // new local dc
        check(pool.getEndpoint(EMPTY, nodeId(0))).hostname("127.0.0.1").nodeID(1).port(p1); // random from local dc
        check(pool.getEndpoint(EMPTY, nodeId(1))).hostname("127.0.0.1").nodeID(1).port(p1);
        check(pool.getEndpoint(EMPTY, nodeId(2))).hostname("127.0.0.2").nodeID(2).port(p2); // local dc
        check(pool.getEndpoint(EMPTY, nodeId(3))).hostname("127.0.0.3").nodeID(3).port(p3);
        check(pool.getEndpoint(EMPTY, nodeId(4))).hostname("127.0.0.1").nodeID(1).port(p1); // random from local dc
    }

    private static class PoolChecker {
        private final EndpointPool pool;

        public PoolChecker(EndpointPool pool) {
            this.pool = pool;
        }

        public PoolChecker records(int count) {
            Assert.assertEquals("Check pool records size", count, pool.getRecords().size());
            return this;
        }

        public EndpointRecordChecker record(int idx) {
            return new EndpointRecordChecker(pool.getRecords().get(idx).getEndpoint());
        }

        public PoolChecker knownNodes(int size) {
            Assert.assertEquals("Check known nodes keys size", size, pool.getEndpointsByNodeId().size());
            return this;
        }

        public PoolChecker needToReDiscovery(boolean value) {
            Assert.assertEquals("Check need to rediscovery", value, pool.needToRunDiscovery());
            return this;
        }

        public PoolChecker bestEndpointsCount(int value) {
            Assert.assertEquals("Check best endpoints count", value, pool.getBestEndpointCount());
            return this;
        }
    }

    private static class EndpointRecordChecker {
        private final EndpointRecord record;

        public EndpointRecordChecker(EndpointRecord record) {
            this.record = record;
        }

        public EndpointRecordChecker isNull() {
            Assert.assertNull("Check endpoint is null", record);
            return this;
        }

        public EndpointRecordChecker hostname(String hostname) {
            Assert.assertNotNull("Check endpoint is ot null", record);
            Assert.assertEquals("Check endpoint host", hostname, record.getHost());
            return this;
        }

        public EndpointRecordChecker port(int port) {
            Assert.assertNotNull("Check endpoint is ot null", record);
            Assert.assertEquals("Check endpoint port", port, record.getPort());
            return this;
        }

        public EndpointRecordChecker nodeID(int nodeID) {
            Assert.assertNotNull("Check endpoint is ot null", record);
            Assert.assertEquals("Check endpoint node id", nodeID, record.getNodeId());
            return this;
        }
    }

    private static PoolChecker check(EndpointPool pool) {
        return new PoolChecker(pool);
    }

    private static EndpointRecordChecker check(EndpointRecord record) {
        return new EndpointRecordChecker(record);
    }

    private static BalancingSettings useAllNodes() {
        return BalancingSettings.fromPolicy(BalancingSettings.Policy.USE_ALL_NODES);
    }

    private static BalancingSettings preferredNode(String selfLocation) {
        return BalancingSettings.fromLocation(selfLocation);
    }

    private static BalancingSettings detectLocalDC() {
        return BalancingSettings.detectLocalDs();
    }

    private static List<EndpointRecord> list(EndpointRecord... records) {
        return Arrays.asList(records);
    }

    private static EndpointRecord endpoint(int nodeID, String hostname, int port, String location) {
        return new EndpointRecord(hostname, port, nodeID, location, null);
    }
}
