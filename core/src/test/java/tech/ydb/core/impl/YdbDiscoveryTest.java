package tech.ydb.core.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.ManagedChannelFactory;
import tech.ydb.proto.discovery.v1.DiscoveryServiceGrpc;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbDiscoveryTest {
    private final MockedClock clock = MockedClock.create(ZoneId.of("UTC"));
    private final MockedScheduler scheduler = new MockedScheduler(clock);
    private final ManagedChannel channel = Mockito.mock(ManagedChannel.class);
    private final ManagedChannelFactory channelFactory = Mockito.mock(ManagedChannelFactory.class);

    @Before
    public void setUp() throws InterruptedException {
        Mockito.when(channel.getState(Mockito.anyBoolean())).thenReturn(ConnectivityState.READY);
        Mockito.when(channel.shutdown()).thenReturn(channel);
        Mockito.when(channel.shutdownNow()).thenReturn(channel);
        Mockito.when(channel.awaitTermination(Mockito.anyLong(), Mockito.any())).thenReturn(true);

        Mockito.when(channelFactory.newManagedChannel(Mockito.any(), Mockito.anyInt(), Mockito.isNull())).thenReturn(channel);
    }

    private <T extends Throwable> T checkFutureException(CompletableFuture<Boolean> f, String message, Class<T> clazz) {
        CompletionException ce = Assert.assertThrows(CompletionException.class, f::join);
        Assert.assertNotNull(ce.getCause());
        Assert.assertTrue(ce.getCause() instanceof IllegalStateException);

        IllegalStateException ex = (IllegalStateException) ce.getCause();
        Assert.assertEquals(message, ex.getMessage());
        if (clazz == null) {
            Assert.assertNull(ex.getCause());
            return null;
        }

        Assert.assertTrue(clazz.isAssignableFrom(ex.getCause().getClass()));
        return clazz.cast(ex.getCause());
    }

    private CompletableFuture<Boolean> createWaitingFuture(YdbDiscovery discovery) {
        return CompletableFuture.supplyAsync(() -> {
            discovery.waitReady(100);
            return Boolean.TRUE;
        });
    }

    private void verifyDiscoveryCount(int times) {
        Mockito.verify(channel, Mockito.times(times)).newCall(
                Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()
        );
    }


    @Test
    public void baseTest() {
        Mockito.when(channel.newCall(Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()))
                .thenReturn(MockedCall.discovery("self", new EndpointRecord("localhost", 12340)));

        scheduler.hasNoTasks();

        YdbDiscovery discovery = new YdbDiscovery(new TestHandler(), scheduler, "/base", Duration.ofMillis(100));
        discovery.start();

        verifyDiscoveryCount(0);
        scheduler.hasTasksCount(1).runNextTask();
        verifyDiscoveryCount(1);

        discovery.waitReady(-1);
        discovery.stop();

        // stop is imdepotent operation
        discovery.stop();
    }

    @Test
    public void baseDiscoveryForcing() {
        Mockito.when(channel.newCall(Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()))
                .thenReturn(MockedCall.discovery("self", new EndpointRecord("localhost", 12340)))
                .thenReturn(MockedCall.discovery("self", new EndpointRecord("localhost", 12340)))
                .thenReturn(MockedCall.discovery("self", new EndpointRecord("localhost", 12340)));

        Duration tick = Duration.ofSeconds(5); // == YdbDiscovery.DISCOVERY_PERIOD_MIN_SECONDS
        Duration normal = Duration.ofSeconds(60); // == YdbDiscovery.DISCOVERY_PERIOD_NORMAL_SECONDS

        TestHandler handler = new TestHandler();
        handler.force = false;

        scheduler.hasNoTasks();

        YdbDiscovery discovery = new YdbDiscovery(handler, scheduler, "/forcing", Duration.ofMillis(100));

        discovery.start();
        // first discovery
        verifyDiscoveryCount(0);
        scheduler.hasTasksCount(1).runNextTask();
        verifyDiscoveryCount(1);

        // few next ticks will be dump
        Instant started = clock.instant();
        Instant lastUpdate = started;

        scheduler.hasTasksCount(1).runNextTask();
        while (!clock.instant().isAfter(started.plus(normal))) {
            verifyDiscoveryCount(1);

            Assert.assertFalse("" + clock.instant() + " must be after " + lastUpdate.plus(tick),
                    clock.instant().isBefore(lastUpdate.plus(tick)));
            lastUpdate = clock.instant();

            scheduler.hasTasksCount(1).runNextTask();
        }

        // second discovery
        verifyDiscoveryCount(2);
        scheduler.hasTasksCount(1);

        // force discovery
        handler.force = true;

        scheduler.runNextTask();
        verifyDiscoveryCount(3);

        discovery.stop();
    }

    @Test
    public void waitWaitingTest() {
        Mockito.when(channel.newCall(Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()))
                .thenReturn(MockedCall.discovery("self", new EndpointRecord("localhost", 12340)));

        scheduler.hasNoTasks();

        final YdbDiscovery discovery = new YdbDiscovery(new TestHandler(), scheduler, "/wait", Duration.ofMillis(500));
        discovery.start();

        scheduler.hasTasksCount(1);

        CompletableFuture<Boolean> req1 = createWaitingFuture(discovery);
        CompletableFuture<Boolean> req2 = createWaitingFuture(discovery);
        Assert.assertFalse(req1.isDone());
        Assert.assertFalse(req2.isDone());

        verifyDiscoveryCount(0);
        scheduler.runNextTask();
        verifyDiscoveryCount(1);

        Assert.assertTrue(req1.join());
        Assert.assertTrue(req1.join());

        CompletableFuture<Boolean> req3 = createWaitingFuture(discovery);
        CompletableFuture<Boolean> req4 = createWaitingFuture(discovery);

        Assert.assertTrue(req3.join());
        Assert.assertTrue(req4.join());

        discovery.stop();
    }

    @Test
    public void failedDiscoveryTest() {
        Mockito.when(channel.newCall(Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()))
                .thenReturn(MockedCall.unavailable())
                .thenReturn(MockedCall.discoveryInternalError())
                .thenReturn(MockedCall.discovery("self")) // Empty discovery is an error too
                .thenThrow(new RuntimeException("Test io problem"));

        TestHandler handler = new TestHandler();
        handler.force = true;

        scheduler.hasNoTasks();

        YdbDiscovery discovery = new YdbDiscovery(handler, scheduler, "/failed", Duration.ofMillis(100));
        discovery.start();

        // test not ready error
        CompletableFuture<Boolean> req1 = createWaitingFuture(discovery);
        scheduler.hasTasksCount(1);
        Assert.assertFalse(req1.isDone());
        Assert.assertNull(checkFutureException(req1, "Discovery is not ready", null));

        // test discovery status
        CompletableFuture<Boolean> req2 = createWaitingFuture(discovery);
        Assert.assertFalse(req2.isDone());

        scheduler.hasTasksCount(1).runNextTask();
        verifyDiscoveryCount(1);

        UnexpectedResultException ex2 = checkFutureException(req2, "Discovery failed", UnexpectedResultException.class);
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, ex2.getStatus().getCode());

        // test discovery other status
        CompletableFuture<Boolean> req3 = createWaitingFuture(discovery);
        Assert.assertFalse(req3.isDone());

        scheduler.hasTasksCount(1).runNextTask();
        verifyDiscoveryCount(2);

        UnexpectedResultException ex3 = checkFutureException(req3, "Discovery failed", UnexpectedResultException.class);
        Assert.assertEquals(StatusCode.INTERNAL_ERROR, ex3.getStatus().getCode());

        // test empty discovery
        CompletableFuture<Boolean> req4 = createWaitingFuture(discovery);
        Assert.assertFalse(req4.isDone());

        scheduler.hasTasksCount(1).runNextTask();
        verifyDiscoveryCount(3);

        UnexpectedResultException ex4 = checkFutureException(req4, "Discovery failed", UnexpectedResultException.class);
        Assert.assertEquals(StatusCode.CLIENT_DISCOVERY_FAILED, ex4.getStatus().getCode());

        // test discovery throwable
        CompletableFuture<Boolean> req5 = createWaitingFuture(discovery);
        Assert.assertFalse(req5.isDone());

        scheduler.hasTasksCount(1).runNextTask();
        verifyDiscoveryCount(4);

        UnexpectedResultException ex5 = checkFutureException(req5, "Discovery failed", UnexpectedResultException.class);
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, ex5.getStatus().getCode());
        Assert.assertEquals("Test io problem, code: CLIENT_INTERNAL_ERROR", ex5.getMessage());

        discovery.stop();
    }

    private class TestHandler implements YdbDiscovery.Handler {
        private volatile boolean force = false;

        @Override
        public Instant instant() {
            return clock.instant();
        }

        @Override
        public GrpcTransport createDiscoveryTransport() {
            EndpointRecord discovery = new EndpointRecord("unknown", 1234);
            return new FixedCallOptionsTransport(scheduler, new AuthCallOptions(), "/test", discovery, channelFactory);
        }

        @Override
        public boolean needToForceDiscovery() {
            return force;
        }

        @Override
        public CompletableFuture<Boolean> handleEndpoints(List<EndpointRecord> endpoints, String selfLocation) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
    }
}
