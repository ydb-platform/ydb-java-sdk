package tech.ydb.topic.read.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.annotations.VisibleForTesting;
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
    private final long maxBufferSize;
    private final AtomicLong totalAvailable;

    private final Executor decompressor;
    private final CodecRegistry codecRegistry;
    private final ReaderMetrics metrics;

    private final Queue<ReadPartitionDecoder.EncodedMessage> decodingQueue = new ConcurrentLinkedQueue<>();
    private final SerialRunnable decodeNext = new SerialRunnable(new DecodeNext());
    private volatile boolean isStopped = false;

    public MessageDecoder(ReadConfig config) {
        this(config.getMaxMemoryUsageBytes(), config.getDecompressor(), config.getCodecRegistry(), config.getMetrics());
    }

    @VisibleForTesting
    MessageDecoder(long maxBufferSize, Executor decompressor, CodecRegistry codecRegistry, ReaderMetrics metrics) {
        if (maxBufferSize <= 0) {
            throw new IllegalArgumentException("maxBufferSize must be positive, but got " + maxBufferSize);
        }

        this.maxBufferSize = maxBufferSize;
        this.totalAvailable = new AtomicLong(maxBufferSize);
        this.decompressor = decompressor;
        this.codecRegistry = codecRegistry;
        this.metrics = metrics;
    }

    public void decodeNext() {
        decodeNext.run();
    }

    public void stop() {
        this.isStopped = true;
    }

    @VisibleForTesting
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
            while (!isStopped) {
                long available = totalAvailable.get();
                if (available <= 0) {
                    return;
                }

                // Only this runnable polls the queue and it is serialized, so peek() cannot be invalidated here
                ReadPartitionDecoder.EncodedMessage next = decodingQueue.peek();
                if (next == null) {
                    return;
                }

                // A message larger than the whole budget must still make progress, but only when nothing else
                // retains the buffer. Otherwise it waits until the already admitted messages are released.
                if (next.getUncompressedSize() > available && available != maxBufferSize) {
                    return;
                }

                decodingQueue.poll();

                long size = next.allocate();
                totalAvailable.addAndGet(-size);
                try {
                    decompressor.execute(() -> next.decode(codecRegistry, metrics));
                } catch (Throwable ex) {
                    logger.error("Cannot execute decompression ", ex);
                    next.setError(ex);
                }
            }
        }
    }
}
