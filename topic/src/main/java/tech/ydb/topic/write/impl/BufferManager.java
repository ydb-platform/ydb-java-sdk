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

    public void close(Status status) {
        this.closed = status;
        // release all waiters
        this.blocksAvailable.release(calculateBlocksCount(bufferMaxSize, blockBitsCount));
        this.countAvailable.release(maxCount);
    }

    public void acquire(long messageSize) throws InterruptedException, QueueOverflowException {
        if (closed != null) {
            throw new IllegalStateException("Writer was closed with status " + closed);
        }

        countAvailable.acquire();

        if (closed != null) {
            countAvailable.release();
            throw new IllegalStateException("Writer was closed with status " + closed);
        }

        int messageBlocks = calculateBlocksCount(messageSize, blockBitsCount);

        try {
            blocksAvailable.acquire(messageBlocks);
        } catch (InterruptedException ex) {
            countAvailable.release();
            throw ex;
        }

        if (closed != null) {
            blocksAvailable.release(messageBlocks);
            countAvailable.release();
            throw new IllegalStateException("Writer was closed with status " + closed);
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
        if (closed != null) {
            throw new IllegalStateException("Writer was closed with status " + closed);
        }

        long expireAt = System.nanoTime() + unit.toNanos(timeout);
        if (!countAvailable.tryAcquire(timeout, unit)) {
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
            if (!blocksAvailable.tryAcquire(messageBlocks, timeout2, TimeUnit.NANOSECONDS)) {
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
    }

    public void releaseMessage(long messageSize) {
        int blocks = calculateBlocksCount(messageSize, blockBitsCount);
        blocksAvailable.release(blocks);
        countAvailable.release();
    }

    public void updateMessageSize(long oldSize, long newSize) {
        int oldBlocks = calculateBlocksCount(oldSize, blockBitsCount);
        int newBlocks = calculateBlocksCount(newSize, blockBitsCount);
        blocksAvailable.release(oldBlocks - newBlocks);
    }

    private static int calculateBlockSize(long maxBufferSize) {
        int bits = 0;
        long blocksCount = maxBufferSize;
        while (blocksCount > Integer.MAX_VALUE - 1) {
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
