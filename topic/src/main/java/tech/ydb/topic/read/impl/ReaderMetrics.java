package tech.ydb.topic.read.impl;

import java.util.Arrays;
import java.util.List;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;
import tech.ydb.topic.description.OffsetsRange;

/**
 * Topic reader counters.
 */
final class ReaderMetrics {
    private static final String MESSAGE_UNIT = "{message}";

    private final LongCounter deliveredMessages;
    private final LongCounter receivedMessages;
    private final LongCounter receivedBytes;
    private final LongCounter commitQueued;
    private final LongCounter commitAcknowledged;
    private final Attr[] commonAttributes;
    private final boolean enabled;

    ReaderMetrics(Meter meter, String consumer, String readerName) {
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
                "The number of messages in commit ranges completed by successful acknowledgements.");
        this.commonAttributes = createCommonAttributes(consumer, readerName);
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

    void reportCommitQueued(List<OffsetsRange> ranges, String topic) {
        if (enabled) {
            long messages = 0;
            for (OffsetsRange range : ranges) {
                messages += range.getEnd() - range.getStart();
            }
            report(commitQueued, messages, topic);
        }
    }

    void reportCommitAcknowledged(long messages, String topic) {
        report(commitAcknowledged, messages, topic);
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
