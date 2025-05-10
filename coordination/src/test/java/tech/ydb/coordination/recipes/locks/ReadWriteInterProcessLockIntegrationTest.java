package tech.ydb.coordination.recipes.locks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.test.junit4.GrpcTransportRule;

public class ReadWriteInterProcessLockIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ReadWriteInterProcessLockIntegrationTest.class);

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

    /**
     * Asserts that code does not throw any exceptions for basic write lock operations
     */
    @Test
    public void simpleWriteLockTest() throws Exception {
        ReadWriteInterProcessLock rwLock = getReadWriteLock();

        rwLock.writeLock().acquire();
        Assert.assertTrue(rwLock.writeLock().isAcquiredInThisProcess());
        Thread.sleep(100);
        rwLock.writeLock().release();
    }

    /**
     * Asserts that code does not throw any exceptions for basic read lock operations
     */
    @Test
    public void simpleReadLockTest() throws Exception {
        ReadWriteInterProcessLock rwLock = getReadWriteLock();

        rwLock.readLock().acquire();
        Assert.assertTrue(rwLock.readLock().isAcquiredInThisProcess());
        Thread.sleep(100);
        rwLock.readLock().release();
    }

    /**
     * Tests that write lock is exclusive (only one can acquire it)
     */
    @Test
    public void writeLockExclusivityTest() throws Exception {
        String coordinationNodePath = "writeLockExclusivityTest";
        String lockName = "writeLockExclusivityTest";
        ReadWriteInterProcessLock lock1 = getReadWriteLock(coordinationNodePath, lockName);
        ReadWriteInterProcessLock lock2 = getReadWriteLock(coordinationNodePath, lockName);

        lock1.writeLock().acquire();
        Assert.assertTrue(lock1.writeLock().isAcquiredInThisProcess());

        Assert.assertFalse(lock2.writeLock().acquire(Duration.ofMillis(100)));
        Assert.assertFalse(lock2.writeLock().isAcquiredInThisProcess());
    }

    /**
     * Tests that multiple read locks can be acquired simultaneously
     */
    @Test
    public void readLockSharedAccessTest() throws Exception {
        String coordinationNodePath = "readLockSharedAccessTest";
        String lockName = "readLockSharedAccessTest";
        ReadWriteInterProcessLock lock1 = getReadWriteLock(coordinationNodePath, lockName);
        ReadWriteInterProcessLock lock2 = getReadWriteLock(coordinationNodePath, lockName);

        lock1.readLock().acquire();
        Assert.assertTrue(lock1.readLock().isAcquiredInThisProcess());

        Assert.assertTrue(lock2.readLock().acquire(Duration.ofMillis(100)));
        Assert.assertTrue(lock2.readLock().isAcquiredInThisProcess());
    }


    /**
     * Tests that write lock cannot be acquired while read lock is held
     */
    @Test
    public void writeLockBlockedByReadLockTest() throws Exception {
        String coordinationNodePath = "writeLockBlockedByReadLockTest";
        String lockName = "writeLockBlockedByReadLockTest";
        ReadWriteInterProcessLock lock1 = getReadWriteLock(coordinationNodePath, lockName);
        ReadWriteInterProcessLock lock2 = getReadWriteLock(coordinationNodePath, lockName);

        lock1.readLock().acquire();
        Assert.assertTrue(lock1.readLock().isAcquiredInThisProcess());

        Assert.assertFalse(lock2.writeLock().acquire(Duration.ofMillis(100)));
        Assert.assertFalse(lock2.writeLock().isAcquiredInThisProcess());
    }

    /**
     * Tests that read lock cannot be acquired while write lock is held
     */
    @Test
    public void readLockBlockedByWriteLockTest() throws Exception {
        String coordinationNodePath = "readLockBlockedByWriteLockTest";
        String lockName = "readLockBlockedByWriteLockTest";
        ReadWriteInterProcessLock lock1 = getReadWriteLock(coordinationNodePath, lockName);
        ReadWriteInterProcessLock lock2 = getReadWriteLock(coordinationNodePath, lockName);

        lock1.writeLock().acquire();
        Assert.assertTrue(lock1.writeLock().isAcquiredInThisProcess());

        Assert.assertFalse(lock2.readLock().acquire(Duration.ofMillis(100)));
        Assert.assertFalse(lock2.readLock().isAcquiredInThisProcess());
    }

    /**
     * Concurrent test for write locks (should be exclusive)
     */
    @Test
    public void concurrentWriteLockTest() {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        int cycles = 10;
        int locksN = 10;

        String nodePath = UUID.randomUUID().toString();
        String lockName = UUID.randomUUID().toString();
        List<ReadWriteInterProcessLock> locks = new ArrayList<>(locksN);
        for (int i = 0; i < locksN; i++) {
            locks.add(getReadWriteLock(nodePath, lockName));
        }

        AtomicInteger counter = new AtomicInteger(0);

        // when
        List<Callable<Void>> tasks = locks.stream().map(lock ->
                (Callable<Void>) () -> {
                    for (int i = 0; i < cycles; i++) {
                        lock.writeLock().acquire();
                        int start = counter.get();
                        logger.debug("Write lock acquired, cycle = {}, count = {}", i, start);
                        Thread.sleep(100);
                        counter.set(start + 1); // not an atomic increment
                        logger.debug("Write lock released, cycle = {}", i);
                        lock.writeLock().release();
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

    /**
     * Mixed read-write lock test with more complex scenarios
     */
    @Test
    public void mixedReadWriteLockTest() throws Exception {
        String coordinationNodePath = "mixedReadWriteLockTest";
        String lockName = "mixedReadWriteLockTest";
        ReadWriteInterProcessLock lock1 = getReadWriteLock(coordinationNodePath, lockName);
        ReadWriteInterProcessLock lock2 = getReadWriteLock(coordinationNodePath, lockName);
        ReadWriteInterProcessLock lock3 = getReadWriteLock(coordinationNodePath, lockName);

        // 1. Test read lock sharing
        lock1.readLock().acquire();
        Assert.assertTrue(lock1.readLock().isAcquiredInThisProcess());

        // Second read lock should be allowed
        Assert.assertTrue(lock2.readLock().acquire(Duration.ofMillis(100)));
        Assert.assertTrue(lock2.readLock().isAcquiredInThisProcess());

        // Write lock should be blocked while read locks are held
        Assert.assertFalse(lock3.writeLock().acquire(Duration.ofMillis(100)));
        Assert.assertFalse(lock3.writeLock().isAcquiredInThisProcess());

        // 2. Release read locks and test write lock exclusivity
        lock1.readLock().release();
        lock2.readLock().release();

        // Now write lock should be acquirable
        Assert.assertTrue(lock3.writeLock().acquire(Duration.ofMillis(100)));
        Assert.assertTrue(lock3.writeLock().isAcquiredInThisProcess());

        // Read locks should be blocked while write lock is held
        Assert.assertFalse(lock1.readLock().acquire(Duration.ofMillis(100)));
        Assert.assertFalse(lock1.readLock().isAcquiredInThisProcess());
        Assert.assertFalse(lock2.readLock().acquire(Duration.ofMillis(100)));
        Assert.assertFalse(lock2.readLock().isAcquiredInThisProcess());

        // 3. Release write lock and test read lock acquisition again
        lock3.writeLock().release();

        // Read locks should be acquirable again
        Assert.assertTrue(lock1.readLock().acquire(Duration.ofMillis(100)));
        Assert.assertTrue(lock1.readLock().isAcquiredInThisProcess());
        Assert.assertTrue(lock2.readLock().acquire(Duration.ofMillis(100)));
        Assert.assertTrue(lock2.readLock().isAcquiredInThisProcess());

        // 4. Test write lock waiting for read locks to be released
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> writeLockFuture = executor.submit(() -> {
            // This should block until read locks are released
            return lock3.writeLock().acquire(Duration.ofSeconds(2));
        });

        // Wait a bit to ensure write lock is waiting
        Thread.sleep(100);
        Assert.assertFalse(writeLockFuture.isDone());

        // Release read locks
        lock1.readLock().release();
        lock2.readLock().release();

        // Now write lock should be acquired
        Assert.assertTrue(writeLockFuture.get(1, TimeUnit.SECONDS));
        Assert.assertTrue(lock3.writeLock().isAcquiredInThisProcess());

        // Cleanup
        executor.shutdown();
    }

    /**
     * Test re-acquisition after release
     */
    @Test
    public void reacquisitionTest() throws Exception {
        ReadWriteInterProcessLock rwLock = getReadWriteLock();

        // 1. Test write lock re-acquisition
        rwLock.writeLock().acquire();
        Assert.assertTrue(rwLock.writeLock().isAcquiredInThisProcess());
        rwLock.writeLock().release();
        Assert.assertFalse(rwLock.writeLock().isAcquiredInThisProcess());

        // Should be able to acquire again
        Assert.assertTrue(rwLock.writeLock().acquire(Duration.ofMillis(100)));
        Assert.assertTrue(rwLock.writeLock().isAcquiredInThisProcess());
        rwLock.writeLock().release();

        // 2. Test read lock re-acquisition
        rwLock.readLock().acquire();
        Assert.assertTrue(rwLock.readLock().isAcquiredInThisProcess());
        rwLock.readLock().release();
        Assert.assertFalse(rwLock.readLock().isAcquiredInThisProcess());

        // Should be able to acquire again
        Assert.assertTrue(rwLock.readLock().acquire(Duration.ofMillis(100)));
        Assert.assertTrue(rwLock.readLock().isAcquiredInThisProcess());
        rwLock.readLock().release();
    }

    private ReadWriteInterProcessLock getReadWriteLock() {
        return getReadWriteLock(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    private ReadWriteInterProcessLock getReadWriteLock(String nodePath, String lockName) {
        client.createNode(nodePath).join().expectSuccess("cannot create coordination path");
        return new ReadWriteInterProcessLock(
                client,
                nodePath,
                lockName,
                ReadWriteInterProcessLockSettings.newBuilder()
                        .withWaitConnection(true)
                        .build()
        );
    }

}
