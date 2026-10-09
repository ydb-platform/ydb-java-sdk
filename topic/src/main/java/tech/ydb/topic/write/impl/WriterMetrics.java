package tech.ydb.topic.write.impl;

import java.util.function.LongSupplier;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;

/**
 * Topic writer metrics.
 */
final class WriterMetrics {
    private static final String MESSAGE_UNIT = "{message}";

    private final LongCounter sendingMessages;
    private final LongCounter sendingBytes;
    private final LongCounter writtenMessages;
    private final DoubleHistogram messageAckDuration;
    private final Attr[] commonAttributes;
    private final Meter meter;
    private final boolean enabled;
    private MetricRegistration bufferUsedGauge = MetricRegistration.NOOP;
    private MetricRegistration bufferLimitGauge = MetricRegistration.NOOP;
    private MetricRegistration oldestAgeGauge = MetricRegistration.NOOP;

    WriterMetrics(Meter meter, String topic, String writerName) {
        this.meter = meter;
        this.enabled = meter != Meter.NOOP;
        this.sendingMessages = meter.createCounter("ydb.topic.writer.sending.messages", MESSAGE_UNIT,
                "The number of messages accepted by the SDK for sending.");
        this.sendingBytes = meter.createCounter("ydb.topic.writer.sending.bytes", "By",
                "The uncompressed body size of messages accepted by the writer.");
        this.writtenMessages = meter.createCounter("ydb.topic.writer.written.messages", MESSAGE_UNIT,
                "The number of messages confirmed written by the server, including already written messages.");
        this.messageAckDuration = meter.createHistogram("ydb.topic.writer.message.ack.duration", "s",
                "Time from first sending a message to the server to its acknowledgement.");
        this.commonAttributes = writerName == null
                ? new Attr[0]
                : new Attr[]{Attr.of("topic", topic), Attr.of("writer.name", writerName)};
    }

    void register(LongSupplier bufferUsed, LongSupplier bufferLimit) {
        bufferUsedGauge = meter.registerLongGauge("ydb.topic.writer.buffer.used.bytes", "By",
                "The occupied budget of the writer buffer limiter.",
                m -> m.record(bufferUsed.getAsLong(), commonAttributes));
        bufferLimitGauge = meter.registerLongGauge("ydb.topic.writer.buffer.limit.bytes", "By",
                "The configured limit of the writer buffer limiter.",
                m -> m.record(bufferLimit.getAsLong(), commonAttributes));
    }

    void register(LongSupplier oldestTimestamp) {
        oldestAgeGauge = meter.registerDoubleGauge("ydb.topic.writer.sending.oldest_age", "s",
                "The age of the oldest message in the writer's in-flight buffer.", m -> {
                    long timestamp = oldestTimestamp.getAsLong();
                    m.record(timestamp == 0 ? 0 : (System.nanoTime() - timestamp) / 1_000_000_000d,
                            commonAttributes);
                });
    }

    void unregister() {
        bufferUsedGauge.close();
        bufferLimitGauge.close();
        oldestAgeGauge.close();
        bufferUsedGauge = MetricRegistration.NOOP;
        bufferLimitGauge = MetricRegistration.NOOP;
        oldestAgeGauge = MetricRegistration.NOOP;
    }

    long reportMessageSendStart() {
        return enabled ? System.nanoTime() : 0;
    }

    void reportMessageAckDuration(long timestamp) {
        if (timestamp != 0) {
            messageAckDuration.record((System.nanoTime() - timestamp) / 1_000_000_000d, commonAttributes);
        }
    }

    void reportSending(long bytes) {
        sendingMessages.add(1, commonAttributes);
        sendingBytes.add(bytes, commonAttributes);
    }

    void reportWritten() {
        writtenMessages.add(1, commonAttributes);
    }
}
