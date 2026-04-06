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
    private final int maxSize;
    private final int maxCount;

    private final Semaphore bytesAvailable;
    private final Semaphore countAvailable;

    public BufferManager(String id, WriterSettings settings) {
        this.id = id;
        this.maxSize = (int) settings.getMaxSendBufferMemorySize();
        this.maxCount = settings.getMaxSendBufferMessagesCount();

        this.bytesAvailable = new Semaphore(maxSize, true);
        this.countAvailable = new Semaphore(maxCount, true);
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void acquire(int messageSize) throws InterruptedException, QueueOverflowException {
        countAvailable.acquire();
        bytesAvailable.acquire(messageSize);
    }

    public void tryAcquire(int messageSize) throws QueueOverflowException {
        if (!countAvailable.tryAcquire()) {
            logger.warn("[{}] Rejecting a message due to reaching message queue in-flight limit of {}", id,
                    maxCount);
            throw new QueueOverflowException("Message queue in-flight limit of " + maxSize + " reached");
        }

        if (!bytesAvailable.tryAcquire(messageSize)) {
            countAvailable.release();
            int size = maxCount - countAvailable.availablePermits();
            String errorMessage = "[" + id + "] Rejecting a message of " + messageSize +
                    " bytes: not enough space in message queue. Buffer currently has " + size +
                    " messages with " + bytesAvailable.availablePermits() + " / " + maxSize + " bytes available";
            logger.warn(errorMessage);
            throw new QueueOverflowException(errorMessage);
        }
    }

    public void tryAcquire(int messageSize, long timeout, TimeUnit unit) throws InterruptedException,
            QueueOverflowException, TimeoutException {
        if (!countAvailable.tryAcquire(timeout, unit)) {
            logger.warn("[{}] Rejecting a message due to reaching message queue in-flight limit of {}", id,
                    maxCount);
            throw new QueueOverflowException("Message queue in-flight limit of " + maxSize + " reached");
        }

        if (!bytesAvailable.tryAcquire(messageSize, timeout, unit)) {
            countAvailable.release();
            int size = maxCount - countAvailable.availablePermits();
            String errorMessage = "[" + id + "] Rejecting a message of " + messageSize +
                    " bytes: not enough space in message queue. Buffer currently has " + size +
                    " messages with " + bytesAvailable.availablePermits() + " / " + maxSize + " bytes available";
            logger.warn(errorMessage);
            throw new TimeoutException(errorMessage);
        }
    }

    public void releaseMessage(int messageSize) {
        bytesAvailable.release(messageSize);
        countAvailable.release();
    }

    public void releaseSize(int size) {
        bytesAvailable.release(size);
    }
}
