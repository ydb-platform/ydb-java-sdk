package tech.ydb.topic.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
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
        AtomicInteger conflicts = new AtomicInteger(0);

        SerialRunnable sr = new SerialRunnable(() -> {
            conflicts.addAndGet(value.value);
            value.value++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // nothing
            }
            value.value--;
        });

        ExecutorService pool = Executors.newCachedThreadPool();
        for (int idx = 0; idx < 8; idx++) {
            pool.execute(sr);
        }

        awaitPool(pool);
        Assert.assertEquals(0, conflicts.get());
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
    public void wrongExecuterTest() throws InterruptedException {
        AtomicInteger value = new AtomicInteger();
        ExecutorService pool = Executors.newCachedThreadPool();

        SerialExecutor se = new SerialExecutor(pool);

        Assert.assertEquals(0, value.get());

        se.execute(value::incrementAndGet);
        se.execute(value::incrementAndGet);

        awaitPool(pool);
        Assert.assertEquals(2, value.get());

        Exception ex = Assert.assertThrows(RejectedExecutionException.class, () -> se.execute(value::incrementAndGet));
        Assert.assertTrue(ex.getMessage().contains("rejected from java.util.concurrent.ThreadPoolExecutor"));
    }

    @Test
    public void wrongTaskTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        Queue<Throwable> problems = new ConcurrentLinkedQueue<>();
        ExecutorService pool = Executors.newCachedThreadPool((task) -> {
            Thread t = new Thread(task);
            t.setUncaughtExceptionHandler((th, ex) -> {
                problems.add(ex);
                latch.countDown();
            });
            return t;
        });

        SerialExecutor se = new SerialExecutor(pool);

        se.execute(latch::countDown);
        se.execute(latch::countDown);

        // SerialExecute is exception-safe
        se.execute(() -> {
            throw new RuntimeException("error");
        });

        se.execute(latch::countDown);
        se.execute(latch::countDown);

        Assert.assertTrue("All tasks must be executed", latch.await(10, TimeUnit.SECONDS));

        awaitPool(pool);
        Assert.assertEquals(1, problems.size());
        Throwable p1 = problems.poll();
        Assert.assertTrue(p1 instanceof RuntimeException);
        Assert.assertEquals("error", p1.getMessage());
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
                    });
                }
                latch.countDown();
            });
        }

        Assert.assertTrue("Producer threads did not finish in time", latch.await(10, TimeUnit.SECONDS));
        awaitPool(pool);
        Assert.assertEquals(800, count.value);
    }
}
