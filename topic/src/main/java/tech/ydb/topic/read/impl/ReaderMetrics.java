package tech.ydb.topic.read.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;
import tech.ydb.proto.topic.YdbTopic;

/**
 * Topic reader metrics.
 */
final class ReaderMetrics {
    interface Source {
        long getMessagesCount();
        long getOldestReceivedTimestamp();
    }

    private static final String MESSAGE_UNIT = "{message}";

    private final LongCounter deliveredMessages;
    private final LongCounter receivedMessages;
    private final LongCounter receivedBytes;
    private final LongCounter commitQueued;
    private final LongCounter commitAcknowledged;
    private final LongCounter sessionErrors;
    private final DoubleHistogram compressionDuration;
    private final DoubleHistogram processingDuration;
    private final Attr[] commonAttributes;
    private final boolean enabled;
    private final Meter meter;
    private final Set<Source> sources = ConcurrentHashMap.newKeySet();
    private MetricRegistration partitionsGauge = MetricRegistration.NOOP;
    private MetricRegistration creditGauge = MetricRegistration.NOOP;
    private MetricRegistration commitLagGauge = MetricRegistration.NOOP;
    private MetricRegistration bufferMessagesGauge = MetricRegistration.NOOP;
    private MetricRegistration ageGauge = MetricRegistration.NOOP;

    ReaderMetrics(Meter meter, String consumer, String readerName) {
        this.meter = meter;
        this.enabled = meter != Meter.NOOP;
        this.deliveredMessages = meter.createCounter(
                "ydb.topic.reader.delivered.messages",
                MESSAGE_UNIT,
                "The number of messages delivered by the SDK to application code.");
        this.receivedMessages = meter.createCounter("ydb.topic.reader.received.messages", MESSAGE_UNIT,
                "The number of messages accepted by the SDK for an active partition session.");
        this.receivedBytes = meter.createCounter("ydb.topic.reader.received.bytes", "By",
                "The protocol bytes_size received in read responses.");
        this.commitQueued = meter.createCounter("ydb.topic.reader.commit.queued", MESSAGE_UNIT,
                "The number of messages in commit ranges accepted by the SDK.");
        this.commitAcknowledged = meter.createCounter("ydb.topic.reader.commit.acknowledged", MESSAGE_UNIT,
                "The number of messages acknowledged by advances of the committed offset.");
        this.sessionErrors = meter.createCounter("ydb.topic.reader.session.errors", "{error}",
                "The number of reader stream session errors by retry decision.");
        this.compressionDuration = meter.createHistogram("ydb.topic.reader.compression.duration", "s",
                "The time spent decompressing a message, excluding queueing.");
        this.processingDuration = meter.createHistogram("ydb.topic.reader.message.processing.duration", "s",
                "The time from receipt until a message is ready for delivery.");
        this.commonAttributes = createCommonAttributes(consumer, readerName);
    }

    void addSource(Source source) {
        sources.add(source);
    }

    void removeSource(Source source) {
        sources.remove(source);
    }

    private Attr[] topicAttributes(String topic) {
        Attr[] attributes = Arrays.copyOf(commonAttributes, commonAttributes.length + 1);
        attributes[commonAttributes.length] = Attr.of("topic", topic);
        return attributes;
    }

    private long getLocalBufferMessages() {
        return sources.stream().mapToLong(Source::getMessagesCount).sum();
    }

    private long getOldestReceivedTimestamp() {
        long oldest = 0;
        for (Source source : sources) {
            long timestamp = source.getOldestReceivedTimestamp();
            if (timestamp != 0 && (oldest == 0 || timestamp < oldest)) {
                oldest = timestamp;
            }
        }
        return oldest;
    }

    void register(LongSupplier bufferBudget, Supplier<Map<String, Long>> partitionCounts,
            Supplier<Map<String, Long>> commitLag) {
        partitionsGauge = meter.registerLongGauge(
                "ydb.topic.reader.partition_session.count", "{session}",
                "The number of partition sessions currently in the reader session processing lifecycle.",
                m -> partitionCounts.get().forEach((topic, count) -> m.record(count, topicAttributes(topic))));
        creditGauge = meter.registerLongGauge("ydb.topic.reader.credit_balance_bytes", "By",
                "The protocol credit granted to the server and not yet consumed by read responses.",
                m -> m.record(bufferBudget.getAsLong(), commonAttributes));
        commitLagGauge = meter.registerLongGauge("ydb.topic.reader.commit_offset.lag.max", MESSAGE_UNIT,
                "The maximum gap between requested and acknowledged commit offsets.",
                m -> commitLag.get().forEach((topic, lag) -> m.record(lag, topicAttributes(topic))));
        bufferMessagesGauge = meter.registerLongGauge("ydb.topic.reader.local_buffer.messages", MESSAGE_UNIT,
                "The number of messages currently buffered by the reader.",
                m -> m.record(getLocalBufferMessages(), commonAttributes));
        ageGauge = meter.registerDoubleGauge("ydb.topic.reader.local_buffer.message_age.max", "s",
                "The age of the oldest batch retained in the reader's local buffer.",
                m -> {
                    long timestamp = getOldestReceivedTimestamp();
                    m.record(timestamp == 0 ? 0 : (System.nanoTime() - timestamp) / 1_000_000_000d,
                            commonAttributes);
                });
    }

