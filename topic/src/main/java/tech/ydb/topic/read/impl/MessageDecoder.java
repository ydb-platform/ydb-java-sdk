package tech.ydb.topic.read.impl;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.utils.Encoder;

/**
 * Decodes message batches while limiting memory consumption for uncompressed data.
 * @author Aleksandr Gorshenin
 */
public class MessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

    private final long maxBufferSize;
    private final Executor decompressionExecutor;
    private final CodecRegistry codecRegistry;
    private final Deque<DecodeTask> decodingQueue = new ArrayDeque<>();
    private final ReentrantLock stateLock = new ReentrantLock();

    private long availableBufferSize;

    public MessageDecoder(long maxBufferSize, Executor decompressionExecutor, CodecRegistry codecRegistry) {
        if (maxBufferSize <= 0) {
            throw new IllegalArgumentException("maxBufferSize must be positive");
        }

        this.maxBufferSize = maxBufferSize;
        this.availableBufferSize = maxBufferSize;
        this.decompressionExecutor = decompressionExecutor;
        this.codecRegistry = codecRegistry;
    }

    public void decode(String traceID, Batch batch, Runnable readyHandler) {
        stateLock.lock();
        try {
            decodingQueue.offer(new DecodeTask(traceID, batch, readyHandler));
        } finally {
            stateLock.unlock();
        }
        scheduleDecodingTasks();
    }

    private void scheduleDecodingTasks() {
        for (DecodeTask task = pollNextTask(); task != null; task = pollNextTask()) {
            try {
                decompressionExecutor.execute(task);
            } catch (RuntimeException exception) {
                logger.error("[{}] Failed to submit decoding task to executor", task.traceID, exception);
                task.fail(exception);
            }
        }
    }

    private DecodeTask pollNextTask() {
        stateLock.lock();
        try {
            while (availableBufferSize > 0) {
                DecodeTask task = decodingQueue.peek();

                if (task == null) {
                    return null;
                }

                if (task.getBatch().getReadFuture().isDone()) {
                    decodingQueue.poll();
                    continue;
                }

                long bufferSize = getUncompressedSize(task.getBatch());

                // A single oversized batch must still make progress, but it must be the only retained batch.
                if (bufferSize > availableBufferSize && availableBufferSize != maxBufferSize) {
                    return null;
                }

                decodingQueue.poll();
                availableBufferSize -= bufferSize;
                task.setReservedBufferSize(bufferSize);

                if (logger.isTraceEnabled()) {
                    logger.trace(
                            "[{}] Reserved {} bytes for decompression, {} bytes remain available",
                            task.traceID,
                            bufferSize,
                            availableBufferSize
                    );
                }

                task.getBatch().getReadFuture().whenComplete((v, th) -> releaseBuffer(bufferSize));
                return task;
            }

            return null;
        } finally {
            stateLock.unlock();
        }
    }

    private void releaseBuffer(long bufferSize) {
        stateLock.lock();
        try {
            availableBufferSize += bufferSize;
        } finally {
            stateLock.unlock();
        }
        scheduleDecodingTasks();
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

    private class DecodeTask implements Runnable {
        private final String traceID;
        private final Batch batch;
        private final Runnable readyHandler;
        private long reservedBufferSize;

        DecodeTask(String traceID, Batch batch, Runnable readyHandler) {
            this.traceID = traceID;
            this.batch = batch;
            this.readyHandler = readyHandler;
        }

        public Batch getBatch() {
            return batch;
        }

        void setReservedBufferSize(long reservedBufferSize) {
            this.reservedBufferSize = reservedBufferSize;
        }

        @Override
        public void run() {
            if (logger.isTraceEnabled()) {
                logger.trace("[{}] Started decoding batch", traceID);
            }

            long producedSize = 0;
            try {
                for (MessageImpl message : batch.getMessages()) {
                    try {
                        byte[] decoded = Encoder.decode(batch.getCodec(), message.getData(), codecRegistry);
                        message.setData(decoded);
                        producedSize += decoded.length;
                    } catch (IOException | RuntimeException decodingException) {
                        IOException exception = asIOException(decodingException);
                        message.setException(exception);
                        logger.warn("[{}] Exception was thrown while decoding a message: ", traceID, exception);
                    }
                }
            } finally {
                if (logger.isTraceEnabled()) {
                    logger.trace(
                            "[{}] Finished decoding batch: previously reserved {} bytes, actually produced {} bytes",
                            traceID,
                            reservedBufferSize,
                            producedSize
                    );
                }

                finishBatch();
            }
        }

        void fail(Throwable throwable) {
            IOException exception = asIOException(throwable);
            batch.getMessages().forEach(message -> message.setException(exception));
            finishBatch();
        }

        IOException asIOException(Throwable throwable) {
            return throwable instanceof IOException
                    ? (IOException) throwable
                    : new IOException("Unexpected decompression failure", throwable);
        }

        void finishBatch() {
            try {
                batch.markAsReady();
                readyHandler.run();
            } catch (Throwable throwable) {
                logger.error("[{}] Exception while completing decoded batch:", traceID, throwable);
            }
        }
    }
}
