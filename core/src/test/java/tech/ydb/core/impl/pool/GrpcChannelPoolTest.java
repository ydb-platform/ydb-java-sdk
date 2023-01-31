package tech.ydb.core.impl.pool;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.impl.YdbSchedulerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcChannelPoolTest {
    private static final GrpcTransportBuilder BUILDER = GrpcTransport.forHost("ydb.tech", 1245, "/Local");

    private final AutoCloseable mocks = MockitoAnnotations.openMocks(this);
    private final MockedStatic<ManagedChannelFactory> factoryStaticMock = Mockito.mockStatic(ManagedChannelFactory.class);
    private final ManagedChannelFactory factoryMock = Mockito.mock(ManagedChannelFactory.class);
    private final ScheduledExecutorService scheduler = YdbSchedulerFactory.createScheduler();

    @Before
    public void setUp() {
        factoryStaticMock.when(() -> ManagedChannelFactory.fromBuilder(BUILDER)).thenReturn(factoryMock);
        Mockito.when(factoryMock.getConnectTimeoutMs()).thenReturn(500l); // timeout for ready watcher
        Mockito.when(factoryMock.newManagedChannel(Mockito.any(), Mockito.anyInt()))
                .then((args) -> ManagedChannelMock.good());
    }

    @After
    public void tearDown() throws Exception {
        factoryStaticMock.close();
        mocks.close();
        YdbSchedulerFactory.shutdownScheduler(scheduler);
    }

    @Test
    public void simpleTest() {
        EndpointRecord e1 = new EndpointRecord("host1", 1234, 10);
        EndpointRecord e2 = new EndpointRecord("host1", 1235, 11);

        GrpcChannelPool pool = new GrpcChannelPool(factoryMock, scheduler);
        Assert.assertEquals(0, pool.getChannels().size());

        GrpcChannel channel1 = pool.getChannel(e1);
        Assert.assertNotNull(channel1);
        Assert.assertEquals(e1, channel1.getEndpoint());
        Assert.assertEquals(1, pool.getChannels().size());

        GrpcChannel channel2 = pool.getChannel(e2);
        Assert.assertNotNull(channel2);
        Assert.assertEquals(e2, channel2.getEndpoint());
        Assert.assertEquals(2, pool.getChannels().size());

        Assert.assertFalse(channel1.isShutdown());
        Assert.assertFalse(channel2.isShutdown());

        pool.shutdown(); // shutdown doesn't remove channels from pool
        Assert.assertEquals(2, pool.getChannels().size());
        Assert.assertTrue(channel1.isShutdown());
        Assert.assertTrue(channel2.isShutdown());

        pool.shutdown(); // double shutdown is ok
        Assert.assertEquals(2, pool.getChannels().size());
    }

    @Test
    public void removeChannels() {
        EndpointRecord e1 = new EndpointRecord("host1", 1234, 10);
        EndpointRecord e2 = new EndpointRecord("host1", 1235, 11);
        EndpointRecord e3 = new EndpointRecord("host1", 1236, 12);

        GrpcChannelPool pool = new GrpcChannelPool(factoryMock, scheduler);
        Assert.assertEquals(0, pool.getChannels().size());

        // create three channels
        GrpcChannel channel1 = pool.getChannel(e1);
        GrpcChannel channel2 = pool.getChannel(e2);
        GrpcChannel channel3 = pool.getChannel(e3);

        Assert.assertFalse(channel1.isShutdown());
        Assert.assertFalse(channel2.isShutdown());
        Assert.assertFalse(channel3.isShutdown());

        Assert.assertEquals(3, pool.getChannels().size());

        // null arguments - nothing happens
        Assert.assertTrue(pool.removeChannels(null));
        Assert.assertEquals(3, pool.getChannels().size());

        // empty arguments - nothing happens
        pool.removeChannels(Collections.emptyList());
        Assert.assertEquals(3, pool.getChannels().size());

        // remove channel1
        Assert.assertTrue(pool.removeChannels(Arrays.asList(e1)));
        Assert.assertEquals(2, pool.getChannels().size());

        Assert.assertTrue(channel1.isShutdown());
        Assert.assertFalse(channel2.isShutdown());
        Assert.assertFalse(channel3.isShutdown());

        // second remove channel1 - nothing happens
        Assert.assertTrue(pool.removeChannels(Arrays.asList(e1)));
        Assert.assertEquals(2, pool.getChannels().size());

        Assert.assertTrue(channel1.isShutdown());
        Assert.assertFalse(channel2.isShutdown());
        Assert.assertFalse(channel3.isShutdown());

        // second remove channel2 - nothing happens
        Assert.assertTrue(pool.removeChannels(Arrays.asList(e2, e2, e2)));
        Assert.assertEquals(1, pool.getChannels().size());

        Assert.assertTrue(channel1.isShutdown());
        Assert.assertTrue(channel2.isShutdown());
        Assert.assertFalse(channel3.isShutdown());

        pool.shutdown();
    }

    @Test
    public void badShutdownTest() {
        Mockito.when(factoryMock.newManagedChannel(Mockito.any(), Mockito.anyInt())).thenReturn(
                ManagedChannelMock.good(), ManagedChannelMock.good(),
                ManagedChannelMock.wrongShutdown(), ManagedChannelMock.wrongShutdown());

        EndpointRecord e1 = new EndpointRecord("host1", 1234, 10);
        EndpointRecord e2 = new EndpointRecord("host1", 1235, 11);
        EndpointRecord e3 = new EndpointRecord("host1", 1236, 12);

        GrpcChannelPool pool = new GrpcChannelPool(factoryMock, scheduler);
        Assert.assertEquals(0, pool.getChannels().size());

        // create three channels
        GrpcChannel channel1 = pool.getChannel(e1);
        GrpcChannel channel2 = pool.getChannel(e2);
        GrpcChannel channel3 = pool.getChannel(e3);

        Assert.assertFalse(channel1.isShutdown());
        Assert.assertFalse(channel2.isShutdown());
        Assert.assertFalse(channel3.isShutdown());

        Assert.assertFalse(pool.removeChannels(Arrays.asList(e3)));
        Assert.assertEquals(2, pool.getChannels().size());

        Assert.assertTrue(pool.removeChannels(Arrays.asList(e3)));
        Assert.assertEquals(2, pool.getChannels().size());

        GrpcChannel channel4 = pool.getChannel(e3);
        Assert.assertNotEquals(channel3, channel4);

        pool.shutdown();

        Assert.assertTrue(channel1.isShutdown());
        Assert.assertTrue(channel2.isShutdown());
        Assert.assertTrue(channel3.isShutdown());
        Assert.assertTrue(channel4.isShutdown());
    }
}
