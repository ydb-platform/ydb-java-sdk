package tech.ydb.topic.write.impl;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;

/**
 * Topic writer counters.
 */
final class WriterMetrics {
    private static final String MESSAGE_UNIT = "{message}";

    private final LongCounter sendingMessages;
    private final LongCounter sendingBytes;
    private final LongCounter writtenMessages;
    private final Attr[] commonAttributes;
    private final boolean enabled;

    WriterMetrics(Meter meter, String topic, String writerName) {
        this.enabled = meter != Meter.NOOP;
        this.sendingMessages = meter.createCounter("ydb.topic.writer.sending.messages", MESSAGE_UNIT,
                "The number of messages accepted by the SDK for sending.");
        this.sendingBytes = meter.createCounter("ydb.topic.writer.sending.bytes", "By",
                "The uncompressed body size of messages accepted by the writer.");
        this.writtenMessages = meter.createCounter("ydb.topic.writer.written.messages", MESSAGE_UNIT,
                "The number of messages confirmed written by the server, including already written messages.");
        this.commonAttributes = enabled
                ? new Attr[]{Attr.of("topic", topic), Attr.of("writer.name", writerName)}
                : new Attr[0];
    }

    void reportSending(long bytes) {
        if (enabled) {
            sendingMessages.add(1, commonAttributes);
            if (bytes > 0) {
                sendingBytes.add(bytes, commonAttributes);
            }
        }
    }

    void reportWritten() {
        if (enabled) {
            writtenMessages.add(1, commonAttributes);
        }
    }
}
