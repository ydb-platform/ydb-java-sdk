package tech.ydb.topic.read.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.impl.SerialRunnable;

/**
 * Decodes message batches while limiting memory consumption for uncompressed data.
 * @author Aleksandr Gorshenin
 */
public class MessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);
    private final AtomicLong totalAvailable;

    private final Executor decompressionExecutor;
    private final CodecRegistry codecRegistry;
    private final Queue<ReadPartitionDecoder.EncodedMessage> decodingQueue = new ConcurrentLinkedQueue<>();
    private final SerialRunnable decodeNext = new SerialRunnable(new DecodeNext());
    private volatile boolean isStopped = false;

    public MessageDecoder(long maxBufferSize, Executor decompressionExecutor, CodecRegistry codecRegistry) {
        this.totalAvailable = new AtomicLong(maxBufferSize);
        this.decompressionExecutor = decompressionExecutor;
        this.codecRegistry = codecRegistry;
    }

    public void decodeNext() {
        decodeNext.run();
    }

    public void stop() {
        this.isStopped = true;
    }

    long getTotalAvailable() {
        return totalAvailable.get();
    }

    void add(ReadPartitionDecoder.EncodedMessage task) {
        decodingQueue.add(task);
    }

    void free(long bufferSize) {
        if (isStopped) {
            return;
        }

        if (bufferSize > 0) {
            totalAvailable.addAndGet(bufferSize);
            decodeNext.run();
        }
    }

    private final class DecodeNext implements Runnable {
        @Override
        public void run() {
            while (!isStopped && totalAvailable.get() > 0) {
                ReadPartitionDecoder.EncodedMessage next = decodingQueue.poll();
                if (next == null) {
                    return;
                }

                long size = next.allocate();
                totalAvailable.addAndGet(-size);
                try {
                    decompressionExecutor.execute(() -> next.decode(codecRegistry));
                } catch (Throwable ex) {
                    logger.error("Cannot execute decompression ", ex);
                    next.setError(ex);
                }
            }
        }
    }
}
