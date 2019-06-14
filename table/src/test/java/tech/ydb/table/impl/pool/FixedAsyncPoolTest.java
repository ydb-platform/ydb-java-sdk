package tech.ydb.table.impl.pool;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.HashedWheelTimer;
import org.junit.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author Sergey Polovko
 */
public class FixedAsyncPoolTest {

    @Test
    public void acquireTimeout() {
        Duration timeout = Duration.ofSeconds(1);
        FixedAsyncPool<Resource> pool = new FixedAsyncPool<>(
            new ResourceHandler(),
            new HashedWheelTimer(),
            0, 2, 2, 10_000, 10_000);

        {
            CompletableFuture<Resource> f = pool.acquire(timeout);
            assertTrue(f.isDone());
            Resource r = f.getNow(null);
            assertNotNull(r);
            assertEquals(1, r.id);
        }
        {
            CompletableFuture<Resource> f = pool.acquire(timeout);
            assertTrue(f.isDone());
            Resource r = f.getNow(null);
            assertNotNull(r);
            assertEquals(2, r.id);
        }
        {
            // immediate fail
            CompletableFuture<Resource> f = pool.acquire(Duration.ZERO);
            assertTrue(f.isDone());
            assertTrue(f.isCompletedExceptionally());
            try {
                f.get();
                fail("must throw IllegalStateException");
            } catch (Exception e) {
                assertTrue(e.getCause() instanceof IllegalStateException);
                assertEquals("too many acquired objects", e.getCause().getMessage());
            }
        }
        {
            // fail on timeout
            CompletableFuture<Resource> f = pool.acquire(timeout);
            assertFalse(f.isDone());
            try {
                f.get();
                fail("must throw TimeoutException");
            } catch (Exception e) {
                Throwable cause = e.getCause();
                assertTrue(cause instanceof TimeoutException);
                assertEquals("cannot acquire object within " + timeout.toMillis() + "ms", cause.getMessage());
            }
        }
    }

    @Test
    public void acquireRelease() {
        FixedAsyncPool<Resource> pool = new FixedAsyncPool<>(
            new ResourceHandler(),
            new HashedWheelTimer(),
            0, 2, 2, 10_000, 10_000);

        Resource r1 = pool.acquire(Duration.ZERO)
            .join();
        assertEquals(1, r1.id);
        assertEquals(1, pool.getAcquiredCount());

        Resource r2 = pool.acquire(Duration.ZERO)
            .join();
        assertEquals(2, r2.id);
        assertEquals(2, pool.getAcquiredCount());

        {
            // release valid object
            pool.release(r1);
            assertEquals(1, pool.getAcquiredCount());

            // get same object on acquire
            Resource r = pool.acquire(Duration.ZERO)
                .join();
            assertEquals(r1.id, r.id);
            assertEquals(2, pool.getAcquiredCount());
        }
        {
            // release invalid object
            r2.markBroken();
            pool.release(r2);
            assertEquals(1, pool.getAcquiredCount());

            // get another object on acquire
            Resource r = pool.acquire(Duration.ZERO)
                .join();
            assertEquals(3, r.id);
            assertEquals(2, pool.getAcquiredCount());
        }
    }

    @Test
    public void delayedAcquire() {
        FixedAsyncPool<Resource> pool = new FixedAsyncPool<>(
            new ResourceHandler(),
            new HashedWheelTimer(),
            0, 2, 2, 10_000, 10_000);

        Resource r1 = pool.acquire(Duration.ZERO)
            .join();
        assertEquals(1, r1.id);
        assertEquals(1, pool.getAcquiredCount());

        Resource r2 = pool.acquire(Duration.ZERO)
            .join();
        assertEquals(2, r2.id);
        assertEquals(2, pool.getAcquiredCount());

        CompletableFuture<Resource> r3Future = pool.acquire(Duration.ofMinutes(10));
        pool.release(r1);

        Resource r3 = r3Future.join();
        assertEquals(1, r3.id);
        assertEquals(2, pool.getAcquiredCount());
    }

    @Test
    public void close() {
        FixedAsyncPool<Resource> pool = new FixedAsyncPool<>(
            new ResourceHandler(),
            new HashedWheelTimer(),
            0, 2, 2, 10_000, 10_000);

        Resource r = pool.acquire(Duration.ZERO).join();
        pool.acquire(Duration.ofMinutes(1));
        pool.acquire(Duration.ofMinutes(1));
        assertEquals(2, pool.getAcquiredCount());

        pool.close();
        assertEquals(0, pool.getAcquiredCount());

        // release after close must throw
        try {
            pool.release(r);
            fail("must throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("pool was closed", e.getMessage());
        }

        // acquire after close also must fail
        try {
            pool.acquire(Duration.ZERO)
                .join();
            fail("must throw IllegalStateException");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalStateException);
            assertEquals("pool was closed", cause.getMessage());
        }
    }

