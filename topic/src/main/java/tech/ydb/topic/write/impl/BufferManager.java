package tech.ydb.topic.write.impl;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.QueueOverflowException;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BufferManager {
    // use logger from WriterImpl
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);
    private static final long MAX_BLOCKS_COUNT = Integer.MAX_VALUE - 1;

    private final String debugId;
    private final long bufferMaxSize;
    private final int maxCount;
    private final int blockBitsCount;

    private final Semaphore blocksAvailable;
    private final Semaphore countAvailable;

    private volatile Status closed = null;

    public BufferManager(String id, WriterSettings settings) {
        this.debugId = id;

        this.maxCount = settings.getMaxSendBufferMessagesCount();
        this.bufferMaxSize = settings.getMaxSendBufferMemorySize();

        this.blockBitsCount = calculateBlockSize(bufferMaxSize);

        this.blocksAvailable = new Semaphore(calculateBlocksCount(bufferMaxSize, blockBitsCount), true);
        this.countAvailable = new Semaphore(maxCount, true);
    }

    public long getMaxSize() {
        return bufferMaxSize;
    }

    long getUsedSize() {
        long usedBlocks = (long) calculateBlocksCount(bufferMaxSize, blockBitsCount)
                - blocksAvailable.availablePermits();
        return usedBlocks << blockBitsCount;
    }

    public void close(Status status) {
        this.closed = status;
        // release all waiters
        this.blocksAvailable.release(calculateBlocksCount(bufferMaxSize, blockBitsCount));
        this.countAvailable.release(maxCount);
    }

    public void acquire(long messageSize) throws InterruptedException, QueueOverflowException {
        acquire(messageSize, null);
    }

    void acquire(long messageSize, WriterMetrics metrics) throws InterruptedException, QueueOverflowException {
        if (closed != null) {
            throw new IllegalStateException("Writer was closed with status " + closed);
        }

        long waitStarted = 0;
        if (!countAvailable.tryAcquire(0, TimeUnit.NANOSECONDS)) {
            if (metrics != null) {
                waitStarted = metrics.reportBufferWaitStart();
            }
            countAvailable.acquire();
        }

        if (closed != null) {
            countAvailable.release();
            throw new IllegalStateException("Writer was closed with status " + closed);
        }

        int messageBlocks = calculateBlocksCount(messageSize, blockBitsCount);

        try {
            if (!blocksAvailable.tryAcquire(messageBlocks, 0, TimeUnit.NANOSECONDS)) {
                if (waitStarted == 0 && metrics != null) {
                    waitStarted = metrics.reportBufferWaitStart();
                }
                blocksAvailable.acquire(messageBlocks);
            }
        } catch (InterruptedException ex) {
            countAvailable.release();
            throw ex;
        }

        if (closed != null) {
            blocksAvailable.release(messageBlocks);
            countAvailable.release();
            throw new IllegalStateException("Writer was closed with status " + closed);
        }
        if (metrics != null) {
            metrics.reportBufferWaitDuration(waitStarted);
        }
    }

    public void tryAcquire(long messageSize) throws QueueOverflowException {
        if (closed != null) {
            throw new IllegalStateException("Writer was closed with status " + closed);
        }

        if (!countAvailable.tryAcquire()) {
            String errorMsg = "[" + debugId + "] Rejecting a message due to reaching message queue in-flight limit of "
                    + maxCount;
            logger.warn(errorMsg);
            throw new QueueOverflowException(errorMsg);
        }

        if (closed != null) {
            countAvailable.release();
            throw new IllegalStateException("Writer was closed with status " + closed);
        }

        int messageBlocks = calculateBlocksCount(messageSize, blockBitsCount);
        if (!blocksAvailable.tryAcquire(messageBlocks)) {
            countAvailable.release();
            int count = maxCount - countAvailable.availablePermits();
            long size = ((long) blocksAvailable.availablePermits()) << blockBitsCount;
            String errorMsg = "[" + debugId + "] Rejecting a message of " + messageSize +
                    " bytes: not enough space in message queue. Buffer currently has " + count +
                    " messages with " + size + " / " + bufferMaxSize + " bytes available";
            logger.warn(errorMsg);
            throw new QueueOverflowException(errorMsg);
        }

        if (closed != null) {
            blocksAvailable.release(messageBlocks);
            countAvailable.release();
            throw new IllegalStateException("Writer was closed with status " + closed);
        }
    }

    public void tryAcquire(long messageSize, long timeout, TimeUnit unit) throws InterruptedException,
            QueueOverflowException, TimeoutException {
        tryAcquire(messageSize, timeout, unit, null);
    }

    void tryAcquire(long messageSize, long timeout, TimeUnit unit, WriterMetrics metrics) throws InterruptedException,
            QueueOverflowException, TimeoutException {
        if (closed != null) {
            throw new IllegalStateException("Writer was closed with status " + closed);
        }

        long expireAt = System.nanoTime() + unit.toNanos(timeout);
        long waitStarted = 0;
        boolean countAcquired = countAvailable.tryAcquire(0, TimeUnit.NANOSECONDS);
        if (!countAcquired) {
            if (metrics != null) {
                waitStarted = metrics.reportBufferWaitStart();
            }
            countAcquired = countAvailable.tryAcquire(timeout, unit);
        }
        if (!countAcquired) {
            String errorMsg = "[" + debugId + "] Rejecting a message due to reaching message queue in-flight limit of "
                    + maxCount;
            logger.warn(errorMsg);
            throw new TimeoutException(errorMsg);
        }

        if (closed != null) {
            countAvailable.release();
            throw new IllegalStateException("Writer was closed with status " + closed);
        }

        int messageBlocks = calculateBlocksCount(messageSize, blockBitsCount);

        try {
            // negative timeout is allowed for tryAcquire
            long timeout2 = expireAt - System.nanoTime();
            boolean blocksAcquired = blocksAvailable.tryAcquire(messageBlocks, 0, TimeUnit.NANOSECONDS);
            if (!blocksAcquired) {
                if (waitStarted == 0 && metrics != null) {
                    waitStarted = metrics.reportBufferWaitStart();
                }
                blocksAcquired = blocksAvailable.tryAcquire(messageBlocks, timeout2, TimeUnit.NANOSECONDS);
            }
            if (!blocksAcquired) {
                countAvailable.release();
                int count = maxCount - countAvailable.availablePermits();
                long size = ((long) blocksAvailable.availablePermits()) << blockBitsCount;
                String errorMsg = "[" + debugId + "] Rejecting a message of " + messageSize +
                        " bytes: not enough space in message queue. Buffer currently has " + count +
                        " messages with " + size + " / " + bufferMaxSize + " bytes available";
                logger.warn(errorMsg);
                throw new TimeoutException(errorMsg);
            }
        } catch (InterruptedException ex) {
            countAvailable.release();
            throw ex;
        }

        if (closed != null) {
            blocksAvailable.release(messageBlocks);
            countAvailable.release();
            throw new IllegalStateException("Writer was closed with status " + closed);
        }
        if (metrics != null) {
            metrics.reportBufferWaitDuration(waitStarted);
        }
    }

    public void releaseMessage(long messageSize) {
        int blocks = calculateBlocksCount(messageSize, blockBitsCount);
        blocksAvailable.release(blocks);
        countAvailable.release();
    }

    /**
     * Tries to update the buffer reservation for an already acquired message.
     * Releases unused blocks when the message becomes smaller. When it grows, reserves the requested additional
     * blocks if possible, or all currently available blocks otherwise.
     *
     * @param oldSize currently reserved message size in bytes
     * @param newSize required message size in bytes
     * @return effective reservation size in bytes
     */
    public long updateMessageSize(long oldSize, long newSize) {
        int oldBlocks = calculateBlocksCount(oldSize, blockBitsCount);
        int newBlocks = calculateBlocksCount(newSize, blockBitsCount);
        int difference = oldBlocks - newBlocks;

        if (difference >= 0) {
            blocksAvailable.release(difference);
            return newSize;
        }

        int requiredBlocks = -difference;

        if (blocksAvailable.tryAcquire(requiredBlocks)) {
            return newSize;
        }

        int acquiredBlocks = blocksAvailable.drainPermits();

        if (acquiredBlocks >= requiredBlocks) {
            blocksAvailable.release(acquiredBlocks - requiredBlocks);
            return newSize;
        }

        if (acquiredBlocks == 0) {
            return oldSize;
        }

        return (oldBlocks + (long) acquiredBlocks) << blockBitsCount;
    }

    private static int calculateBlockSize(long maxBufferSize) {
        int bits = 0;
        long blocksCount = maxBufferSize;
        while (blocksCount > MAX_BLOCKS_COUNT) {
            bits = bits + 1;
            blocksCount = blocksCount >>> 1;

            if (bits > 10) {
                throw new IllegalArgumentException("Writer buffer size must be less than 1024 GB");
            }
        }
        return bits;
    }

    private static int calculateBlocksCount(long messageSize, int bitsCount) {
        if (bitsCount == 0) {
            return (int) messageSize;
        }

        long blocks = messageSize >>> bitsCount;
        long reverse = blocks << bitsCount;
        return (int) ((reverse < messageSize) ?  blocks + 1 : blocks);
    }
}