    void unregister() {
        partitionsGauge.close();
        creditGauge.close();
        commitLagGauge.close();
        bufferMessagesGauge.close();
        ageGauge.close();
        partitionsGauge = MetricRegistration.NOOP;
        creditGauge = MetricRegistration.NOOP;
        commitLagGauge = MetricRegistration.NOOP;
        bufferMessagesGauge = MetricRegistration.NOOP;
        ageGauge = MetricRegistration.NOOP;
        sources.clear();
    }

    void reportDelivered(long messages, String topic) {
        report(deliveredMessages, messages, topic);
    }

    void reportReceivedMessages(long messages, String topic) {
        report(receivedMessages, messages, topic);
    }

    void reportReceivedBytes(long bytes) {
        if (enabled) {
            record(receivedBytes, bytes);
        }
    }

    void reportSessionError(Status status, boolean retry) {
        StatusCode code = status.getCode();
        Attr[] attributes = Arrays.copyOf(commonAttributes, commonAttributes.length + 3);
        attributes[commonAttributes.length] = Attr.of("retry_decision", retry ? "retry" : "stop");
        attributes[commonAttributes.length + 1] = Attr.of("status_code", code.name());
        String errorType = code == StatusCode.CLIENT_INTERNAL_ERROR ? "client_error"
                : code == StatusCode.SUCCESS || code == StatusCode.UNUSED_STATUS
                        ? "session_closed" : code.isTransportError() ? "transport_error" : "ydb_error";
        attributes[commonAttributes.length + 2] = Attr.of("error.type", errorType);
        sessionErrors.add(1, attributes);
    }

    void reportCommitQueued(long messages, String topic) {
        report(commitQueued, messages, topic);
    }

    void reportCommitAcknowledged(long messages, String topic) {
        report(commitAcknowledged, messages, topic);
    }

    void reportDecoded(long compressionNanos, long processingNanos, int codec, String topic, Throwable error) {
        recordDuration(compressionDuration, compressionNanos, codec, topic, error);
        recordDuration(processingDuration, processingNanos, codec, topic, error);
    }

    private void recordDuration(DoubleHistogram histogram, long durationNanos, int codec, String topic,
            Throwable error) {
        if (enabled) {
            Attr[] attributes = Arrays.copyOf(commonAttributes, commonAttributes.length + (error == null ? 3 : 4));
            attributes[commonAttributes.length] = Attr.of("topic", topic);
            YdbTopic.Codec codecType = YdbTopic.Codec.forNumber(codec);
            attributes[commonAttributes.length + 1] = Attr.of("codec", codecType == null ? Integer.toString(codec)
                    : codecType.name().substring("CODEC_".length()).toLowerCase(java.util.Locale.ROOT));
            attributes[commonAttributes.length + 2] = Attr.of("outcome", error == null ? "ok" : "error");
            if (error != null) {
                attributes[commonAttributes.length + 3] = Attr.of("error.type", error.getClass().getName());
            }
            histogram.record(durationNanos / 1_000_000_000d, attributes);
        }
    }

    private void report(LongCounter counter, long messages, String topic) {
        if (enabled && messages > 0) {
            record(counter, messages, Attr.of("topic", topic));
        }
    }

    private void record(LongCounter counter, long value, Attr... extraAttributes) {
        if (value > 0) {
            Attr[] attributes = Arrays.copyOf(commonAttributes, commonAttributes.length + extraAttributes.length);
            System.arraycopy(extraAttributes, 0, attributes, commonAttributes.length, extraAttributes.length);
            counter.add(value, attributes);
        }
    }

    private static Attr[] createCommonAttributes(String consumer, String readerName) {
        boolean hasReaderName = readerName != null && !readerName.isEmpty();
        if (consumer == null) {
            return hasReaderName ? new Attr[]{Attr.of("reader.name", readerName)} : new Attr[0];
        }
        if (!hasReaderName) {
            return new Attr[]{Attr.of("consumer", consumer)};
        }
        return new Attr[]{Attr.of("consumer", consumer), Attr.of("reader.name", readerName)};
    }

}
