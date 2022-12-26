package tech.ydb.topic.write;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;


/**
 * @author Nikolay Perfilov
 */
public class Writer {

    /**
     * Initialize reading in the background. Non-blocking
     */
    public void start() { }

    /**
     * Send message data. Blocks if in-flight or memory usage limits is reached.
     * @param data message data to write
     */
    public void send(byte[] data) {
        try {
            send(data, null);
        } catch (TimeoutException ignored) {
            throw new RuntimeException("TimeoutException in simple send method"); // should not happen
        }
    }

    /**
     * Send message. Blocks if in-flight or memory usage limits is reached.
     * @param message message data to write
     */
    public void send(Message message) {
        try {
            send(message, null);
        } catch (TimeoutException ignored) {
            throw new RuntimeException("TimeoutException in simple send method"); // should not happen
        }
    }

    /**
     * Send message data
     * @param data message data to write
     * @param blockTimeout time to wait until the message is put into internal sending queue.
     *                     null: wait infinitely until message is put into queue.
     *                     Duration.ZERO: do not block.
     *                     TimeoutException is thrown if message was not put into queue within timeout
     *                     due to in-flight or memory usage limits
     */
    public void send(byte[] data, Duration blockTimeout) throws TimeoutException {
        send(Message.of(data), blockTimeout);
    }

    /**
     * Send message
     * @param message message to write
     * @param blockTimeout time to wait until the message is put into internal sending queue.
     *                     null: wait infinitely until message is put into queue.
     *                     Duration.ZERO: do not block.
     *                     TimeoutException is thrown if message was not put into queue within timeout
     *                     due to in-flight or memory usage limits
     */
    public void send(Message message, Duration blockTimeout) throws TimeoutException {
    }

    /**
     * Send message and wait for {@link WriteAck} response. This is a slow way and a bad choice in most cases
     * @param message message to write
     * @param timeout time to wait until the WriteAck response is received.
     *                null: wait infinitely until message is put into queue.
     *                TimeoutException is thrown if WriteAck within timeout
     *                due to in-flight or memory usage limits
     */
    public WriteAck sendWithAck(Message message, Duration timeout) throws TimeoutException {
        return new WriteAck(0, WriteAck.State.DISCARDED, null);
    }

    /**
     * Send message data. Non-blocking
     * @param data message data to write
     * @return {@link CompletableFuture} for write acknowledgement
     */
    public CompletableFuture<WriteAck> sendAsync(byte[] data) {
        return sendAsync(data, null);
    }

    /**
     * Send message. Non-blocking
     * @param message message data to write
     * @return {@link CompletableFuture} for write acknowledgement
     */
    public CompletableFuture<WriteAck> sendAsync(Message message) {
        return sendAsync(message, null);
    }

    /**
     * Send message data. Non-blocking.
     * @param data message data to write
     * @param blockTimeout time to wait until the message is put into internal sending queue before failing.
     *                     null: wait infinitely until message is put into queue.
     *                     Completes exceptionally with {@link TimeoutException} if message was not put into queue
     *                     within timeout due to in-flight or memory usage limits
     * @return {@link CompletableFuture} for write acknowledgement
     */
    public CompletableFuture<WriteAck> sendAsync(byte[] data, Duration blockTimeout) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Send message. Non-blocking.
     * @param message message data to write
     * @param blockTimeout time to wait until the message is put into internal sending queue before failing.
     *                     null: wait infinitely until message is put into queue.
     *                     Completes exceptionally with {@link TimeoutException} if message was not put into
     *                     queue within timeout due to in-flight or memory usage limits
     * @return {@link CompletableFuture} for write acknowledgement
     */
    public CompletableFuture<WriteAck> sendAsync(Message message, Duration blockTimeout) {
        return CompletableFuture.completedFuture(null);
    }

    public MessageBuilder newMessage() {
        return new MessageBuilder(this);
    }

    public static class MessageBuilder {
        private final Writer writer;
        private final Message.Builder message = Message.newBuilder();
        private Duration blockTimeout = null;

        MessageBuilder(Writer writer) {
            this.writer = writer;
        }

        public MessageBuilder setData(byte[] data) {
            message.setData(data);
            return this;
        }

        public MessageBuilder setSeqNo(long seqNo) {
            message.setSeqNo(seqNo);
            return this;
        }

        public MessageBuilder setCreateTimestamp(Instant createTimestamp) {
            message.setCreateTimestamp(createTimestamp);
            return this;
        }

        /**
         * Set blocking timeout
         * @param blockTimeout time to wait until the message is put into internal sending queue.
         *                     null: wait infinitely until message is put into queue.
         *                     Duration.ZERO: do not block.
         *                     TimeoutException is thrown if message was not put into queue within timeout
         *                     due to in-flight or memory usage limits
         * @return {@link CompletableFuture} for write acknowledgement
         */
        public MessageBuilder setBlockingTimeout(Duration blockTimeout) {
            this.blockTimeout = blockTimeout;
            return this;
        }

        public void send() throws TimeoutException {
            writer.send(message.build(), blockTimeout);
        }

        public WriteAck sendWithAck() throws TimeoutException {
            return writer.sendWithAck(message.build(), blockTimeout);
        }

        public CompletableFuture<WriteAck> sendAsync() {
            return writer.sendAsync(message.build(), blockTimeout);
        }
    }

    /**
     * Finishes current writes, stops internal threads and makes cleanup in background. Non-blocking
     */
    public void close() {

    }

    /**
     * Waits until all internal threads are stopped and cleanup is performed.
     * Throws {@link java.util.concurrent.TimeoutException} if timeout runs off
     *
     * @param timeout  timeout to wait with
     *                 null: wait infinitely until message is put into queue.
     */
    public void waitForFinish(Duration timeout) throws TimeoutException {

    }

    /**
     * Waits until all internal threads are stopped and cleanup is performed.
     */
    public void waitForFinish() {
        try {
            waitForFinish(null);
        } catch (TimeoutException ignored) {
            throw new RuntimeException("TimeoutException in simple waitForFinish method");
        }
    }
}
