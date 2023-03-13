package tech.ydb.core.impl.pool;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.utils.Timer;
import tech.ydb.discovery.DiscoveryProtos;

import javax.net.ServerSocketFactory;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksandr Gorshenin
 * @author Kirill Kurdyukov
 */
public class EndpointPoolTest {
    private final AutoCloseable mocks = MockitoAnnotations.openMocks(this);
    private final MockedStatic<ThreadLocalRandom> threadLocalStaticMock = mockStatic(ThreadLocalRandom.class);
    private final ThreadLocalRandom random = Mockito.mock(ThreadLocalRandom.class);

    @Before
    public void setUp() {
        threadLocalStaticMock.when(ThreadLocalRandom::current).thenReturn(random);
    }

    @After
    public void tearDown() throws Exception {
        threadLocalStaticMock.close();
        mocks.close();
    }

    @Test
    public void uninitializedTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        check(pool.getEndpoint(null)).isNull();
        check(pool.getEndpoint(0)).isNull();
        check(pool.getEndpoint(1)).isNull();

        pool.setNewState(list("DC1"));

        check(pool.getEndpoint(null)).isNull();
        check(pool.getEndpoint(0)).isNull();
        check(pool.getEndpoint(1)).isNull();
    }

    @Test
    public void useAllNodesTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        pool.setNewState(list("DC1",
                endpoint(1, "n1.ydb.tech", 12345, "DC1"),
                endpoint(2, "n2.ydb.tech", 12345, "DC2"),
                endpoint(3, "n3.ydb.tech", 12345, "DC3")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(3);

        when(random.nextInt(3)).thenReturn(2, 0, 2, 1);

        check(pool.getEndpoint(null)).hostname("n3.ydb.tech").nodeID(3).port(12345); // random choice
        check(pool.getEndpoint(0)).hostname("n1.ydb.tech").nodeID(1).port(12345); // random choose
        check(pool.getEndpoint(1)).hostname("n1.ydb.tech").nodeID(1).port(12345);
        check(pool.getEndpoint(2)).hostname("n2.ydb.tech").nodeID(2).port(12345);
        check(pool.getEndpoint(3)).hostname("n3.ydb.tech").nodeID(3).port(12345);
        check(pool.getEndpoint(4)).hostname("n3.ydb.tech").nodeID(3).port(12345); // random choose
        check(pool.getEndpoint(5)).hostname("n2.ydb.tech").nodeID(2).port(12345); // random choose

        verify(random, times(4)).nextInt(3);
    }

    @Test
    public void localDcTest() {
        EndpointPool pool = new EndpointPool(preferredNode(null));
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        pool.setNewState(list("DC2",
                endpoint(1, "n1.ydb.tech", 12345, "DC1"),
                endpoint(2, "n2.ydb.tech", 12345, "DC2"),
                endpoint(3, "n3.ydb.tech", 12345, "DC3")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(1);

        when(random.nextInt(1)).thenReturn(0, 0, 0);

        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12345); // random from local DC
        check(pool.getEndpoint(0)).hostname("n2.ydb.tech").nodeID(2).port(12345); // random from local DC
        check(pool.getEndpoint(1)).hostname("n1.ydb.tech").nodeID(1).port(12345); // preffered
        check(pool.getEndpoint(2)).hostname("n2.ydb.tech").nodeID(2).port(12345); // preffered
        check(pool.getEndpoint(3)).hostname("n3.ydb.tech").nodeID(3).port(12345); // preffered
        check(pool.getEndpoint(4)).hostname("n2.ydb.tech").nodeID(2).port(12345); // random from local DC

        verify(random, times(3)).nextInt(1);
    }

    @Test
    public void preferredDcTest() {
        EndpointPool pool = new EndpointPool(preferredNode("DC1"));
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        pool.setNewState(list("DC3",
                endpoint(1, "n1.ydb.tech", 12345, "DC1"),
                endpoint(2, "n2.ydb.tech", 12345, "DC2"),
                endpoint(3, "n3.ydb.tech", 12345, "DC3")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(1);

        when(random.nextInt(1)).thenReturn(0, 0, 0);

        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12345); // random from DC1
        check(pool.getEndpoint(0)).hostname("n1.ydb.tech").nodeID(1).port(12345); // random from DC1
        check(pool.getEndpoint(1)).hostname("n1.ydb.tech").nodeID(1).port(12345); // preferred
        check(pool.getEndpoint(2)).hostname("n2.ydb.tech").nodeID(2).port(12345); // preferred
        check(pool.getEndpoint(3)).hostname("n3.ydb.tech").nodeID(3).port(12345); // preferred
        check(pool.getEndpoint(4)).hostname("n1.ydb.tech").nodeID(1).port(12345); // random from DC1

        verify(random, times(3)).nextInt(1);
    }

    @Test
    public void preferredEndpointsTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        pool.setNewState(list("DC3",
                endpoint(1, "n1.ydb.tech", 12341, "DC1"),
                endpoint(2, "n2.ydb.tech", 12342, "DC2"),
                endpoint(3, "n3.ydb.tech", 12343, "DC3")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(3);

        when(random.nextInt(3)).thenReturn(2, 0, 2, 1);

        // If node is known
        check(pool.getEndpoint(1)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(1)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(1)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(1)).hostname("n1.ydb.tech").nodeID(1).port(12341);

        // If node is unknown - use default random choice
        check(pool.getEndpoint(4)).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(5)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(6)).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(7)).hostname("n2.ydb.tech").nodeID(2).port(12342);

        verify(random, times(4)).nextInt(3);
    }

    @Test
    public void nodePessimizationTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false).bestEndpointsCount(-1);

        pool.setNewState(list("DC3",
                endpoint(1, "n1.ydb.tech", 12341, "DC1"),
                endpoint(2, "n2.ydb.tech", 12342, "DC2"),
                endpoint(3, "n3.ydb.tech", 12343, "DC3"),
                endpoint(4, "n4.ydb.tech", 12344, "DC4"),
                endpoint(5, "n5.ydb.tech", 12345, "DC5")
        ));

        check(pool).records(5).knownNodes(5).needToReDiscovery(false).bestEndpointsCount(5);

        when(random.nextInt(5)).thenReturn(0, 1, 3, 2, 4);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool.getEndpoint(null)).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(null)).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(null)).hostname("n5.ydb.tech").nodeID(5).port(12345);
        verify(random, times(5)).nextInt(5);

        // Pessimize one node - four left in use
        pool.pessimizeEndpoint(pool.getEndpoint(2));
        check(pool).records(5).knownNodes(5).needToReDiscovery(false).bestEndpointsCount(4);

        when(random.nextInt(4)).thenReturn(0, 2, 1, 3);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(null)).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(null)).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(null)).hostname("n5.ydb.tech").nodeID(5).port(12345);
        verify(random, times(4)).nextInt(4);

        // but we can use pessimized node if specify it as preffered
        check(pool.getEndpoint(2)).hostname("n2.ydb.tech").nodeID(2).port(12342);

        // Pessimize unknown nodes - nothing is changed
        pool.pessimizeEndpoint(new EndpointRecord("n2.ydb.tech", 12341, 2));
        pool.pessimizeEndpoint(new EndpointRecord("n2.ydb.tech", 12342, 2));
        pool.pessimizeEndpoint(null);
        check(pool).records(5).knownNodes(5).needToReDiscovery(false).bestEndpointsCount(4);

        // Repeat node pessimization - nothing is change
        pool.pessimizeEndpoint(pool.getEndpoint(2));
        check(pool).records(5).knownNodes(5).needToReDiscovery(false).bestEndpointsCount(4);

        when(random.nextInt(4)).thenReturn(3, 1, 2, 0);
        check(pool.getEndpoint(null)).hostname("n5.ydb.tech").nodeID(5).port(12345);
        check(pool.getEndpoint(null)).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(null)).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        verify(random, times(8)).nextInt(4); // Mockito counts also previous 4

        // Pessimize two nodes - then we need to discovery
        pool.pessimizeEndpoint(pool.getEndpoint(3));
        check(pool).records(5).knownNodes(5).needToReDiscovery(false).bestEndpointsCount(3);
        pool.pessimizeEndpoint(pool.getEndpoint(5));
        check(pool).records(5).knownNodes(5).needToReDiscovery(true).bestEndpointsCount(2);

        when(random.nextInt(2)).thenReturn(1, 1, 0, 0);
        check(pool.getEndpoint(null)).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(null)).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        verify(random, times(4)).nextInt(2);
    }

    @Test
    public void nodePessimizationFallbackTest() {
        EndpointPool pool = new EndpointPool(preferredNode("DC1"));
        check(pool).records(0).knownNodes(0).needToReDiscovery(false);

        pool.setNewState(list("DC3",
                endpoint(1, "n1.ydb.tech", 12341, "DC1"),
                endpoint(2, "n2.ydb.tech", 12342, "DC1"),
                endpoint(3, "n3.ydb.tech", 12343, "DC2"),
                endpoint(4, "n4.ydb.tech", 12344, "DC2")
        ));

        check(pool).records(4).knownNodes(4).needToReDiscovery(false).bestEndpointsCount(2);

        // Only local nodes are used
        when(random.nextInt(2)).thenReturn(0, 1);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12342);
        verify(random, times(2)).nextInt(2);

        // Pessimize first local node - use second
        pool.pessimizeEndpoint(pool.getEndpoint(1));
        check(pool).records(4).knownNodes(4).needToReDiscovery(false).bestEndpointsCount(1);

        when(random.nextInt(1)).thenReturn(0);
        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12342);
        verify(random, times(1)).nextInt(1);

        // Pessimize second local node - use unlocal nodes
        pool.pessimizeEndpoint(pool.getEndpoint(2));
        check(pool).records(4).knownNodes(4).needToReDiscovery(false).bestEndpointsCount(2);

        when(random.nextInt(2)).thenReturn(1, 0);
        check(pool.getEndpoint(null)).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(null)).hostname("n3.ydb.tech").nodeID(3).port(12343);
        verify(random, times(4)).nextInt(2);

        // Pessimize all - fallback to use all nodes
        pool.pessimizeEndpoint(pool.getEndpoint(3));
        pool.pessimizeEndpoint(pool.getEndpoint(4));
        check(pool).records(4).knownNodes(4).needToReDiscovery(true).bestEndpointsCount(4);

        when(random.nextInt(4)).thenReturn(3, 2, 1, 0);
        check(pool.getEndpoint(null)).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(null)).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        verify(random, times(4)).nextInt(4);

        // setNewState reset all
        pool.setNewState(list("DC3",
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

        pool.setNewState(list("DC",
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

        when(random.nextInt(4)).thenReturn(2, 0, 3, 1);

        check(pool.getEndpoint(null)).hostname("n3.ydb.tech").nodeID(3).port(12343); // random
        check(pool.getEndpoint(0)).hostname("n1.ydb.tech").nodeID(1).port(12341); // random
        check(pool.getEndpoint(1)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(2)).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool.getEndpoint(3)).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(4)).hostname("n3.ydb.tech").nodeID(6).port(12344); // random
        check(pool.getEndpoint(5)).hostname("n2.ydb.tech").nodeID(2).port(12342); // random
        check(pool.getEndpoint(6)).hostname("n3.ydb.tech").nodeID(6).port(12344);

        verify(random, times(4)).nextInt(4);
    }

    @Test
    public void duplicateNodesTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false);

        pool.setNewState(list("DC",
                endpoint(1, "n1.ydb.tech", 12341, "DC"),
                endpoint(2, "n2.ydb.tech", 12342, "DC"),
                endpoint(2, "n3.ydb.tech", 12343, "DC")
        ));

        check(pool).records(3).knownNodes(2).needToReDiscovery(false).bestEndpointsCount(3);

        check(pool).record(0).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool).record(1).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool).record(2).hostname("n3.ydb.tech").nodeID(2).port(12343);

        when(random.nextInt(3)).thenReturn(1, 0, 2);

        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12342); // random
        check(pool.getEndpoint(0)).hostname("n1.ydb.tech").nodeID(1).port(12341); // random
        check(pool.getEndpoint(1)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(2)).hostname("n3.ydb.tech").nodeID(2).port(12343);
        check(pool.getEndpoint(3)).hostname("n3.ydb.tech").nodeID(2).port(12343); // random

        verify(random, times(3)).nextInt(3);
    }

    @Test
    public void removeEndpointsTest() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false);

        pool.setNewState(list("DC",
                endpoint(1, "n1.ydb.tech", 12341, "DC"),
                endpoint(2, "n2.ydb.tech", 12342, "DC"),
                endpoint(3, "n3.ydb.tech", 12343, "DC")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(3);

        check(pool).record(0).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool).record(1).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool).record(2).hostname("n3.ydb.tech").nodeID(3).port(12343);

        when(random.nextInt(3)).thenReturn(1, 0, 2);

        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12342); // random
        check(pool.getEndpoint(0)).hostname("n1.ydb.tech").nodeID(1).port(12341); // random
        check(pool.getEndpoint(1)).hostname("n1.ydb.tech").nodeID(1).port(12341);
        check(pool.getEndpoint(2)).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool.getEndpoint(3)).hostname("n3.ydb.tech").nodeID(3).port(12343);
        check(pool.getEndpoint(4)).hostname("n3.ydb.tech").nodeID(3).port(12343); // random

        verify(random, times(3)).nextInt(3);

        pool.setNewState(list("DC",
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

        when(random.nextInt(4)).thenReturn(3, 1, 2, 0);

        check(pool.getEndpoint(null)).hostname("n6.ydb.tech").nodeID(6).port(12346); // random
        check(pool.getEndpoint(0)).hostname("n4.ydb.tech").nodeID(4).port(12344); // random
        check(pool.getEndpoint(1)).hostname("n5.ydb.tech").nodeID(5).port(12345); // random
        check(pool.getEndpoint(2)).hostname("n2.ydb.tech").nodeID(2).port(12342);
        check(pool.getEndpoint(3)).hostname("n2.ydb.tech").nodeID(2).port(12342); // random
        check(pool.getEndpoint(4)).hostname("n4.ydb.tech").nodeID(4).port(12344);
        check(pool.getEndpoint(5)).hostname("n5.ydb.tech").nodeID(5).port(12345);
        check(pool.getEndpoint(6)).hostname("n6.ydb.tech").nodeID(6).port(12346);

        verify(random, times(4)).nextInt(4);
    }


    @Test
    public void detectLocalDCTest() {
        List<ServerSocket> servers = Arrays.stream(new int[]{8081, 8082, 8083})
                .mapToObj(
                        port -> {
                            try {
                                return ServerSocketFactory.getDefault()
                                        .createServerSocket(port);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                )
                .collect(Collectors.toList());

        EndpointPool pool = new EndpointPool(detectLocalDC());
        check(pool).records(0).knownNodes(0).needToReDiscovery(false);

        MockedStatic<Timer> systemMocked = mockStatic(Timer.class);

        long delta = 10_000_000;

        systemMocked.when(Timer::nanoTime).thenReturn(delta, 2 * delta,
                5 * delta, 10 * delta, 10 * delta, 20 * delta);

        pool.setNewState(list("DC",
                endpoint(1, "localhost", 8081, "DC1"),
                endpoint(2, "localhost", 8082, "DC2"),
                endpoint(3, "localhost", 8083, "DC3")
        ));

        check(pool).records(3).knownNodes(3).needToReDiscovery(false).bestEndpointsCount(1);

        check(pool.getEndpoint(null)).hostname("localhost").nodeID(2).port(8082); // detect local dc
        check(pool.getEndpoint(0)).hostname("localhost").nodeID(2).port(8082); // random from local dc
        check(pool.getEndpoint(1)).hostname("localhost").nodeID(1).port(8081);
        check(pool.getEndpoint(2)).hostname("localhost").nodeID(2).port(8082); // local dc
        check(pool.getEndpoint(3)).hostname("localhost").nodeID(3).port(8083);
        check(pool.getEndpoint(4)).hostname("localhost").nodeID(2).port(8082); // random from local dc

        systemMocked.verify(Timer::nanoTime, times(6));

        servers.forEach(serverSocket -> {
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        systemMocked.close();
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
            return new EndpointRecordChecker(pool.getRecords().get(idx));
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

    private static DiscoveryProtos.ListEndpointsResult list(String selfLocation, DiscoveryProtos.EndpointInfo... endpoints) {
        return DiscoveryProtos.ListEndpointsResult.newBuilder()
                .setSelfLocation(selfLocation)
                .addAllEndpoints(Arrays.asList(endpoints))
                .build();
    }

    private static DiscoveryProtos.EndpointInfo endpoint(int nodeID, String hostname, int port, String location) {
        return DiscoveryProtos.EndpointInfo.newBuilder()
                .setAddress(hostname)
                .setPort(port)
                .setNodeId(nodeID)
                .setLocation(location)
                .build();
    }

    private static DiscoveryProtos.EndpointInfo endpoint(int nodeID, String hostname, int port) {
        return DiscoveryProtos.EndpointInfo.newBuilder()
                .setAddress(hostname)
                .setPort(port)
                .setNodeId(nodeID)
                .build();
    }
}
