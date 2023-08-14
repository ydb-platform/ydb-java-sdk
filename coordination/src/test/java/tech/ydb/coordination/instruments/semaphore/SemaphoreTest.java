package tech.ydb.coordination.instruments.semaphore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.instruments.semaphore.exceptions.SemaphoreCreationException;
import tech.ydb.test.junit4.GrpcTransportRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemaphoreTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();

    private final CoordinationClient client = CoordinationClient.newClient(YDB_TRANSPORT);
    private volatile boolean failed = false;

    @Test
    public void testSimultaneousWork() throws InterruptedException {
        final String nodePath = client.getDatabase() + "/new_node";
        final int semaphoreCount = 120;
        final int acquireCount = 30;
        final int maxNumberOfWorker = semaphoreCount / acquireCount;
        final int workers = 10;

        final CountDownLatch latch = new CountDownLatch(workers);
        Resource.setMaxNumberOfWorkers(maxNumberOfWorker);

        for (int i = 0; i < workers; i++) {
            Semaphore semaphore =
                    SemaphoreImpl.newSemaphore(client, nodePath, "testSimultaneousWork", semaphoreCount).join();
            semaphore.acquireAsync(SemaphoreSettings.newBuilder()
                    .withCount(acquireCount)
                    .withTimeout(30_000)
                    .build()
            ).thenAcceptAsync(acquired -> {
                        if (acquired) {
                            try {
                                new Resource().work();
                            } catch (Exception e) {
                                failed = true;
                            }
                            semaphore.release();
                            latch.countDown();
                        } else {
                            latch.countDown();
                        }
                    }
            );
        }

        Assert.assertTrue("Test succeed 120 seconds.", latch.await(120, TimeUnit.SECONDS));
        if (failed) {
            Assert.fail("More than expected threads got access to resource simultaneously.");
        }
    }

    @Test
    public void testAcquireMultipleTimes() {
        final String nodePath = client.getDatabase() + "/new_node";
        final int semaphoreCount = 60;
        final int acquireCount = 30;

        final Semaphore semaphore1 =
                SemaphoreImpl.newSemaphore(client, nodePath, "testAcquireMultipleTimes", semaphoreCount).join();
        final Semaphore semaphore2 =
                SemaphoreImpl.newSemaphore(client, nodePath, "testAcquireMultipleTimes", semaphoreCount).join();
        final Semaphore semaphoreLame =
                SemaphoreImpl.newSemaphore(client, nodePath, "testAcquireMultipleTimes", semaphoreCount).join();

        for (int i = 0; i < 2; i++) {
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            for (Semaphore semaphore : Arrays.asList(semaphore1, semaphore2)) {
                futures.add(semaphore.acquireAsync(SemaphoreSettings.newBuilder()
                        .withCount(acquireCount)
                        .withTimeout(30_000)
                        .build()
                ));
            }

            futures.stream().map(CompletableFuture::join).forEach(Assert::assertTrue);

            Assert.assertFalse(semaphoreLame.acquire(SemaphoreSettings.newBuilder()
                    .withCount(1)
                    .withTimeout(1_000)
                    .build())
            );

            for (Semaphore semaphore : Arrays.asList(semaphore1, semaphore2)) {
                Assert.assertTrue(semaphore.release());
            }
        }
    }

    @Test
    public void testFailCreateSemaphoreWithDifferentSettings() {
        final String nodePath = client.getDatabase() + "/new_node";

        SemaphoreImpl.newSemaphore(client, nodePath, "testCreateSemaphoreWithDifferentSettings", 60).join();
        try {
            SemaphoreImpl.newSemaphore(client, nodePath, "testCreateSemaphoreWithDifferentSettings", 120).join();
            Assert.fail();
        } catch (Exception proxyException) {
            Assert.assertTrue(proxyException.getCause() instanceof SemaphoreCreationException);
            SemaphoreCreationException e = (SemaphoreCreationException) proxyException.getCause();
            Assert.assertEquals(e.getName(), "testCreateSemaphoreWithDifferentSettings");
            Assert.assertEquals(e.getLimit(), 60);
            Assert.assertEquals(e.getCount(), 0);
            Assert.assertEquals(e.getMessage(),
                    "The semaphore has already been created and its settings are different from yours.");

        }
    }
}

class Resource {
    private static final AtomicInteger WORKING_NOW = new AtomicInteger(0);
    private static final Logger logger = LoggerFactory.getLogger(Resource.class);
    private static int workingMax;

    public static void setMaxNumberOfWorkers(int n) {
        workingMax = n;
    }

    public int work() {
        final int workingNow = WORKING_NOW.incrementAndGet();
        logger.trace("Number of workers now: " + workingNow + ", max number of workers: " + workingMax);
        if (workingNow > workingMax) {
            throw new IllegalStateException(
                    "Number of workers more than expected (expected: " + workingMax + ", work: " + workingNow + ")"
            );
        }
        // Do some 'helpful' work
        int j = 0;
        for (AtomicInteger i = new AtomicInteger(0); i.get() < 100_000; i.incrementAndGet()) {
            j += i.get();
        }
        WORKING_NOW.decrementAndGet();
        return j;
    }
}
