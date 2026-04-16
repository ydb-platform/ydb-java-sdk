package tech.ydb.topic.write.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.QueueOverflowException;

/**
 * @author Aleksandr Gorshenin
 */
public class BufferManagerTest {

    private static BufferManager manager(long maxBytes, int maxCount) {
        return new BufferManager("test", WriterSettings.newBuilder()
                .setTopicPath("/test")
                .setMaxSendBufferMemorySize(maxBytes)
                .setMaxSendBufferMessagesCount(maxCount)
                .build()
        );
    }

    private static void assertOverflow(String msg, ThrowingRunnable runnable) {
        QueueOverflowException ex = Assert.assertThrows("Must be thrown QueueOverflowException",
                QueueOverflowException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    private static void assertTimeout(String msg, ThrowingRunnable runnable) {
        TimeoutException ex = Assert.assertThrows("Must be thrown TimeoutException", TimeoutException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    private static void assertIllegalState(String msg, ThrowingRunnable runnable) {
        IllegalStateException ex = Assert.assertThrows("Must be thrown IllegalStateException",
                IllegalStateException.class, runnable);
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
        while (t.isAlive()) {
            t.interrupt();
            t.join(100);
        }

        Assert.assertTrue(interrupted.get());
    }

    @Test
    public void testGetMaxSize() {
        BufferManager bm = manager(1024, 10);
        Assert.assertEquals(1024, bm.getMaxSize());
    }

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

        bm.updateMessageSize(15, 10);
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

    @Test
    public void testLargeBufferConfiguration() throws QueueOverflowException {
        BufferManager bm = manager(Integer.MAX_VALUE, 10);
        bm.tryAcquire(Integer.MAX_VALUE); //success
        assertOverflow("[test] Rejecting a message of 1 bytes: not enough space in message queue. "
                + "Buffer currently has 1 messages with 0 / 2147483647 bytes available",
                () -> bm.tryAcquire(1));
        bm.releaseMessage(Integer.MAX_VALUE); // success

        BufferManager bm2 = manager(0x80000000L, 10); // MAX_VALUE + 1 = 2GB
        bm2.tryAcquire(0x40000000);
        bm2.tryAcquire(0x40000000);
        assertOverflow("[test] Rejecting a message of 1 bytes: not enough space in message queue. "
                + "Buffer currently has 2 messages with 0 / 2147483648 bytes available",
                () -> bm2.tryAcquire(1));
        bm2.releaseMessage(0x40000000);
        bm2.releaseMessage(0x40000000);

        BufferManager bm3 = manager(0x800000000L, 100); // 32 GB
        for (int idx = 0; idx < 64; idx++) {
            bm3.tryAcquire(0x20000000);
        }
        assertOverflow("[test] Rejecting a message of 1 bytes: not enough space in message queue. "
                + "Buffer currently has 64 messages with 0 / 34359738368 bytes available",
                () -> bm3.tryAcquire(1));
        for (int idx = 0; idx < 64; idx++) {
            bm3.releaseMessage(0x20000000);
        }

        IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> manager(0x20000000000L, 1000000)); // 2024 GB
        Assert.assertEquals("Writer buffer size must be less than 1024 GB", ex.getMessage());
    }

    @Test
    public void testLargeBufferSize() throws Exception {
        // With maxBytes = 0x100000003L, calculateBlockSize returns 2 (blockBitsCount=2)
        // so all messages are rounded up to 4-byte blocks
        BufferManager bm = manager(0x100000003L, 10);
        Assert.assertEquals(0x100000003L, bm.getMaxSize());

        // every message acquire 1 block = 4 bytes
        bm.tryAcquire(1);
        bm.tryAcquire(1);
        bm.tryAcquire(1);

        bm.tryAcquire(0x40000000);
        bm.tryAcquire(0x40000000);
        bm.tryAcquire(0x40000000);

        // there is not overflow by bytes,
        assertOverflow("[test] Rejecting a message of 1073741824 bytes: not enough space in message queue. "
                + "Buffer currently has 6 messages with 1073741816 / 4294967299 bytes available",
                () -> bm.tryAcquire(0x40000000));

        bm.releaseMessage(0x40000000);
        bm.releaseMessage(0x40000000);
        bm.releaseMessage(0x40000000);
        bm.releaseMessage(1);
        bm.releaseMessage(1);
        bm.releaseMessage(1);
    }

    @Test
    public void testClosedBuffer() {
        BufferManager bm = manager(100, 3);

        bm.close(Status.SUCCESS);

        assertIllegalState("Writer was closed with status Status{code = SUCCESS}",
                () -> bm.acquire(1));
        assertIllegalState("Writer was closed with status Status{code = SUCCESS}",
                () -> bm.tryAcquire(1));
        assertIllegalState("Writer was closed with status Status{code = SUCCESS}",
                () -> bm.tryAcquire(1, 1, TimeUnit.SECONDS));
    }

    @Test
    public void testReleaseCountOnBufferClosing() throws InterruptedException, QueueOverflowException {
        BufferManager bm = manager(100, 3);

        bm.acquire(10);
        bm.acquire(10);
        bm.acquire(10);

        CountDownLatch started = new CountDownLatch(2);
        Queue<Exception> problems = new ConcurrentLinkedQueue<>();
        Thread t1 = new Thread(() -> {
            try {
                started.countDown();
                bm.acquire(10);
            } catch (InterruptedException | QueueOverflowException | RuntimeException ex) {
                problems.add(ex);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                started.countDown();
                bm.tryAcquire(10, 1, TimeUnit.MINUTES);
            } catch (InterruptedException | QueueOverflowException | TimeoutException | RuntimeException ex) {
                problems.add(ex);
            }
        });
        t1.start();
        t2.start();

        Assert.assertTrue(started.await(1, TimeUnit.SECONDS));

        bm.close(Status.of(StatusCode.ABORTED));

        t1.join(100);
        t2.join(100);

        Assert.assertEquals(2, problems.size());
        for (Exception ex: problems) {
            Assert.assertTrue("Unexpected " + ex.getClass(), ex instanceof IllegalStateException);
            Assert.assertEquals("Writer was closed with status Status{code = ABORTED(code=400040)}", ex.getMessage());
        }
    }

    @Test
    public void testReleaseSizeOnBufferClosing() throws InterruptedException, QueueOverflowException {
        BufferManager bm = manager(70, 5);

        bm.acquire(20);
        bm.acquire(20);
        bm.acquire(20);

        CountDownLatch started = new CountDownLatch(2);
        Queue<Exception> problems = new ConcurrentLinkedQueue<>();
        Thread t1 = new Thread(() -> {
            try {
                started.countDown();
                bm.acquire(70);
            } catch (InterruptedException | QueueOverflowException | RuntimeException ex) {
                problems.add(ex);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                started.countDown();
                bm.tryAcquire(70, 1, TimeUnit.MINUTES);
            } catch (InterruptedException | QueueOverflowException | TimeoutException | RuntimeException ex) {
                problems.add(ex);
            }
        });
        t1.start();
        t2.start();

        Assert.assertTrue(started.await(1, TimeUnit.SECONDS));

        bm.close(Status.of(StatusCode.TIMEOUT));

        t1.join(100);
        t2.join(100);

        Assert.assertEquals(2, problems.size());
        for (Exception ex: problems) {
            Assert.assertTrue("Unexpected " + ex.getClass(), ex instanceof IllegalStateException);
            Assert.assertEquals("Writer was closed with status Status{code = TIMEOUT(code=400090)}", ex.getMessage());
        }
    }
}