    @Test
    public void keepAlive() throws InterruptedException {
        // TODO: inject special clock for manual control time
        FixedAsyncPool<Resource> pool = new FixedAsyncPool<>(
            new ResourceHandler(),
            new HashedWheelTimer(),
            1, 10, 10, 500, 2_000);

        Resource r1 = pool.acquire(Duration.ZERO).join();
        assertEquals(1, r1.id);

        Resource r2 = pool.acquire(Duration.ZERO).join();
        assertEquals(2, r2.id);

        Resource r3 = pool.acquire(Duration.ZERO).join();
        assertEquals(3, r3.id);

        pool.release(r1);
        pool.release(r2);
        pool.release(r3);

        // wait some time to make all resources been keepalived
        Thread.sleep(1000);
        assertTrue(r1.keepAlived);
        assertTrue(r2.keepAlived);
        assertTrue(r3.keepAlived);

        Resource rUsed = pool.acquire(Duration.ZERO).join();
        assertSame(r3, rUsed);

        // wait more time to make one resource been destroyed
        Thread.sleep(2000);
        assertEquals(ResourceState.DESTROYED, r1.state);
        assertEquals(ResourceState.OK, r2.state); // kept idle in pool, because minSize = 1
        assertEquals(ResourceState.OK, r3.state); // keeps ok because used right now
    }

    @Test
    public void concurrentAcquireRelease() throws Exception {
        HashedWheelTimer timer = new HashedWheelTimer();
        FixedAsyncPool<Resource> pool = new FixedAsyncPool<>(
            new ResourceHandler(),
            timer,
            0, 1, Integer.MAX_VALUE,
            10_000, 10_000);

        final int numWorkers = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);

        try {
            AtomicInteger id = new AtomicInteger(1);
            Runnable worker = () -> {
                int myId = id.getAndIncrement();
                System.out.println("worker #" + myId + " started");
                try {
                    Duration timeout = Duration.ofSeconds(5);
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    for (int i = 0; i < 1000; i++) {
                        // sleep 1-5us to add contention
                        nanoSleep(rnd.nextInt(1000, 5000));

                        Resource r = pool.acquire(timeout).join();
                        try {
                            assertEquals(1, r.id);
                            // sleep 1-2us to add contention
                            nanoSleep(rnd.nextInt(1000, 2000));
                        } finally {
                            pool.release(r);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                System.out.println("worker #" + myId + " done");
            };

            Future[] futures = new Future[numWorkers];
            for (int i = 0; i < numWorkers; i++) {
                futures[i] = executor.submit(worker);
            }
            for (Future future : futures) {
                future.get(); // rethrow exception if any
            }

            assertEquals(0, pool.getAcquiredCount());
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);

            pool.close();
            timer.stop();
        }
    }

    private static void nanoSleep(int delay) {
        final long start = System.nanoTime();
        while (System.nanoTime() - start < delay) {
            /* busy loop */
        }
    }

    /**
     * RESOURCE HANDLER
     */
    private static class ResourceHandler implements PooledObjectHandler<Resource> {
        private final AtomicInteger idSeq = new AtomicInteger();

        @Override
        public CompletableFuture<Resource> create(long deadlineAfter) {
            return completedFuture(new Resource(idSeq.incrementAndGet()));
        }

        @Override
        public CompletableFuture<Void> destroy(Resource r) {
            r.markDestroyed();
            return completedFuture(null);
        }

        @Override
        public boolean isValid(Resource r) {
            return r.state == ResourceState.OK;
        }

        @Override
        public CompletableFuture<Void> keepAlive(Resource r) {
            r.markKeepAlived();
            return completedFuture(null);
        }
    }

    /**
     * RESOURCE STATE
     */
    private enum ResourceState {
        OK,
        BROKEN,
        DESTROYED,
    }

    /**
     * RESOURCE
     */
    private static final class Resource {
        private final int id;
        private volatile ResourceState state;
        private volatile boolean keepAlived = false;

        Resource(int id) {
            this.id = id;
            this.state = ResourceState.OK;
        }

        void markBroken() {
            state = ResourceState.BROKEN;
        }

        void markDestroyed() {
            state = ResourceState.DESTROYED;
        }

        void markKeepAlived() {
            keepAlived = true;
        }

        @Override
        public String toString() {
            return "Resource{id=" + id + ", state=" + state + ", keepAlived=" + keepAlived + '}';
        }
    }
}
