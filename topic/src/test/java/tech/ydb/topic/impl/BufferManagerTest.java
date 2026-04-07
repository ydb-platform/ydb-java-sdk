package tech.ydb.topic.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.impl.BufferManager;

/**
 * @author Aleksandr Gorshenin
 */
public class BufferManagerTest {

    private static WriterSettings settings(int maxBytes, int maxCount) {
        return WriterSettings.newBuilder()
                .setTopicPath("/test")
                .setMaxSendBufferMemorySize(maxBytes)
                .setMaxSendBufferMessagesCount(maxCount)
                .build();
    }

    private static BufferManager manager(int maxBytes, int maxCount) {
        return new BufferManager("test", settings(maxBytes, maxCount));
    }

    private static void assertOverflow(String msg, ThrowingRunnable runnable) {
        QueueOverflowException ex = Assert.assertThrows("Must be throwed QueueOverflowException",
                QueueOverflowException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    private static void assertTimeout(String msg, ThrowingRunnable runnable) {
        TimeoutException ex = Assert.assertThrows("Must be throwed TimeoutException", TimeoutException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    private static void assertInterrupted(ThrowingRunnable runnable) throws InterruptedException {
        // Now try to acquire more bytes in a separate thread — it will block
        AtomicBoolean interrupted = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            try {
                started.countDown();
                runnable.run();
            } catch (InterruptedException e) {
                interrupted.set(true);
            } catch (Throwable e) {
                // unexpected
                throw new AssertionError(e);
            }
        });
        t.start();
        Assert.assertTrue(started.await(1, TimeUnit.SECONDS));
        t.interrupt();
        t.join(1000);

        Assert.assertTrue(interrupted.get());
    }

    @Test
    public void testGetMaxSize() {
        BufferManager bm = manager(1024, 10);
        Assert.assertEquals(1024, bm.getMaxSize());
    }

    // --- acquire / release ---

    @Test
    public void testAcquireAndRelease() throws Exception {
        BufferManager bm = manager(70, 5);
        bm.acquire(30); // success
        bm.acquire(30); // success
        assertOverflow("[test] Rejecting a message of 40 bytes: not enough space in message queue. "
                + "Buffer currently has 2 messages with 10 / 70 bytes available", () -> bm.tryAcquire(40)); // fail
        bm.releaseMessage(30);

        bm.tryAcquire(40); // success

        bm.releaseMessage(30);
        bm.releaseMessage(40);

        // validate buffer - 70 bytes in 5 messages can be stored
        bm.tryAcquire(10, 100, TimeUnit.MILLISECONDS);
        bm.tryAcquire(20, 100, TimeUnit.MILLISECONDS);
        bm.tryAcquire(10, 100, TimeUnit.MILLISECONDS);
        bm.tryAcquire(20, 100, TimeUnit.MILLISECONDS);
        bm.tryAcquire(10, 100, TimeUnit.MILLISECONDS);
        bm.releaseMessage(10);
        bm.releaseMessage(20);
        bm.releaseMessage(10);
        bm.releaseMessage(20);
        bm.releaseMessage(10);
    }

    @Test
    public void testAcquireReleasesCountOnByteInterrupt() throws Exception {
        BufferManager bm = manager(20, 2);
        // Fill bytes completely: acquire 10 bytes
        bm.acquire(10);

        assertInterrupted(() -> bm.acquire(15));

        // Count semaphore permit should have been returned — we can acquire count again
        bm.tryAcquire(10); // success
        assertOverflow("[test] Rejecting a message due to reaching message queue in-flight limit of 2",
                () -> bm.tryAcquire(1)); // fail

        bm.releaseMessage(10);
        bm.releaseMessage(10);
    }

    @Test
    public void testTryAcquireWithTimeoutSuccess() throws Exception {
        BufferManager bm = manager(100, 5);
        bm.tryAcquire(50, 100, TimeUnit.MILLISECONDS);
        bm.releaseMessage(50);
    }

    @Test
    public void testTryAcquireWithTimeoutBytesTimeoutThrows() throws Exception {
        BufferManager bm = manager(20, 2);

        bm.tryAcquire(15, 1, TimeUnit.MILLISECONDS); // success
        assertTimeout("[test] Rejecting a message of 15 bytes: not enough space in message queue. "
                + "Buffer currently has 1 messages with 5 / 20 bytes available",
                () -> bm.tryAcquire(15, 1, TimeUnit.MILLISECONDS));

        bm.releaseSize(5);
        bm.tryAcquire(6, 1, TimeUnit.MILLISECONDS); // success
        assertTimeout("[test] Rejecting a message due to reaching message queue in-flight limit of 2",
                () -> bm.tryAcquire(1, 1, TimeUnit.MILLISECONDS));

        bm.releaseMessage(6);
        bm.releaseMessage(10); // acquired 15, reduced on 5 = 10

        bm.tryAcquire(20, 1, TimeUnit.MILLISECONDS); // success
        assertInterrupted(() -> bm.tryAcquire(20, 10, TimeUnit.SECONDS));
        bm.releaseMessage(20);

        // validate buffer - 20 bytes in 2 messages can be stored
        bm.tryAcquire(10);
        bm.tryAcquire(10);
        bm.releaseMessage(10);
        bm.releaseMessage(10);
    }

    /*

    @Test
    public void testTryAcquireWithTimeoutBytesTimeoutReleasesCount() throws Exception {
        BufferManager bm = manager(10, 2);
        bm.tryAcquire(6, 100, TimeUnit.MILLISECONDS);
        // Bytes exhausted → TimeoutException; count must be rolled back
        try {
            bm.tryAcquire(6, 1, TimeUnit.MILLISECONDS);
            Assert.fail("Expected TimeoutException");
        } catch (TimeoutException e) {
            // expected
        }
        // Count rolled back: can still fill second slot
        bm.tryAcquire(4, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testTryAcquireWithTimeoutInterruptedReleasesCount() throws Exception {
        BufferManager bm = manager(10, 5);
        bm.tryAcquire(10, 100, TimeUnit.MILLISECONDS); // fill bytes

        AtomicBoolean interrupted = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            started.countDown();
            try {
                bm.tryAcquire(5, 10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrupted.set(true);
            } catch (QueueOverflowException | TimeoutException e) {
                // unexpected
            }
        });
        t.start();
        Assert.assertTrue(started.await(1, TimeUnit.SECONDS));
        Thread.sleep(50);
        t.interrupt();
        t.join(1000);
        Assert.assertTrue(interrupted.get());

        // Count semaphore permit returned — free it and re-acquire
        bm.releaseMessage(10);
        bm.tryAcquire(10, 100, TimeUnit.MILLISECONDS);
        bm.releaseMessage(10);
    }

    // --- releaseSize ---

    @Test
    public void testReleaseSize() throws QueueOverflowException {
        BufferManager bm = manager(100, 10);
        bm.tryAcquire(60);
        // Release only bytes (not count) — simulates a size adjustment
        bm.releaseSize(20);
        // 40 bytes consumed; should be able to acquire 60 more bytes (up to 100)
        bm.tryAcquire(60);
    }

    // --- combined acquire/release cycle ---

    @Test
    public void testAcquireBlocksAndUnblocksOnRelease() throws Exception {
        BufferManager bm = manager(10, 10);
        bm.acquire(10); // fill bytes

        CountDownLatch acquired = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            try {
                bm.acquire(5);
                success.set(true);
                acquired.countDown();
            } catch (InterruptedException | QueueOverflowException e) {
                // unexpected
            }
        });
        t.start();
        Thread.sleep(50); // let thread block
        bm.releaseMessage(10); // release bytes so thread can proceed
        Assert.assertTrue(acquired.await(2, TimeUnit.SECONDS));
        Assert.assertTrue(success.get());
        t.join();
    }
*/
}
