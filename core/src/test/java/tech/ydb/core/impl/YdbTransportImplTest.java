package tech.ydb.core.impl;


import java.time.Duration;
import java.time.ZoneId;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nonnull;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.ManagedChannelFactory;
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.proto.discovery.DiscoveryProtos;
import tech.ydb.proto.discovery.v1.DiscoveryServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbTransportImplTest {
    private final ScheduledExecutorService testScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ManagedChannel discoveryChannel = Mockito.mock(ManagedChannel.class);
    private final ManagedChannel transportChannel = Mockito.mock(ManagedChannel.class);
    private final ManagedChannelFactory channelFactory = Mockito.mock(ManagedChannelFactory.class);

    @Before
    public void setUp() throws InterruptedException {
        Mockito.when(discoveryChannel.getState(Mockito.anyBoolean())).thenReturn(ConnectivityState.READY);
        Mockito.when(discoveryChannel.shutdown()).thenReturn(discoveryChannel);
        Mockito.when(discoveryChannel.shutdownNow()).thenReturn(discoveryChannel);
        Mockito.when(discoveryChannel.awaitTermination(Mockito.anyLong(), Mockito.any())).thenReturn(true);

        Mockito.when(transportChannel.getState(Mockito.anyBoolean())).thenReturn(ConnectivityState.READY);
        Mockito.when(transportChannel.shutdown()).thenReturn(transportChannel);
        Mockito.when(transportChannel.shutdownNow()).thenReturn(transportChannel);
        Mockito.when(transportChannel.awaitTermination(Mockito.anyLong(), Mockito.any())).thenReturn(true);

        Mockito.when(channelFactory.newManagedChannel(Mockito.eq("mocked"), Mockito.eq(2136), Mockito.isNull()))
                .thenReturn(discoveryChannel);
        Mockito.when(channelFactory.newManagedChannel(Mockito.eq("node"), Mockito.eq(2136), Mockito.isNull()))
                .thenReturn(transportChannel);
    }

    @After
    public void shutdown() {
        testScheduler.shutdown();
    }

    private CompletableFuture<Result<DiscoveryProtos.WhoAmIResult>> whoAmI(GrpcTransport transport) {
        GrpcRequestSettings settings = GrpcRequestSettings.newBuilder().build();
        DiscoveryProtos.WhoAmIRequest request = DiscoveryProtos.WhoAmIRequest.newBuilder().build();
        return transport.unaryCall(DiscoveryServiceGrpc.getWhoAmIMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(
                        DiscoveryProtos.WhoAmIResponse::getOperation,
                        DiscoveryProtos.WhoAmIResult.class
                ));
    }

    @Test
    public void defaultBuildGoodTest() {
        Mockito.when(discoveryChannel.newCall(Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()))
                .thenReturn(MockedCall.discovery(testScheduler, "self", new EndpointRecord("node", 2136)));

        Mockito.when(transportChannel.newCall(Mockito.eq(DiscoveryServiceGrpc.getWhoAmIMethod()), Mockito.any()))
                .thenReturn(MockedCall.whoAmICall(testScheduler, "i am node"));

        GrpcTransport transport = GrpcTransport.forConnectionString("grpc://mocked:2136/local")
                .withChannelFactoryBuilder(builder -> channelFactory)
                .build();

        Result<DiscoveryProtos.WhoAmIResult> result = whoAmI(transport).join();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals("i am node", result.getValue().getUser());

        transport.close();
    }

    @Test
    public void defaultBuildUnavailableTest() {
        Mockito.when(discoveryChannel.newCall(Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()))
                .thenReturn(MockedCall.unavailable(testScheduler));

        IllegalStateException ex = Assert.assertThrows(IllegalStateException.class,
                () -> GrpcTransport.forConnectionString("grpc://mocked:2136/local")
                        .withDiscoveryTimeout(Duration.ofMillis(100))
                        .withChannelFactoryBuilder(builder -> channelFactory)
                        .build()
        );

        Assert.assertEquals("Discovery failed", ex.getMessage());
        Assert.assertTrue(ex.getCause() instanceof UnexpectedResultException);

        UnexpectedResultException unexpected = (UnexpectedResultException) ex.getCause();
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, unexpected.getStatus().getCode());
    }

    @Test
    public void defaultBuildNotReadyTest() {
        Mockito.when(discoveryChannel.newCall(Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()))
                .thenReturn(MockedCall.neverAnswer(testScheduler));

        IllegalStateException ex = Assert.assertThrows(IllegalStateException.class,
                () -> GrpcTransport.forConnectionString("grpc://mocked:2136/local")
                        .withDiscoveryTimeout(Duration.ofMillis(200))
                        .withChannelFactoryBuilder(builder -> channelFactory)
                        .build()
        );

        Assert.assertEquals("Discovery is not ready", ex.getMessage());
        Assert.assertNull(ex.getCause());
    }

    @Test
    public void asyncBuildGoodTest() {
        Ticker tickerRequests = new Ticker();
        MockedScheduler scheduler = new MockedScheduler(MockedClock.create(ZoneId.of("UTC")));

        Mockito.when(discoveryChannel.newCall(Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()))
                .thenReturn(MockedCall.discovery("self", new EndpointRecord("node", 2136)));

        Mockito.when(discoveryChannel.newCall(Mockito.eq(DiscoveryServiceGrpc.getWhoAmIMethod()), Mockito.any()))
                .thenReturn(MockedCall.whoAmICall(tickerRequests, "i am discovery"));
        Mockito.when(transportChannel.newCall(Mockito.eq(DiscoveryServiceGrpc.getWhoAmIMethod()), Mockito.any()))
                .thenReturn(MockedCall.whoAmICall(tickerRequests, "i am node"));

        CompletableFuture<Void> isReady = new CompletableFuture<>();

        @SuppressWarnings("deprecation")
        GrpcTransport transport = GrpcTransport.forConnectionString("grpc://mocked:2136/local")
                .withSchedulerFactory(() -> scheduler)
                .withChannelFactoryBuilder(builder -> channelFactory)
                .buildAsync(() -> isReady.complete(null));

        Assert.assertNotNull(transport);
        Assert.assertFalse(isReady.isDone());

        // before discovery completed we send requests to discovery endpoint
        tickerRequests.noTasks();
        CompletableFuture<Result<DiscoveryProtos.WhoAmIResult>> f1 = whoAmI(transport);
        Assert.assertFalse(f1.isDone());

        tickerRequests.runNextTask().noTasks();
        Assert.assertTrue(f1.isDone());
        Assert.assertEquals("i am discovery", f1.join().getValue().getUser());

        // Complete discovery
        Assert.assertFalse(isReady.isDone());
        scheduler.hasTasksCount(2).runNextTask().runNextTask();

        CompletableFuture<Result<DiscoveryProtos.WhoAmIResult>> f2 = whoAmI(transport);
        Assert.assertFalse(f2.isDone());
        tickerRequests.runNextTask().noTasks();
        Assert.assertTrue(f2.isDone());
        Assert.assertEquals("i am node", f2.join().getValue().getUser());

        Assert.assertTrue(isReady.isDone());
    }

    @Test
    public void failFastOnMissingPort() {
        String endpoint = "127.1.2.3";
        try {
            GrpcTransport.forEndpoint(endpoint, "test").build().close();
            Assert.fail("Exception expected");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Can't create discovery rpc, port is not specified for endpoint 127.1.2.3", e.getMessage());
        }
    }

    @Test
    public void defaultSchedulerFactoryTest() {
        Mockito.when(discoveryChannel.newCall(Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()))
                .thenReturn(MockedCall.discovery("self", new EndpointRecord("node", 2136)));

        GrpcTransport transport = GrpcTransport.forEndpoint("grpc://mocked:2136", "/local")
                .withChannelFactoryBuilder(builder -> channelFactory)
                .build();

        ScheduledExecutorService scheduler = transport.getScheduler();
        Assert.assertNotNull(scheduler);
        Assert.assertFalse(scheduler.isShutdown());
        Assert.assertFalse(scheduler.isTerminated());

        transport.close();

        Assert.assertSame(scheduler, transport.getScheduler());
        Assert.assertTrue(scheduler.isShutdown());
        Assert.assertTrue(scheduler.isTerminated());
    }

    @Test
    public void customSchedulerTest() {
        Mockito.when(discoveryChannel.newCall(Mockito.eq(DiscoveryServiceGrpc.getListEndpointsMethod()), Mockito.any()))
                .thenReturn(MockedCall.discovery("self", new EndpointRecord("node", 2136)));

        ScheduledExecutorService custom = Executors.newSingleThreadScheduledExecutor();

        GrpcTransport transport = GrpcTransport.forEndpoint("grpc://mocked:2136", "/local")
                .withChannelFactoryBuilder(builder -> channelFactory)
                .withScheduler(custom)
                .build();

        ScheduledExecutorService scheduler = transport.getScheduler();
        Assert.assertNotNull(scheduler);
        Assert.assertFalse(scheduler.isShutdown());
        Assert.assertFalse(scheduler.isTerminated());

        transport.close();

        Assert.assertSame(scheduler, transport.getScheduler());
        Assert.assertFalse(scheduler.isShutdown());
        Assert.assertFalse(scheduler.isTerminated());

        YdbSchedulerFactory.shutdownScheduler(custom);
    }

    public static class Ticker implements Executor {
        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

        @Override
        public void execute(@Nonnull Runnable command) {
            tasks.offer(command);
        }

        public Ticker runNextTask() {
            Runnable next = tasks.poll();
            Assert.assertNotNull("Executor's queue is empty", next);
            next.run();
            return this;
        }

        public Ticker noTasks() {
            Assert.assertTrue("Executor have extra tasks", tasks.isEmpty());
            return this;
        }
    }
}
