package tech.ydb.core.impl.pool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.impl.YdbSchedulerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcChannelPoolTest {
    private final ManagedChannelFactory factoryMock = Mockito.mock(ManagedChannelFactory.class);
    private final ScheduledExecutorService scheduler = YdbSchedulerFactory.createScheduler();
    private final List<String> events = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> awaitTimeouts = Collections.synchronizedList(new ArrayList<>());

    @Before
    public void setUp() {
        Mockito.when(factoryMock.newManagedChannel(Mockito.any(), Mockito.anyInt(), Mockito.isNull()))
                .then((args) -> ManagedChannelMock.good());
    }

    @After
    public void tearDown() {
        YdbSchedulerFactory.shutdownScheduler(scheduler);
    }

    @Test
    public void simpleTest() {
        EndpointRecord e1 = new EndpointRecord("host1", 1234, 10, null, null);
        EndpointRecord e2 = new EndpointRecord("host1", 1235, 11, null, null);

        GrpcChannelPool pool = new GrpcChannelPool(factoryMock, scheduler);
        Assert.assertEquals(0, pool.getChannels().size());

        GrpcChannel channel1 = pool.getChannel(e1);
        Assert.assertNotNull(channel1);
        Assert.assertEquals(e1, channel1.getEndpoint());
        Assert.assertEquals(1, pool.getChannels().size());
        Assert.assertSame(channel1, pool.getChannel(e1));

        GrpcChannel channel2 = pool.getChannel(e2);
        Assert.assertNotNull(channel2);
        Assert.assertEquals(e2, channel2.getEndpoint());
        Assert.assertEquals(2, pool.getChannels().size());
        Assert.assertSame(channel2, pool.getChannel(e2));

        Assert.assertFalse(channel1.isShutdown());
        Assert.assertFalse(channel2.isShutdown());

        pool.shutdown().join(); // shutdown doesn't remove channels from pool
        Assert.assertEquals(2, pool.getChannels().size());
        Assert.assertTrue(channel1.isShutdown());
        Assert.assertTrue(channel2.isShutdown());

        pool.shutdown().join(); // double shutdown is ok
        Assert.assertEquals(2, pool.getChannels().size());
    }

    @Test
    public void removeChannels() {
        EndpointRecord e1 = new EndpointRecord("host1", 1234, 10, null, null);
        EndpointRecord e2 = new EndpointRecord("host1", 1235, 11, null, null);
        EndpointRecord e3 = new EndpointRecord("host1", 1236, 12, null, null);

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
        Assert.assertTrue(pool.removeChannels(null).join());
        Assert.assertEquals(3, pool.getChannels().size());

        // empty arguments - nothing happens
        pool.removeChannels(Collections.emptyList());
        Assert.assertEquals(3, pool.getChannels().size());

        // remove channel1
        Assert.assertTrue(pool.removeChannels(Collections.singletonList(e1)).join());
        Assert.assertEquals(2, pool.getChannels().size());

        Assert.assertTrue(channel1.isShutdown());
        Assert.assertFalse(channel2.isShutdown());
        Assert.assertFalse(channel3.isShutdown());

        // second remove channel1 - nothing happens
        Assert.assertTrue(pool.removeChannels(Collections.singletonList(e1)).join());
        Assert.assertEquals(2, pool.getChannels().size());

        Assert.assertTrue(channel1.isShutdown());
        Assert.assertFalse(channel2.isShutdown());
        Assert.assertFalse(channel3.isShutdown());

        // second remove channel2 - nothing happens
        Assert.assertTrue(pool.removeChannels(Arrays.asList(e2, e2, e2)).join());
        Assert.assertEquals(1, pool.getChannels().size());

        Assert.assertTrue(channel1.isShutdown());
        Assert.assertTrue(channel2.isShutdown());
        Assert.assertFalse(channel3.isShutdown());

        pool.shutdown().join();
    }

    @Test
    public void badShutdownTest() {
        Mockito.when(factoryMock.newManagedChannel(Mockito.any(), Mockito.anyInt(), Mockito.isNull())).thenReturn(
                ManagedChannelMock.good(), ManagedChannelMock.good(),
                ManagedChannelMock.wrongShutdown(), ManagedChannelMock.wrongShutdown());

        EndpointRecord e1 = new EndpointRecord("host1", 1234, 10, null, null);
        EndpointRecord e2 = new EndpointRecord("host1", 1235, 11, null, null);
        EndpointRecord e3 = new EndpointRecord("host1", 1236, 12, null, null);

        GrpcChannelPool pool = new GrpcChannelPool(factoryMock, scheduler);
        Assert.assertEquals(0, pool.getChannels().size());

        // create three channels
        GrpcChannel channel1 = pool.getChannel(e1);
        GrpcChannel channel2 = pool.getChannel(e2);
        GrpcChannel channel3 = pool.getChannel(e3);

        Assert.assertFalse(channel1.isShutdown());
        Assert.assertFalse(channel2.isShutdown());
        Assert.assertFalse(channel3.isShutdown());

        Assert.assertFalse(pool.removeChannels(Collections.singletonList(e3)).join());
        Assert.assertEquals(2, pool.getChannels().size());

        Assert.assertTrue(pool.removeChannels(Collections.singletonList(e3)).join());
        Assert.assertEquals(2, pool.getChannels().size());

        GrpcChannel channel4 = pool.getChannel(e3);
        Assert.assertNotEquals(channel3, channel4);

        pool.shutdown().join();

        Assert.assertTrue(channel1.isShutdown());
        Assert.assertTrue(channel2.isShutdown());
        Assert.assertTrue(channel3.isShutdown());
        Assert.assertTrue(channel4.isShutdown());
    }

    /**
     * Channel with active calls which don't complete for {@code awaitMillis}
     */
    private ManagedChannel busyChannel(String name, long awaitMillis, CountDownLatch awaitStarted)
            throws InterruptedException {
        ManagedChannel channel = Mockito.mock(ManagedChannel.class);
        Mockito.when(channel.getState(Mockito.anyBoolean())).thenReturn(ConnectivityState.READY);
        Mockito.when(channel.shutdown()).then(inv -> {
            events.add("shutdown " + name);
            return channel;
        });
        Mockito.when(channel.shutdownNow()).then(inv -> {
            events.add("shutdownNow " + name);
            return channel;
        });
        Mockito.when(channel.awaitTermination(Mockito.anyLong(), Mockito.any())).then(inv -> {
            events.add("await " + name);
            awaitTimeouts.add(inv.<TimeUnit>getArgument(1).toNanos(inv.getArgument(0)));
            awaitStarted.countDown();
            Thread.sleep(awaitMillis);
            return false;
        });
        return channel;
    }

    private GrpcChannelPool poolWithBusyChannels(ScheduledExecutorService executor, long awaitMillis,
            CountDownLatch awaitStarted) throws InterruptedException {
        ManagedChannel c1 = busyChannel("c1", awaitMillis, awaitStarted);
        ManagedChannel c2 = busyChannel("c2", awaitMillis, awaitStarted);
        ManagedChannel c3 = busyChannel("c3", awaitMillis, awaitStarted);
        Mockito.when(factoryMock.newManagedChannel(Mockito.any(), Mockito.anyInt(), Mockito.isNull()))
                .thenReturn(c1, c2, c3);

        GrpcChannelPool pool = new GrpcChannelPool(factoryMock, executor);
        pool.getChannel(new EndpointRecord("host1", 1234, 10, null, null));
        pool.getChannel(new EndpointRecord("host1", 1235, 11, null, null));
        pool.getChannel(new EndpointRecord("host1", 1236, 12, null, null));
        return pool;
    }

    @Test(timeout = 10_000)
    public void channelsShutdownInParallelTest() throws InterruptedException {
        GrpcChannelPool pool = poolWithBusyChannels(scheduler, 300, new CountDownLatch(1));

        Assert.assertFalse(pool.shutdown().join());

        // all channels are shut down before waiting for any of them
        Assert.assertEquals(events.toString(), 9, events.size());

        for (int i = 0; i < 3; i++) {
            Assert.assertTrue(events.toString(), events.get(i).startsWith("shutdown "));
        }

        Assert.assertTrue(events.toString(), events.containsAll(Arrays.asList(
                "shutdownNow c1", "shutdownNow c2", "shutdownNow c3"
        )));

        // and they share one deadline, so the last channel waits only for the rest of it
        Assert.assertEquals(3, awaitTimeouts.size());
        long fullTimeout = TimeUnit.MILLISECONDS.toNanos(GrpcChannel.WAIT_FOR_CLOSING_MS);
        long lastTimeout = awaitTimeouts.get(2);
        Assert.assertTrue("Unexpected timeout " + lastTimeout, lastTimeout <= fullTimeout - 2 * 300_000_000L);
    }

    @Test(timeout = 10_000)
    public void interruptedShutdownClosesAllChannelsTest() throws InterruptedException {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch awaitStarted = new CountDownLatch(1);
        GrpcChannelPool pool = poolWithBusyChannels(executor, 60_000, awaitStarted);

        CompletableFuture<Boolean> shutdown = pool.shutdown();
        awaitStarted.await();
        executor.shutdownNow(); // interrupts waiting of the channel termination

        Assert.assertFalse(shutdown.join());
        Assert.assertTrue(events.toString(), events.containsAll(Arrays.asList(
                "shutdown c1", "shutdown c2", "shutdown c3",
                "shutdownNow c1", "shutdownNow c2", "shutdownNow c3"
        )));
    }
}
