package tech.ydb.coordination;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import tech.ydb.coordination.scenario.semaphore.AsyncSemaphore;
import tech.ydb.coordination.scenario.semaphore.Semaphore;
import tech.ydb.coordination.scenario.semaphore.settings.SemaphoreSettings;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.test.junit4.GrpcTransportRule;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemaphoreTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();
    private static final Logger logger = LoggerFactory.getLogger(SemaphoreTest.class);
    private final CoordinationClient client = CoordinationClient.newClient(YDB_TRANSPORT);
    private volatile boolean failed = false;

    @Test
    public void testSimultaneousWork()
            throws InterruptedException {
        final String nodePath = client.getDatabase() + "/new_node";
        final int semaphoreCount = 120;
        final int acquireCount = 30;
        final int maxNumberOfWorker = semaphoreCount / acquireCount;
        final int workers = 10;

        final CountDownLatch latch = new CountDownLatch(workers);
        Resource.setMaxNumberOfWorkers(maxNumberOfWorker);

        AsyncSemaphore.newAsyncSemaphore(client, nodePath, "testSimultaneousWork", semaphoreCount).join();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            final CompletableFuture<Void> future =
                    AsyncSemaphore.newAsyncSemaphore(client, nodePath, "testSimultaneousWork", semaphoreCount)
                            .thenCompose(semaphore -> {
                                logger.info("created");
                                return CompletableFuture.completedFuture(semaphore);
                            })
                            .thenCompose(semaphore -> semaphore.acquireAsync(SemaphoreSettings.newBuilder()
                                                    .withCount(acquireCount)
                                                    .withTimeout(60_000)
                                                    .build()
                                            ).thenCompose(acquired -> {
                                                if (acquired) {
                                                    try {
                                                        new Resource().work();
                                                    } catch (Exception e) {
                                                        failed = true;
                                                    }
                                                }
                                                return semaphore.releaseAsync();
                                            })
                                            .thenAccept(released -> latch.countDown())
                                            .exceptionally(e -> {
                                                Assume.assumeNoException(e);
                                                return null;
                                            })
                            );
            futures.add(future);
        }
        futures.forEach(CompletableFuture::join);
        Assert.assertTrue("Test succeed 10 minutes.", latch.await(10, TimeUnit.MINUTES));
        if (failed) {
            Assert.fail("More than expected threads got access to resource simultaneously.");
        }
    }

    @Test
    public void testAcquireMultipleTimes() {
        final String nodePath = client.getDatabase() + "/new_node";
        final int semaphoreCount = 60;
        final int acquireCount = 30;

        final Semaphore worker1 =
                Semaphore.newSemaphore(client, nodePath, "testAcquireMultipleTimes", semaphoreCount).join();
        final Semaphore worker2 =
                Semaphore.newSemaphore(client, nodePath, "testAcquireMultipleTimes", semaphoreCount).join();
        final Semaphore workerWhoNeverWork =
                Semaphore.newSemaphore(client, nodePath, "testAcquireMultipleTimes", semaphoreCount).join();

        for (int i = 0; i < 3; i++) {
            final List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            for (Semaphore semaphore : Arrays.asList(worker1, worker2)) {
                futures.add(semaphore.acquireAsync(SemaphoreSettings.newBuilder()
                        .withCount(acquireCount)
                        .withTimeout(60_000)
                        .build()
                ));
            }
            futures.stream().map(CompletableFuture::join).forEach(Assert::assertTrue);
        }

        Assert.assertFalse(workerWhoNeverWork.acquire(SemaphoreSettings.newBuilder()
                .withCount(1)
                .withTimeout(1_000)
                .build())
        );

        for (Semaphore semaphore : Arrays.asList(worker1, worker2)) {
            Assert.assertTrue(semaphore.release());
        }
    }

    @Test
    public void testFailCreateSemaphoreWithDifferentSettings() {
        final String nodePath = client.getDatabase() + "/new_node";
        final String semaphoreName = "testFailCreateSemaphoreWithDifferentSettings";

        try {
            Semaphore.newSemaphore(client, nodePath, semaphoreName, 60).join();
        } catch (Exception e) {
            Assume.assumeNoException("Error while trying to create/connect semaphore.", e);
        }

        try {
            Semaphore.newSemaphore(client, nodePath, semaphoreName, 120).join();
            Assert.fail();
        } catch (Exception proxyException) {
            Assert.assertTrue(proxyException.getCause() instanceof UnexpectedResultException);
            Assume.assumeTrue(((UnexpectedResultException) proxyException.getCause()).getStatus().isSuccess());
        }
    }

    @Test
    public void testSeveralAcquires() {
        final String nodePath = client.getDatabase() + "/new_node";
        final String semaphoreName = "testChangeCountInAcquire";
        final Semaphore semaphore = Semaphore.newSemaphore(client, nodePath, semaphoreName, 60).join();

        Assert.assertTrue(semaphore.acquire(SemaphoreSettings.newBuilder()
                .withTimeout(60_000)
                .withCount(30)
                .build())
        );

        CompletableFuture<Boolean> acquireInFuture = Semaphore.newSemaphore(client, nodePath, semaphoreName, 60).join()
                .acquireAsync(SemaphoreSettings.newBuilder()
                        .withCount(55)
                        .withTimeout(60_000)
                        .build());

        /* You can make several acquires on the one semaphore, but every new acquire have to be less than previous
        one */
        Assert.assertTrue(semaphore.acquire(SemaphoreSettings.newBuilder()
                .withTimeout(60_000)
                .withCount(2)
                .build())
        );

        Assert.assertTrue(acquireInFuture.join());

        /* You cannot acquire more than you have now */
        Assert.assertThrows(Exception.class, () -> semaphore.acquire(SemaphoreSettings.newBuilder()
                .withTimeout(60_000)
                .withCount(10)
                .build()));
    }

    @Test
    public void testZeroTimeout() {
        final String nodePath = client.getDatabase() + "/new_node";
        final String semaphoreName = "testZeroTimeout";
        final AsyncSemaphore semaphore1 = AsyncSemaphore.newAsyncSemaphore(client, nodePath, semaphoreName, 60).join();
        final AsyncSemaphore semaphore2 = AsyncSemaphore.newAsyncSemaphore(client, nodePath, semaphoreName, 60).join();
        Resource.setMaxNumberOfWorkers(1);

        final BiFunction<AsyncSemaphore, Long, CompletableFuture<Boolean>> acquireFunction = (semaphore, timeout) ->
                semaphore.acquireAsync(SemaphoreSettings.newBuilder()
                        .withTimeout(timeout)
                        .withCount(60)
                        .build());
        final Function<AsyncSemaphore, CompletableFuture<Boolean>> tryAcquire =
                semaphore -> acquireFunction.apply(semaphore, 0L);

        Assert.assertTrue(acquireFunction.apply(semaphore1, 60_000L).join());

        final CompletableFuture<Boolean> tryAcquireOnAlreadyAcquiredSemaphore = tryAcquire.apply(semaphore2);

        Stream.generate(() -> (Runnable) (() -> new Resource().work())).limit(10).forEach(Runnable::run);

        Assert.assertTrue(semaphore1.releaseAsync().join());

        /* If you pass the timeout parameter with 0 or just don't pass timeout, then you will try to acquire
        semaphore only once, and you won't be placed in semaphore's queue. */
        Assert.assertFalse(tryAcquireOnAlreadyAcquiredSemaphore.join());

        Assert.assertTrue(tryAcquire.apply(semaphore2).join());
        Assert.assertTrue(semaphore2.releaseAsync().join());

        AsyncSemaphore.deleteSemaphoreAsync(client, nodePath, semaphoreName, false).join();
    }


    @Test
    public void testCancelSemaphoreCreation() {
        final CoordinationClient stubClient = new CoordinationClientStub();
        final String nodePath = stubClient.getDatabase() + "/new_node";
        final String semaphoreName = "testCancelSemaphoreCreation";

        CoordinationSessionStub.getStagesQueue().clear();
        CoordinationSessionStub.setNumberOfStartSession(new AtomicLong(0));
        CoordinationSessionStub.setNumberOfCreateSemaphore(new AtomicLong(0));

        List<CompletableFuture<AsyncSemaphore>> semaphoreFutures = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            semaphoreFutures.add(AsyncSemaphore.newAsyncSemaphore(stubClient, nodePath, semaphoreName, 60));
        }

        /* If you immediately cancel the creation of Asynchronous Semaphore, you may suppose that there were no extra
         sessions created */
        semaphoreFutures.forEach(createFuture -> {
            try {
                createFuture.get(300, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.info(e.toString());
                createFuture.cancel(true);
            }
        });
        CoordinationSessionStub.runNStagesInQueue(3);
        Assert.assertEquals(3, CoordinationSessionStub.getNumberOfStartSession().get());

        /* Creation was canceled on the previous stage, hence Semaphore won't be created */
        CoordinationSessionStub.runNStagesInQueue(3);
        Assert.assertEquals(0, CoordinationSessionStub.getNumberOfCreateSemaphore().get());
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
