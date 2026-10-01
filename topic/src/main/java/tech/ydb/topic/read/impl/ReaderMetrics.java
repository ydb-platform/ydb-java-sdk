package tech.ydb.topic.read.impl;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;

/**
 * Topic reader metrics.
 */
final class ReaderMetrics {
    private static final String MESSAGE_UNIT = "{message}";

    private final LongCounter deliveredMessages;
    private final LongCounter receivedMessages;
    private final LongCounter receivedBytes;
    private final Attr[] commonAttributes;
    private final boolean enabled;

    ReaderMetrics(Meter meter, String consumer, String readerName) {
        this.enabled = meter != Meter.NOOP;
        this.deliveredMessages = meter.createCounter(
                "ydb.topic.reader.delivered.messages",
                MESSAGE_UNIT,
                "The number of messages delivered by the SDK to application code.");
        this.receivedMessages = meter.createCounter("ydb.topic.reader.received.messages", MESSAGE_UNIT,
                "Messages accepted by the SDK for active partition sessions.");
        this.receivedBytes = meter.createCounter("ydb.topic.reader.received.bytes", "By",
                "Bytes in received ReadResponse messages.");
        this.commonAttributes = createCommonAttributes(consumer, readerName);
    }

    void registerGauges(Meter meter, ReaderImpl reader) {
        if (!enabled) {
            return;
        }
        WeakReference<ReaderImpl> source = new WeakReference<>(reader);
        meter.createLongGauge("ydb.topic.reader.partition_session.count", "{session}",
                "The number of partition sessions currently in the reader session processing lifecycle.", m -> {
                    ReaderImpl current = source.get();
                    if (current != null && !current.isClosed()) {
                        m.record(current.getPartitionSessionCount(), commonAttributes);
                    }
                });
        meter.createLongGauge("ydb.topic.reader.credit_balance_bytes", "By",
                "The protocol credit granted to the server and not yet consumed by read responses.", m -> {
                    ReaderImpl current = source.get();
                    if (current != null && !current.isClosed()) {
                        m.record(current.getCreditBalanceBytes(), commonAttributes);
                    }
                });
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
