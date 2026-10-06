package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class LazyExecutorTest {

    @Test
    public void customExecutorTest() {
        AtomicInteger counter = new AtomicInteger();
        List<Runnable> queue = new ArrayList<>();
        LazyExecutor custom = new LazyExecutor("test", queue::add);

        custom.execute(counter::incrementAndGet);
        custom.execute(counter::incrementAndGet);
        custom.execute(counter::incrementAndGet);

        Assert.assertEquals(0, counter.get());
        Assert.assertEquals(3, queue.size());
        queue.forEach(Runnable::run);
        Assert.assertEquals(3, counter.get());

        custom.close();

        custom.execute(counter::incrementAndGet);
        custom.execute(counter::incrementAndGet);

        Assert.assertEquals(3, counter.get());
        Assert.assertEquals(3, queue.size());

        custom.close(); // no effect
    }

    @Test
    public void lazyExecutorTest() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        LazyExecutor lazy = new LazyExecutor("lazy", null);
        CountDownLatch latch = new CountDownLatch(600);

        ExecutorService producer = Executors.newFixedThreadPool(6);
        for (int i = 0; i < 6; i++) {
            producer.execute(() -> {
                for (int j = 0; j < 100; j++) {
                    lazy.execute(counter::incrementAndGet);
                    latch.countDown();
                }
            });
        }

        Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
        producer.shutdown();
        Assert.assertTrue(producer.awaitTermination(5, TimeUnit.SECONDS));

        lazy.close();
        Assert.assertEquals(600, counter.get());
        lazy.close();
        Assert.assertEquals(600, counter.get());
    }

    @Test
    public void decompressionPoolRunsTenThreads() throws InterruptedException {
        int poolSize = LazyExecutor.DECOMPRESSION_THREAD_COUNT;
        LazyExecutor decoder = new LazyExecutor("decoder", null, poolSize);
        CountDownLatch started = new CountDownLatch(poolSize);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch extraStarted = new CountDownLatch(1);

        try {
            for (int idx = 0; idx < poolSize; idx++) {
                decoder.execute(() -> {
                    started.countDown();
                    await(release);
                });
            }
            Assert.assertTrue(started.await(5, TimeUnit.SECONDS));

            decoder.execute(extraStarted::countDown);
            Assert.assertFalse(extraStarted.await(200, TimeUnit.MILLISECONDS));
        } finally {
            release.countDown();
            decoder.close();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
