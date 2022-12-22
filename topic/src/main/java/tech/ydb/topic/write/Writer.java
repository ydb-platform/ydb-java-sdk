package tech.ydb.topic.write;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import tech.ydb.topic.settings.WriteSettings;


/**
 * @author Nikolay Perfilov
 */
public class Writer {

    /**
     * Initialize reading in the background. Non-blocking
     */
    public void start() { }

    /**
     * Send message and wait for the result (or until timeout runs off). Blocking.
     * @param data message data to write
     * @param settings write settings
     * @return {@link WriteAck} write acknowledgement
     */
    public WriteAck send(byte[] data, WriteSettings settings) throws TimeoutException {
        // Temp ----
        return new WriteAck(0, WriteAck.State.DISCARDED, null);
        // ---------
    }

    /**
     * Send message and wait infinitely for the result. Blocking.
     * @param data message data to write
     * @return {@link WriteAck} write acknowledgement
     */
    public WriteAck send(byte[] data) {
        try {
            return send(data, WriteSettings.newBuilder().build());
        } catch (TimeoutException ignored) {
            throw new RuntimeException("TimeoutException in simple send method");
        }
    }

    /**
     * Send message. Non-blocking.
     * @param data message data to write
     * @param settings write settings
     * @return {@link CompletableFuture} for write acknowledgement
     */
    public CompletableFuture<WriteAck> sendAsync(byte[] data, WriteSettings settings) {
        // Temp ----
        return CompletableFuture.completedFuture(null);
        // ---------
    }

    /**
     * Send message. Non-blocking.
     * @param data message data to write
     * @return {@link CompletableFuture} for write acknowledgement
     */
    public CompletableFuture<WriteAck> sendAsync(byte[] data) {
        // Temp ----
        return sendAsync(data, WriteSettings.newBuilder().build());
        // ---------
    }

    public MessageBuilder newMessage() {
        return new MessageBuilder(this);
    }

    public class MessageBuilder {
        private final Writer writer;
        private final WriteSettings.Builder writeSettings = WriteSettings.newBuilder();
        private byte[] data;

        MessageBuilder(Writer writer) {
            this.writer = writer;
        }

        public MessageBuilder setData(byte[] data) {
            this.data = data;
            return this;
        }

        public MessageBuilder setTimeout(Duration timeout) {
            writeSettings.setTimeout(timeout);
            return this;
        }

        public MessageBuilder setSeqNo(long seqNo) {
            writeSettings.setSeqNo(seqNo);
            return this;
        }

        public MessageBuilder setCreateTimestamp(Instant createTimestamp) {
            writeSettings.setCreateTimestamp(createTimestamp);
            return this;
        }

        public MessageBuilder setBlockingTimeout(Duration blockingTimeout) {
            writeSettings.setBlockingTimeout(blockingTimeout);
            return this;
        }

        public WriteAck send() throws TimeoutException {
            return writer.send(data, writeSettings.build());
        }

        public CompletableFuture<WriteAck> sendAsync() {
            return writer.sendAsync(data, writeSettings.build());
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
     * @param timeout  timeout to wait a Message with
     */
    public void waitForFinish(Duration timeout) throws TimeoutException {

    }

    /**
     * Waits until all internal threads are stopped and cleanup is performed.
     */
    public void waitForFinish() {
        try {
            waitForFinish(Duration.ZERO);
        } catch (TimeoutException ignored) {
            throw new RuntimeException("TimeoutException in simple waitForFinish method");
        }
    }
}
