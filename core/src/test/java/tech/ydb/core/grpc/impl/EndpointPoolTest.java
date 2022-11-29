package tech.ydb.core.grpc.impl;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.OngoingStubbing;

import tech.ydb.core.grpc.BalancingPolicy;
import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.discovery.DiscoveryProtos;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class EndpointPoolTest {
    private final AutoCloseable mocks = MockitoAnnotations.openMocks(this);
    private final MockedStatic<ThreadLocalRandom> threadLocalStaticMock = Mockito.mockStatic(ThreadLocalRandom.class);

    @Mock
    private ThreadLocalRandom random;

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
    public void testUseAllNodes() {
        EndpointPool pool = new EndpointPool(useAllNodes());
        check(pool).records(0).knownEndpoints(0).needToReDiscovery(false);

        pool.setNewState(list("DC1",
                endpoint(1, "n1.ydb.tech", 12345, "DC1"),
                endpoint(2, "n2.ydb.tech", 12345, "DC2"),
                endpoint(3, "n3.ydb.tech", 12345, "DC3")
        ));

        check(pool).records(3).knownEndpoints(3).needToReDiscovery(false);

        mockMethod(random.nextInt(Mockito.eq(3)), 2, 0, 2, 1);

        check(pool.getEndpoint(null)).hostname("n3.ydb.tech").nodeID(3).port(12345);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12345);
        check(pool.getEndpoint(null)).hostname("n3.ydb.tech").nodeID(3).port(12345);
        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12345);
    }

    @Test
    public void testLocalDC() {
        EndpointPool pool = new EndpointPool(prefferedNode(null));
        check(pool).records(0).knownEndpoints(0).needToReDiscovery(false);

        pool.setNewState(list("DC2",
                endpoint(1, "n1.ydb.tech", 12345, "DC1"),
                endpoint(2, "n2.ydb.tech", 12345, "DC2"),
                endpoint(3, "n3.ydb.tech", 12345, "DC3")
        ));

        check(pool).records(3).knownEndpoints(3).needToReDiscovery(false);

        mockMethod(random.nextInt(Mockito.eq(3)), 2, 0, 2, 1);

        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12345);
        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12345);
        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12345);
        check(pool.getEndpoint(null)).hostname("n2.ydb.tech").nodeID(2).port(12345);
    }

    @Test
    public void testPreffedDC() {
        EndpointPool pool = new EndpointPool(prefferedNode("DC1"));
        check(pool).records(0).knownEndpoints(0).needToReDiscovery(false);

        pool.setNewState(list("DC3",
                endpoint(1, "n1.ydb.tech", 12345, "DC1"),
                endpoint(2, "n2.ydb.tech", 12345, "DC2"),
                endpoint(3, "n3.ydb.tech", 12345, "DC3")
        ));

        check(pool).records(3).knownEndpoints(3).needToReDiscovery(false);

        mockMethod(random.nextInt(Mockito.eq(3)), 2, 0, 2, 1);

        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12345);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12345);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12345);
        check(pool.getEndpoint(null)).hostname("n1.ydb.tech").nodeID(1).port(12345);
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

        public PoolChecker knownEndpoints(int size) {
            Assert.assertEquals("Check known endpoints keys size", size, pool.getKnownEndpoints().size());
            return this;
        }

        public PoolChecker needToReDiscovery(boolean value) {
            Assert.assertEquals("Check need to rediscovery", value, pool.needToRunDiscovery());
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
        return BalancingSettings.fromPolicy(BalancingPolicy.USE_ALL_NODES);
    }

    private static BalancingSettings prefferedNode(String selfLocaltion) {
        return BalancingSettings.fromLocation(selfLocaltion);
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

    private static void mockMethod(int methodCall, int... values) {
        OngoingStubbing<Integer> stubbing = Mockito.when(methodCall);
        for (int value: values) {
            stubbing = stubbing.thenReturn(value);
        }
    }
}
