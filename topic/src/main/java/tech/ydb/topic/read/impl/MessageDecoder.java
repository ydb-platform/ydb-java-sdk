package tech.ydb.topic.read.impl;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.utils.Encoder;

/**
 * Decodes message batches while limiting memory consumption for uncompressed data.
 * @author Aleksandr Gorshenin
 */
public class MessageDecoder {
    // TODO: Backward compatibility
    private static final Logger logger = LoggerFactory.getLogger(PartitionSessionImpl.class);

    private final AtomicLong availableBufferSize;
    private final Executor decompressionExecutor;
    private final Queue<DecodeTask> decodingQueue = new ConcurrentLinkedQueue<>();

    public MessageDecoder(long maxBufferSize, Executor decompressionExecutor) {
        this.availableBufferSize = new AtomicLong(maxBufferSize);
        this.decompressionExecutor = decompressionExecutor;
    }

    public void decode(String fullId, Batch batch, Runnable readyHandler) {
        decodingQueue.offer(new DecodeTask(fullId, batch, readyHandler));
        tryToDecodeNextBatch();
    }

    private void tryToDecodeNextBatch() {
        if (availableBufferSize.get() <= 0) {
            return;
        }

        while (availableBufferSize.get() > 0) {
            DecodeTask task = decodingQueue.poll();
            if (task == null) {
                return;
            }

            Batch batch = task.getBatch();
            if (batch.getReadFuture().isDone()) { // session was closed, just skip decoding
                continue;
            }

            long bufferSize = getUncompressedSize(batch);
            availableBufferSize.addAndGet(-bufferSize);
            batch.getReadFuture().whenComplete((v, th) -> {
                availableBufferSize.addAndGet(bufferSize);
                tryToDecodeNextBatch();
            });

            decompressionExecutor.execute(task);
        }
    }

    private long getUncompressedSize(Batch batch) {
        long uncompressed = 0;
        long compressed = 0;
        for (MessageImpl msg: batch.getMessages()) {
            uncompressed += msg.getUncompressedSize();
            compressed += msg.getData().length;
        }

        if (uncompressed > 0) {
            return uncompressed;
        }

        // TODO: Implement moving average for compression level
        return 2 * compressed;
    }

    private static class DecodeTask implements Runnable {
        private final String fullId;
        private final Batch batch;
        private final Runnable readyHandler;

        DecodeTask(String fullId, Batch batch, Runnable readyHandler) {
            this.fullId = fullId;
            this.batch = batch;
            this.readyHandler = readyHandler;
        }

        public Batch getBatch() {
            return batch;
        }

        @Override
        public void run() {
            if (logger.isTraceEnabled()) {
                logger.trace("[{}] Started decoding batch", fullId);
            }

            batch.getMessages().forEach(message -> {
                try {
                    message.setData(Encoder.decode(batch.getCodec(), message.getData()));
                } catch (IOException exception) {
                    message.setException(exception);
                    logger.warn("[{}] Exception was thrown while decoding a message: ", fullId, exception);
                }
            });
            batch.markAsReady();

            if (logger.isTraceEnabled()) {
                logger.trace("[{}] Finished decoding batch", fullId);
            }

            readyHandler.run();
        }
    }
}
