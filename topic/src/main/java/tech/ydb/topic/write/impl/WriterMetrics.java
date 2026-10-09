package tech.ydb.topic.write.impl;

import java.util.function.LongSupplier;

import tech.ydb.core.Status;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;
import tech.ydb.topic.impl.TopicMetricsUtils;

/**
 * Topic writer metrics.
 */
final class WriterMetrics {
    private static final String MESSAGE_UNIT = "{message}";

    private final LongCounter sendingMessages;
    private final LongCounter sendingBytes;
    private final LongCounter writtenMessages;
    private final LongCounter sessionErrors;
    private final Attr[] commonAttributes;
    private final Meter meter;
    private MetricRegistration bufferUsedGauge = MetricRegistration.NOOP;
    private MetricRegistration bufferLimitGauge = MetricRegistration.NOOP;

    WriterMetrics(Meter meter, String topic, String writerName) {
        this.meter = meter;
        this.sendingMessages = meter.createCounter("ydb.topic.writer.sending.messages", MESSAGE_UNIT,
                "The number of messages accepted by the SDK for sending.");
        this.sendingBytes = meter.createCounter("ydb.topic.writer.sending.bytes", "By",
                "The uncompressed body size of messages accepted by the writer.");
        this.writtenMessages = meter.createCounter("ydb.topic.writer.written.messages", MESSAGE_UNIT,
                "The number of messages confirmed written by the server, including already written messages.");
        this.sessionErrors = meter.createCounter("ydb.topic.writer.session.errors", "{error}",
                "The number of writer stream session errors by retry decision.");
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

    void unregister() {
        bufferUsedGauge.close();
        bufferLimitGauge.close();
        bufferUsedGauge = MetricRegistration.NOOP;
        bufferLimitGauge = MetricRegistration.NOOP;
    }

    void reportSending(long bytes) {
        sendingMessages.add(1, commonAttributes);
        sendingBytes.add(bytes, commonAttributes);
    }

    void reportWritten() {
        writtenMessages.add(1, commonAttributes);
    }

    void reportSessionError(Status status, boolean retry) {
        sessionErrors.add(1, TopicMetricsUtils.sessionErrorAttributes(commonAttributes, status.getCode(), retry));
    }
}
