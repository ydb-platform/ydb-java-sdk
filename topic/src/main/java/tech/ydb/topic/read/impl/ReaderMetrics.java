package tech.ydb.topic.read.impl;

import java.util.Arrays;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;

/**
 * Topic reader delivery metrics.
 */
final class ReaderMetrics {
    private static final String MESSAGE_UNIT = "{message}";

    private final LongCounter deliveredMessages;
    private final Attr[] commonAttributes;

    ReaderMetrics(Meter meter, String consumer, String readerName) {
        this.deliveredMessages = meter.createCounter(
                "ydb.topic.reader.delivered.messages",
                MESSAGE_UNIT,
                "The number of messages delivered by the SDK to application code.");
        this.commonAttributes = createCommonAttributes(consumer, readerName);
    }

    void reportDelivered(long messages, String topic) {
        record(deliveredMessages, messages, topic);
    }

    private void record(LongCounter counter, long value, String topic) {
        if (value > 0) {
            Attr[] attributes = Arrays.copyOf(commonAttributes, commonAttributes.length + 1);
            attributes[commonAttributes.length] = Attr.of("topic", topic);
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
