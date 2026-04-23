package tech.ydb.coordination.recipes.locks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.recipes.locks.exception.LockAlreadyAcquiredException;
import tech.ydb.test.junit4.GrpcTransportRule;

public class InterProcessMutexIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(InternalLockMockedTest.class);

    @ClassRule
    public static final GrpcTransportRule ydbRule = new GrpcTransportRule();

    private static CoordinationClient client;

    @BeforeClass
    public static void init() {
        client = CoordinationClient.newClient(ydbRule);
    }

    @AfterClass
    public static void clean() {
        ydbRule.close();
    }

    private InterProcessMutex getInterProcessMutex() {
        return getInterProcessMutex(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    private InterProcessMutex getInterProcessMutex(String testName) {
        return getInterProcessMutex(testName, testName);
    }

    private InterProcessMutex getInterProcessMutex(String nodePath, String lockName) {
        client.createNode(nodePath).join().expectSuccess("cannot create coordination path");
        InterProcessMutex lock = new InterProcessMutex(
                client,
                nodePath,
                lockName,
                InterProcessMutexSettings.newBuilder()
                        .withWaitConnection(true)
                        .build()
        );
        return lock;
    }

    /**
     * Asserts that code does not throw any exceptions
     */
    @Test(timeout = 10000)
    public void simpleLockTest() throws Exception {
        InterProcessMutex lock = getInterProcessMutex();

        lock.acquire();
        Assert.assertTrue(lock.isAcquiredInThisProcess());
        Thread.sleep(100);
        lock.release();
    }

    /**
     * Asserts that code does not throw any exceptions
     */
    @Test(timeout = 10000)
    public void sessionListenerTest() throws Exception {
        InterProcessMutex lock = getInterProcessMutex();

        Consumer<CoordinationSession.State> syncListener = state -> logger.info("Recieved sync state change: " + state);
        Consumer<CoordinationSession.State> asyncListener = state -> logger.info("Recieved async state change: " + state);

        lock.getSessionListenable().addListener(syncListener);
        // try add listener twice
        lock.getSessionListenable().addListener(syncListener);

        lock.getSessionListenable().addListener(asyncListener, Executors.newSingleThreadExecutor());
        // try add listener twice
        lock.getSessionListenable().addListener(asyncListener, Executors.newSingleThreadExecutor());

        lock.acquire();
        Assert.assertTrue(lock.isAcquiredInThisProcess());
        Thread.sleep(100);
        lock.release();
        lock.close();

        lock.getSessionListenable().removeListener(syncListener);
        lock.getSessionListenable().removeListener(asyncListener);
    }

    /**
     * Asserts that code does not throw any exceptions
     */
    @Test(timeout = 10000)
    public void tryLockTest() throws Exception {
        String testName = "tryLockTest";
        InterProcessMutex lock1 = getInterProcessMutex(testName);
        InterProcessMutex lock2 = getInterProcessMutex(testName);

        lock1.acquire();
        Assert.assertTrue(lock1.isAcquiredInThisProcess());

        Assert.assertFalse(lock2.acquire(Duration.ofMillis(100)));
        Assert.assertFalse(lock2.isAcquiredInThisProcess());
    }

    /**
     * Asserts that there is no data race around counter that is protected by distributed lock
     * When locksN sessions tries to acquire lock at the same time
     */
    @Test(timeout = 10000)
    public void concurrentLockTest() {
        String testName = "concurrentLockTest";
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        int cycles = 5;
        int locksN = 5;

        List<InterProcessMutex> locks = new ArrayList<>(locksN);
        for (int i = 0; i < locksN; i++) {
            locks.add(getInterProcessMutex(testName));
        }

        AtomicInteger counter = new AtomicInteger(0);

        // when
        List<Callable<Void>> tasks = locks.stream().map(lock ->
                (Callable<Void>) () -> {
                    for (int i = 0; i < cycles; i++) {
                        lock.acquire();
                        int start = counter.get();
                        logger.debug("Lock acquired, cycle = {}, count = {}", i, start);
                        Thread.sleep(100);
                        counter.set(start + 1);
                        logger.debug("Lock released, cycle = {}", i);
                        lock.release();
                    }
                    return null;
                }
        ).collect(Collectors.toList());

        try {
            List<Future<Void>> futures = executor.invokeAll(tasks);
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception ignored) {
        }

        // then
        Assert.assertEquals(cycles * locksN, counter.get());

        executor.shutdown();
    }

    @Test(timeout = 10000)
    public void reentrantLockTest() throws Exception {
        InterProcessMutex lock = getInterProcessMutex();

        // first acquire
        lock.acquire();
        Assert.assertTrue(lock.isAcquiredInThisProcess());

        // try to acquire the same lock
        Assert.assertThrows(LockAlreadyAcquiredException.class, lock::acquire);
        Assert.assertTrue(lock.isAcquiredInThisProcess());
        Assert.assertTrue(lock.release());
    }

    @Test(timeout = 10000)
    public void longWaitTimeoutTest() throws Exception {
        String testName = "reentrantLockTest";
        InterProcessMutex lock1 = getInterProcessMutex(testName);
        InterProcessMutex lock2 = getInterProcessMutex(testName);

        lock1.acquire();
        Assert.assertFalse(lock2.acquire(Duration.ofMillis(10)));
    }

    @Test(timeout = 10000)
    public void releaseNotAcquiredLockTest() throws Exception {
        InterProcessMutex lock = getInterProcessMutex();

        Assert.assertFalse(lock.release());
    }

    @Test(timeout = 10000)
    public void concurrentReleaseTest() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        InterProcessMutex lock = getInterProcessMutex();

        lock.acquire();

        Future<Boolean> future1 = executor.submit(lock::release);
        Future<Boolean> future2 = executor.submit(lock::release);

        // Only one concurrent release must be successful
        Assert.assertTrue(future1.get() ^ future2.get());

        executor.shutdown();
    }

}



























