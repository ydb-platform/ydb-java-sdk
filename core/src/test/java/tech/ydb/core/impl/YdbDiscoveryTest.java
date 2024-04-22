package tech.ydb.core.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.pool.EndpointRecord;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbDiscoveryTest {
    private final MockedClock clock = MockedClock.create(ZoneId.of("UTC"));
    private final MockedScheduler scheduler = new MockedScheduler(clock);
    private final MockedTransport transport = new MockedTransport(scheduler, "/testdb");

    private class TestHandler implements YdbDiscovery.Handler {
        private volatile boolean force = false;

        @Override
        public Instant instant() {
            return clock.instant();
        }

        @Override
        public GrpcTransport createDiscoveryTransport() {
            return transport;
        }

        @Override
        public boolean forceDiscovery() {
            return force;
        }

        @Override
        public void handleEndpoints(List<EndpointRecord> endpoints, String selfLocation) {
            // nothing
        }
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
            discovery.waitReady();
            return Boolean.TRUE;
        });
    }


    @Test
    public void baseTest() {
        scheduler.check().hasNoTasks();

        YdbDiscovery discovery = new YdbDiscovery(new TestHandler(), scheduler, "/base", Duration.ofMillis(100));
        discovery.start();

        scheduler.check().taskCount(1);
        transport.checkDiscoveryCallCount(0);

        scheduler.runNextTask();
        transport.checkDiscoveryCallCount(1);
        transport.completeNextDiscovery("self", new EndpointRecord("localhost", 12340));

        discovery.waitReady();
        discovery.stop();

        // stop is imdepotent operation
        discovery.stop();
    }

    @Test
    public void baseDiscoveryForcing() {
        Duration tick = Duration.ofSeconds(5); // == YdbDiscovery.DISCOVERY_PERIOD_MIN_SECONDS
        Duration normal = Duration.ofSeconds(60); // == YdbDiscovery.DISCOVERY_PERIOD_NORMAL_SECONDS

        TestHandler handler = new TestHandler();
        handler.force = false;

        scheduler.check().hasNoTasks();

        YdbDiscovery discovery = new YdbDiscovery(handler, scheduler, "/forcing", Duration.ofMillis(100));

        discovery.start();
        // first discovery
        scheduler.check().taskCount(1);
        transport.checkDiscoveryCallCount(0);
        scheduler.runNextTask();
        transport.checkDiscoveryCallCount(1);
        transport.completeNextDiscovery("self", new EndpointRecord("localhost", 12340));

        // few next ticks will be dump
        Instant started = clock.instant();
        Instant lastUpdate = started;

        scheduler.check().taskCount(1);
        scheduler.runNextTask();
        while (!clock.instant().isAfter(started.plus(normal))) {
            Assert.assertFalse("" + clock.instant() + " must be after " + lastUpdate.plus(tick),
                    clock.instant().isBefore(lastUpdate.plus(tick)));
            lastUpdate = clock.instant();

            transport.checkDiscoveryCallCount(0);
            scheduler.check().taskCount(1);
            scheduler.runNextTask();
        }

        // second discovery
        scheduler.check().taskCount(0);
        transport.checkDiscoveryCallCount(1);
        transport.completeNextDiscovery("self", new EndpointRecord("localhost", 12340));

        // force discovery
        handler.force = true;

        scheduler.check().taskCount(1);
        scheduler.runNextTask();
        scheduler.check().taskCount(0);
        transport.checkDiscoveryCallCount(1);

        discovery.stop();
    }

    @Test
    public void waitWaitingTest() {
        scheduler.check().hasNoTasks();

        final YdbDiscovery discovery = new YdbDiscovery(new TestHandler(), scheduler, "/wait", Duration.ofMillis(500));
        discovery.start();

        scheduler.check().taskCount(1);

        CompletableFuture<Boolean> req1 = createWaitingFuture(discovery);
        CompletableFuture<Boolean> req2 = createWaitingFuture(discovery);
        Assert.assertFalse(req1.isDone());
        Assert.assertFalse(req2.isDone());

        scheduler.runNextTask();

        transport.checkDiscoveryCallCount(1);
        transport.completeNextDiscovery("self", new EndpointRecord("localhost", 12340));

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
        TestHandler handler = new TestHandler();
        handler.force = true;

        scheduler.check().hasNoTasks();

        YdbDiscovery discovery = new YdbDiscovery(handler, scheduler, "/failed", Duration.ofMillis(100));
        discovery.start();

        // test not ready error
        CompletableFuture<Boolean> req1 = createWaitingFuture(discovery);
        scheduler.check().taskCount(1);
        Assert.assertFalse(req1.isDone());
        Assert.assertNull(checkFutureException(req1, "Discovery is not ready", null));

        // test discovery status
        CompletableFuture<Boolean> req2 = createWaitingFuture(discovery);
        Assert.assertFalse(req2.isDone());

        scheduler.check().taskCount(1);
        scheduler.runNextTask();
        transport.checkDiscoveryCallCount(1);
        transport.completeNextDiscovery(Status.of(StatusCode.UNAUTHORIZED));

        UnexpectedResultException ex2 = checkFutureException(req2, "Discovery failed", UnexpectedResultException.class);
        Assert.assertEquals(StatusCode.UNAUTHORIZED, ex2.getStatus().getCode());

        // test discovery other status
        CompletableFuture<Boolean> req3 = createWaitingFuture(discovery);
        Assert.assertFalse(req3.isDone());

        scheduler.check().taskCount(1);
        scheduler.runNextTask();
        transport.checkDiscoveryCallCount(1);
        transport.completeNextDiscovery(Status.of(StatusCode.TRANSPORT_UNAVAILABLE));

        UnexpectedResultException ex3 = checkFutureException(req3, "Discovery failed", UnexpectedResultException.class);
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, ex3.getStatus().getCode());

        // test empty discovery
        CompletableFuture<Boolean> req4 = createWaitingFuture(discovery);
        Assert.assertFalse(req4.isDone());

        scheduler.check().taskCount(1);
        scheduler.runNextTask();
        transport.checkDiscoveryCallCount(1);
        transport.completeNextDiscovery("self"); // empty discovery

        UnexpectedResultException ex4 = checkFutureException(req4, "Discovery failed", UnexpectedResultException.class);
        Assert.assertEquals(StatusCode.CLIENT_DISCOVERY_FAILED, ex4.getStatus().getCode());

        // test discovery throwable
        CompletableFuture<Boolean> req5 = createWaitingFuture(discovery);
        Assert.assertFalse(req5.isDone());

        scheduler.check().taskCount(1);
        scheduler.runNextTask();
        transport.checkDiscoveryCallCount(1);
        transport.completeNextDiscovery(new IOException("cannot open socket"));

        IOException ex5 = checkFutureException(req5, "Discovery failed", IOException.class);
        Assert.assertEquals("cannot open socket", ex5.getMessage());

        discovery.stop();
    }
}
