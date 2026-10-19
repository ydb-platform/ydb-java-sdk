package tech.ydb.topic.write.impl;

import java.util.Arrays;
import java.util.function.LongSupplier;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;
import tech.ydb.proto.topic.YdbTopic;

/**
 * Topic writer metrics.
 */
final class WriterMetrics {
    private static final String MESSAGE_UNIT = "{message}";

    private final LongCounter sendingMessages;
    private final LongCounter sendingBytes;
    private final LongCounter writtenMessages;
    private final LongCounter sessionErrors;
    private final DoubleHistogram compressionDuration;
    private final DoubleHistogram processingDuration;
    private final Attr[] commonAttributes;
    private final boolean enabled;
    private final Meter meter;
    private MetricRegistration bufferUsedGauge = MetricRegistration.NOOP;
    private MetricRegistration bufferLimitGauge = MetricRegistration.NOOP;

    WriterMetrics(Meter meter, String topic, String writerName) {
        this.meter = meter;
        this.enabled = meter != Meter.NOOP;
        this.sendingMessages = meter.createCounter("ydb.topic.writer.sending.messages", MESSAGE_UNIT,
                "The number of messages accepted by the SDK for sending.");
        this.sendingBytes = meter.createCounter("ydb.topic.writer.sending.bytes", "By",
                "The uncompressed body size of messages accepted by the writer.");
        this.writtenMessages = meter.createCounter("ydb.topic.writer.written.messages", MESSAGE_UNIT,
                "The number of messages confirmed written by the server, including already written messages.");
        this.sessionErrors = meter.createCounter("ydb.topic.writer.session.errors", "{error}",
                "The number of writer stream session errors by retry decision.");
        this.compressionDuration = meter.createHistogram("ydb.topic.writer.compression.duration", "s",
                "The time spent compressing a message, excluding queueing.");
        this.processingDuration = meter.createHistogram("ydb.topic.writer.message.processing.duration", "s",
                "The time from buffer acceptance until a message is ready for sending.");
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

    void reportCompression(long durationNanos, int codec, Throwable error) {
        recordDuration(compressionDuration, durationNanos, codec, error);
    }

    void reportProcessing(long durationNanos, int codec, Throwable error) {
        recordDuration(processingDuration, durationNanos, codec, error);
    }

    private void recordDuration(DoubleHistogram histogram, long durationNanos, int codec, Throwable error) {
        if (enabled) {
            Attr[] attributes = Arrays.copyOf(commonAttributes, commonAttributes.length + (error == null ? 2 : 3));
            YdbTopic.Codec codecType = YdbTopic.Codec.forNumber(codec);
            attributes[commonAttributes.length] = Attr.of("codec", codecType == null ? Integer.toString(codec)
                    : codecType.name().substring("CODEC_".length()).toLowerCase(java.util.Locale.ROOT));
            attributes[commonAttributes.length + 1] = Attr.of("outcome", error == null ? "ok" : "error");
            if (error != null) {
                attributes[commonAttributes.length + 2] = Attr.of("error.type", error.getClass().getName());
            }
            histogram.record(durationNanos / 1_000_000_000d, attributes);
        }
    }

    void reportSessionError(Status status, boolean retry) {
        StatusCode code = status.getCode();
        Attr[] attributes = Arrays.copyOf(commonAttributes, commonAttributes.length + 3);
        attributes[commonAttributes.length] = Attr.of("retry_decision", retry ? "retry" : "stop");
        attributes[commonAttributes.length + 1] = Attr.of("status_code", code.name());
        String errorType = code == StatusCode.SUCCESS || code == StatusCode.UNUSED_STATUS
                ? "session_closed" : code.isTransportError() ? "transport_error" : "ydb_error";
        attributes[commonAttributes.length + 2] = Attr.of("error.type", errorType);
        sessionErrors.add(1, attributes);
    }
}
