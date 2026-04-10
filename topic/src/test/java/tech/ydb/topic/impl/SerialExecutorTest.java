package tech.ydb.topic.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Aleksandr Gorshenin
 */
public class SerialExecutorTest {
    private static void awaitPool(ExecutorService pool) throws InterruptedException {
        pool.shutdown();
        Assert.assertTrue("Pool did not terminate in time", pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    private static class IntHolder {
        private volatile int value;
    }

    @Test
    public void serialRunnableTest() throws InterruptedException {
        IntHolder value = new IntHolder();
        Semaphore semaphore = new Semaphore(0);
        AtomicInteger conficts = new AtomicInteger(0);

        SerialRunnable sr = new SerialRunnable(() -> {
            conficts.addAndGet(value.value);
            value.value = 1;
            semaphore.acquireUninterruptibly();
            value.value = 0;
        });

        ExecutorService pool = Executors.newCachedThreadPool();
        for (int idx = 0; idx < 8; idx++) {
            pool.execute(sr);
        }

        while (semaphore.hasQueuedThreads()) {
            semaphore.release();
        }

        awaitPool(pool);
        Assert.assertEquals(0, conficts.get());
    }

    @Test
    public void skipAllowedTest() throws InterruptedException {
        Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

        AtomicInteger value = new AtomicInteger();
        SerialExecutor se = new SerialExecutor(queue::add, true);

        Assert.assertEquals(0, value.get());

        se.execute(value::incrementAndGet);
        // all others will be skipped
        se.execute(value::incrementAndGet);
        se.execute(value::incrementAndGet);
        se.execute(value::incrementAndGet);

        for (Runnable r: queue) {
            r.run();
        }

        Assert.assertEquals(1, value.get());
    }

    @Test
    public void skipNotAllowedTest() throws InterruptedException {
        Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

        AtomicInteger value = new AtomicInteger();
        SerialExecutor se = new SerialExecutor(queue::add, false);

        Assert.assertEquals(0, value.get());

        se.execute(value::incrementAndGet);
        se.execute(value::incrementAndGet);
        se.execute(value::incrementAndGet);
        se.execute(value::incrementAndGet);

        for (Runnable r: queue) {
            r.run();
        }

        Assert.assertEquals(4, value.get());
    }

    @Test
    public void innerTasksTest() throws InterruptedException {
        AtomicInteger value = new AtomicInteger();
        SerialExecutor se = new SerialExecutor(Runnable::run);

        se.execute(() -> {
            value.incrementAndGet();
            // inner tasks will be executed one by one
            se.execute(() -> {
                value.incrementAndGet();
                se.execute(() -> {
                    value.incrementAndGet();
                    Assert.assertEquals(3, value.get());
                });
                Assert.assertEquals(2, value.get());
            });
            Assert.assertEquals(1, value.get());
        });
        Assert.assertEquals(3, value.get());
    }

    @Test
    public void concurrentStressTest() throws InterruptedException {
        IntHolder count = new IntHolder();
        CountDownLatch latch = new CountDownLatch(8);
        ExecutorService pool = Executors.newCachedThreadPool();

        SerialExecutor se = new SerialExecutor(pool);
        for (int parallel = 0; parallel < 8; parallel++) {
            pool.execute(() -> {
                for (int idx = 0; idx < 100; idx++) {
                    se.execute(() -> {
                        count.value++; // not atomic action
                        throw new RuntimeException("error"); // SerialExecute is exception-safe
                    });
                }
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        awaitPool(pool);
        Assert.assertEquals(800, count.value);
    }
}
