package tech.ydb.topic.read.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;

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
    private final Attr[] commonAttributes;
    private final boolean enabled;
    private final Meter meter;
    private final Map<Source, BooleanSupplier> sources = new ConcurrentHashMap<>();
    private final Map<Long, CommitOffsets> partitions = new ConcurrentHashMap<>();
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
        this.commonAttributes = createCommonAttributes(consumer, readerName);
    }

    void addSource(Source source) {
        addSource(source, () -> true);
    }

    void addSource(Source source, BooleanSupplier active) {
        sources.put(source, active);
    }

    void removeSource(Source source) {
        sources.remove(source);
    }

    void addPartition(long id, long committedOffset) {
        partitions.put(id, new CommitOffsets(committedOffset));
    }

    void removePartition(long id) {
        partitions.remove(id);
    }

    void updateCommittedOffset(long id, long offset) {
        CommitOffsets offsets = partitions.get(id);
        if (offsets != null) {
            offsets.committed.accumulateAndGet(offset, Math::max);
        }
    }

    private long getCommitOffsetLagMax() {
        return partitions.values().stream()
                .mapToLong(p -> Math.max(0, p.requested.get() - p.committed.get())).max().orElse(0);
    }

    private long getLocalBufferMessages() {
        return sources.entrySet().stream().filter(s -> s.getValue().getAsBoolean())
                .mapToLong(s -> s.getKey().getMessagesCount()).sum();
    }

    private long getOldestReceivedTimestamp() {
        long oldest = 0;
        for (Map.Entry<Source, BooleanSupplier> entry : sources.entrySet()) {
            if (entry.getValue().getAsBoolean()) {
                long timestamp = entry.getKey().getOldestReceivedTimestamp();
                if (timestamp != 0 && (oldest == 0 || timestamp < oldest)) {
                    oldest = timestamp;
                }
            }
        }
        return oldest;
    }

    void register(LongSupplier bufferBudget) {
        partitionsGauge = meter.registerLongGauge(
                "ydb.topic.reader.partition_session.count", "{session}",
                "The number of partition sessions currently in the reader session processing lifecycle.",
                m -> m.record(partitions.size(), commonAttributes));
        creditGauge = meter.registerLongGauge("ydb.topic.reader.credit_balance_bytes", "By",
                "The protocol credit granted to the server and not yet consumed by read responses.",
                m -> m.record(bufferBudget.getAsLong(), commonAttributes));
        commitLagGauge = meter.registerLongGauge("ydb.topic.reader.commit_offset.lag.max", MESSAGE_UNIT,
                "The maximum gap between requested and acknowledged commit offsets.",
                m -> m.record(getCommitOffsetLagMax(), commonAttributes));
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
        partitions.clear();
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

    void reportCommitQueued(long partitionId, long offset, long messages, String topic) {
        CommitOffsets offsets = partitions.get(partitionId);
        if (offsets != null) {
            offsets.requested.accumulateAndGet(offset, Math::max);
        }
        report(commitQueued, messages, topic);
    }

    void reportCommitAcknowledged(long partitionId, long offset, String topic) {
        CommitOffsets offsets = partitions.get(partitionId);
        if (offsets != null) {
            long old = offsets.committed.getAndAccumulate(offset, Math::max);
            report(commitAcknowledged, offset > old ? offset - old : 0, topic);
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

    private static class CommitOffsets {
        private final AtomicLong requested;
        private final AtomicLong committed;

        CommitOffsets(long offset) {
            this.requested = new AtomicLong(offset);
            this.committed = new AtomicLong(offset);
        }
    }
}
