package tech.ydb.topic.settings;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import tech.ydb.topic.description.Codec;

/**
 * Settings for a multiwriter using public partition AsyncWriter instances.
 * The byte budget bounds accepted payload and codec reservations, not all JVM/protobuf/gRPC allocations.
 * Custom codecs must provide a truthful conservative getMaxEncodedSize bound in the TopicClient registry.
 * A producer prefix has one active owner for a topic. It recovers sequence history after restart,
 * but does not persist payload or identify a repeated business operation.
 */
public final class MultiWriterSettings {
    public enum RoutingMode {
        AUTO,
        BOUND,
        HASH,
        EXPLICIT
    }

    private final String topicPath;
    private final String producerPrefix;
    private final RoutingMode routingMode;
    private final long bufferBytes;
    private final int bufferMessages;
    private final int maxMessageBytes;
    private final int maxWriters;
    private final int maxConcurrentInits;
    private final int ioThreads;
    private final long writerBufferBytes;
    private final int writerBufferMessages;
    private final Duration initTimeout;
    private final Duration recoveryTimeout;
    private final Duration closeTimeout;
    private final Duration idleTimeout;
    private final Duration deliveryTimeout;
    private final int codec;
    private final boolean directWrite;

    private MultiWriterSettings(Builder builder) {
        this.topicPath = builder.topicPath;
        this.producerPrefix = builder.producerPrefix;
        this.routingMode = builder.routingMode;
        this.bufferBytes = builder.bufferBytes;
        this.bufferMessages = builder.bufferMessages;
        this.maxMessageBytes = builder.maxMessageBytes;
        this.maxWriters = builder.maxWriters;
        this.maxConcurrentInits = builder.maxConcurrentInits;
        this.ioThreads = builder.ioThreads;
        this.writerBufferBytes = builder.writerBufferBytes;
        this.writerBufferMessages = builder.writerBufferMessages;
        this.initTimeout = builder.initTimeout;
        this.recoveryTimeout = builder.recoveryTimeout;
        this.closeTimeout = builder.closeTimeout;
        this.idleTimeout = builder.idleTimeout;
        this.deliveryTimeout = builder.deliveryTimeout;
        this.codec = builder.codec;
        this.directWrite = builder.directWrite;
        validate();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getTopicPath() {
        return topicPath;
    }

    public String getProducerPrefix() {
        return producerPrefix;
    }

    public RoutingMode getRoutingMode() {
        return routingMode;
    }

    public long getBufferBytes() {
        return bufferBytes;
    }

    public int getBufferMessages() {
        return bufferMessages;
    }

    public int getMaxMessageBytes() {
        return maxMessageBytes;
    }

    public int getMaxWriters() {
        return maxWriters;
    }

    public int getMaxConcurrentInits() {
        return maxConcurrentInits;
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public long getWriterBufferBytes() {
        return writerBufferBytes;
    }

    public int getWriterBufferMessages() {
        return writerBufferMessages;
    }

    public Duration getInitTimeout() {
        return initTimeout;
    }

    public Duration getRecoveryTimeout() {
        return recoveryTimeout;
    }

    public Duration getCloseTimeout() {
        return closeTimeout;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public Duration getDeliveryTimeout() {
        return deliveryTimeout;
    }

    public int getCodec() {
        return codec;
    }

    public boolean isDirectWrite() {
        return directWrite;
    }

    private void validate() {
        requireText(topicPath, "topicPath");
        requireText(producerPrefix, "producerPrefix");
        // Leave room for '-' and a decimal partition ID. This is a client-side bound, not a server promise.
        if (producerPrefix.getBytes(StandardCharsets.UTF_8).length > 2000) {
            throw new IllegalArgumentException("Producer prefix must not exceed 2000 UTF-8 bytes");
        }
        Objects.requireNonNull(routingMode, "routingMode");
        positive(bufferBytes, "bufferBytes");
        positive(bufferMessages, "bufferMessages");
        positive(maxMessageBytes, "maxMessageBytes");
        positive(maxWriters, "maxWriters");
        positive(maxConcurrentInits, "maxConcurrentInits");
        positive(ioThreads, "ioThreads");
        positive(writerBufferBytes, "writerBufferBytes");
        positive(writerBufferMessages, "writerBufferMessages");
        if (writerBufferBytes > Integer.MAX_VALUE - 1L) {
            throw new IllegalArgumentException("Partition writer byte buffer must be at most Integer.MAX_VALUE - 1");
        }
        if (maxMessageBytes > bufferBytes || maxMessageBytes > writerBufferBytes) {
            throw new IllegalArgumentException("Maximum message size must fit both configured byte buffers");
        }
        if (maxConcurrentInits > maxWriters) {
            throw new IllegalArgumentException("Concurrent init limit must not exceed the writer limit");
        }
        if (codec <= 0) {
            throw new IllegalArgumentException("Codec ID must be positive and registered on TopicClient");
        }
        duration(initTimeout, "initTimeout", false);
        duration(recoveryTimeout, "recoveryTimeout", false);
        duration(closeTimeout, "closeTimeout", false);
        duration(idleTimeout, "idleTimeout", false);
        duration(deliveryTimeout, "deliveryTimeout", true);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }

    private static void positive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void duration(Duration value, String name, boolean allowZero) {
        Objects.requireNonNull(value, name);
        if (value.isNegative() || (!allowZero && value.isZero())) {
            throw new IllegalArgumentException(name + " must be positive" + (allowZero ? " or zero" : ""));
        }
        try {
            value.toNanos();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(name + " is too large for a nanosecond deadline", ex);
        }
    }

    public static final class Builder {
        private String topicPath;
        private String producerPrefix;
        private RoutingMode routingMode = RoutingMode.AUTO;
        private long bufferBytes = 64L * 1024 * 1024;
        private int bufferMessages = 10000;
        private int maxMessageBytes = 1024 * 1024;
        private int maxWriters = 32;
        private int maxConcurrentInits = 4;
        private int ioThreads = 4;
        private long writerBufferBytes = 8L * 1024 * 1024;
        private int writerBufferMessages = 1024;
        private Duration initTimeout = Duration.ofSeconds(30);
        private Duration recoveryTimeout = Duration.ofSeconds(30);
        private Duration closeTimeout = Duration.ofSeconds(30);
        private Duration idleTimeout = Duration.ofSeconds(60);
        private Duration deliveryTimeout = Duration.ZERO;
        private int codec = Codec.RAW;
        private boolean directWrite = false;

        public Builder setTopicPath(String topicPath) {
            this.topicPath = topicPath;
            return this;
        }

        public Builder setProducerPrefix(String producerPrefix) {
            this.producerPrefix = producerPrefix;
            return this;
        }

        /**
         * AUTO selects once at bootstrap. HASH requires a stable partition set; BOUND requires complete key bounds.
         * EXPLICIT cannot choose a child after split without a key and therefore reports an error.
         * @param routingMode routing strategy
         * @return this builder
         */
        public Builder setRoutingMode(RoutingMode routingMode) {
            this.routingMode = routingMode;
            return this;
        }

        public Builder setBufferBytes(long bufferBytes) {
            this.bufferBytes = bufferBytes;
            return this;
        }

        public Builder setBufferMessages(int bufferMessages) {
            this.bufferMessages = bufferMessages;
            return this;
        }

        public Builder setMaxMessageBytes(int maxMessageBytes) {
            this.maxMessageBytes = maxMessageBytes;
            return this;
        }

        public Builder setMaxWriters(int maxWriters) {
            this.maxWriters = maxWriters;
            return this;
        }

        public Builder setMaxConcurrentInits(int maxConcurrentInits) {
            this.maxConcurrentInits = maxConcurrentInits;
            return this;
        }

        public Builder setIoThreads(int ioThreads) {
            this.ioThreads = ioThreads;
            return this;
        }

        public Builder setWriterBufferBytes(long writerBufferBytes) {
            this.writerBufferBytes = writerBufferBytes;
            return this;
        }

        public Builder setWriterBufferMessages(int writerBufferMessages) {
            this.writerBufferMessages = writerBufferMessages;
            return this;
        }

        public Builder setInitTimeout(Duration initTimeout) {
            this.initTimeout = initTimeout;
            return this;
        }

        public Builder setRecoveryTimeout(Duration recoveryTimeout) {
            this.recoveryTimeout = recoveryTimeout;
            return this;
        }

        public Builder setCloseTimeout(Duration closeTimeout) {
            this.closeTimeout = closeTimeout;
            return this;
        }

        public Builder setIdleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        /**
         * Sets a delivery deadline. Zero disables it; expiration leaves the server write outcome unknown.
         * @param deliveryTimeout deadline after admission, not a caller wait timeout
         * @return this builder
         */
        public Builder setDeliveryTimeout(Duration deliveryTimeout) {
            this.deliveryTimeout = deliveryTimeout;
            return this;
        }

        /**
         * Selects a codec registered on TopicClient. Its encoded-size bound must be conservative.
         * @param codec codec ID
         * @return this builder
         */
        public Builder setCodec(int codec) {
            this.codec = codec;
            return this;
        }

        public Builder setDirectWrite(boolean directWrite) {
            this.directWrite = directWrite;
            return this;
        }

        public MultiWriterSettings build() {
            return new MultiWriterSettings(this);
        }
    }
}
