package tech.ydb.topic.write.impl;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.QueueOverflowException;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BufferManager {
    // use logger from WriterImpl
    private static final Logger logger = LoggerFactory.getLogger(WriterImpl.class);

    private final String id;
    private final long bufferMaxSize;
    private final int maxCount;
    private final int blockBitsCount;

    private final Semaphore blocksAvailable;
    private final Semaphore countAvailable;

    public BufferManager(String id, WriterSettings settings) {
        this.id = id;

        this.maxCount = settings.getMaxSendBufferMessagesCount();
        this.bufferMaxSize = settings.getMaxSendBufferMemorySize();

        this.blockBitsCount = calculateBlockSize(bufferMaxSize);

        this.blocksAvailable = new Semaphore(calculateBlocksCount(bufferMaxSize, blockBitsCount), true);
        this.countAvailable = new Semaphore(maxCount, true);
    }

    public long getMaxSize() {
        return bufferMaxSize;
    }

    public void acquire(long messageSize) throws InterruptedException, QueueOverflowException {
        countAvailable.acquire();
        try {
            int messageBlocks = calculateBlocksCount(messageSize, blockBitsCount);
            blocksAvailable.acquire(messageBlocks);
        } catch (InterruptedException ex) {
            countAvailable.release();
            throw ex;
        }
    }

    public void tryAcquire(long messageSize) throws QueueOverflowException {
        if (!countAvailable.tryAcquire()) {
            String errorMessage = "[" + id + "] Rejecting a message due to reaching message queue in-flight limit of "
                    + maxCount;
            logger.warn(errorMessage);
            throw new QueueOverflowException(errorMessage);
        }

        int messageBlocks = calculateBlocksCount(messageSize, blockBitsCount);
        if (!blocksAvailable.tryAcquire(messageBlocks)) {
            countAvailable.release();
            int count = maxCount - countAvailable.availablePermits();
            int size = blocksAvailable.availablePermits() << blockBitsCount;
            String errorMessage = "[" + id + "] Rejecting a message of " + messageSize +
                    " bytes: not enough space in message queue. Buffer currently has " + count +
                    " messages with " + size + " / " + bufferMaxSize + " bytes available";
            logger.warn(errorMessage);
            throw new QueueOverflowException(errorMessage);
        }
    }

    public void tryAcquire(long messageSize, long timeout, TimeUnit unit) throws InterruptedException,
            QueueOverflowException, TimeoutException {
        long expireAt = System.nanoTime() + unit.toNanos(timeout);
        if (!countAvailable.tryAcquire(timeout, unit)) {
            String errorMessage = "[" + id + "] Rejecting a message due to reaching message queue in-flight limit of "
                    + maxCount;
            logger.warn(errorMessage);
            throw new TimeoutException(errorMessage);
        }

        try {
            // negative timeout is allowed for tryAcquire
            long timeout2 = unit.convert(expireAt - System.nanoTime(), TimeUnit.NANOSECONDS);
            int messageBlocks = calculateBlocksCount(messageSize, blockBitsCount);
            if (!blocksAvailable.tryAcquire(messageBlocks, timeout2, unit)) {
                countAvailable.release();
                int count = maxCount - countAvailable.availablePermits();
                int size = blocksAvailable.availablePermits() << blockBitsCount;
                String errorMessage = "[" + id + "] Rejecting a message of " + messageSize +
                        " bytes: not enough space in message queue. Buffer currently has " + count +
                        " messages with " + size + " / " + bufferMaxSize + " bytes available";
                logger.warn(errorMessage);
                throw new TimeoutException(errorMessage);
            }
        } catch (InterruptedException ex) {
            countAvailable.release();
            throw ex;
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
                throw new IllegalArgumentException("Writer buffer size must be less 1024 GB");
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
