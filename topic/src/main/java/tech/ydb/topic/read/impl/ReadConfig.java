package tech.ydb.topic.read.impl;

import java.util.concurrent.Executor;
import java.util.function.LongSupplier;

import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.settings.ReaderSettings;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class ReadConfig {
    private final CodecRegistry codecRegistry;
    private final Executor processor;
    private final Executor decompressor;
    private final String consumerName;
    private final long maxMemoryUsageBytes;
    private final int maxBatchSize;
    private final ReaderMetrics metrics;
    final LongSupplier readyMessages;

    public ReadConfig(CodecRegistry codecRegistry, Executor processor, Executor decompressor, ReaderSettings settings) {
        this(codecRegistry, processor, decompressor, settings, () -> 0);
    }

    ReadConfig(CodecRegistry codecRegistry, Executor processor, Executor decompressor, ReaderSettings settings,
            LongSupplier readyMessages) {
        this.codecRegistry = codecRegistry;
        this.processor = processor;
        this.decompressor = decompressor;
        this.consumerName = settings.getConsumerName();
        this.maxMemoryUsageBytes = settings.getMaxMemoryUsageBytes();
        this.maxBatchSize = settings.getMaxBatchSize();
        this.metrics = new ReaderMetrics(settings.getMeter(), settings.getConsumerName(), settings.getReaderName());
        this.readyMessages = readyMessages;
    }

    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    public Executor getDecompressor() {
        return decompressor;
    }

    public Executor getProcessor() {
        return processor;
    }

    public long getMaxMemoryUsageBytes() {
        return maxMemoryUsageBytes;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    ReaderMetrics getMetrics() {
        return metrics;
    }
}
