package tech.ydb.topic.read.impl;

import java.util.concurrent.Executor;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.settings.ReaderSettings;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class ReadConfig {
    private final CodecRegistry codecRegistry;
    private final Executor control;
    private final Executor processor;
    private final Executor decompressor;
    private final String consumerName;
    private final long maxMemoryUsageBytes;
    private final int maxBatchSize;
    private final ReaderMetrics metrics;
    final LongSupplier readyMessages;
    final DoubleSupplier readyMessageAgeMax;

    public ReadConfig(CodecRegistry codecRegistry, Executor control, Executor processor, Executor decompressor,
            ReaderSettings settings) {
        this(codecRegistry, control, processor, decompressor, settings, () -> 0);
    }

    ReadConfig(CodecRegistry codecRegistry, Executor control, Executor processor, Executor decompressor,
            ReaderSettings settings, LongSupplier readyMessages) {
        this(codecRegistry, control, processor, decompressor, settings, readyMessages, () -> 0);
    }

    public ReadConfig(CodecRegistry codecRegistry, Executor control, Executor processor, Executor decompressor,
            ReaderSettings settings, LongSupplier readyMessages, DoubleSupplier readyMessageAgeMax) {
        this.codecRegistry = codecRegistry;
        this.control = control;
        this.processor = processor;
        this.decompressor = decompressor;
        this.consumerName = settings.getConsumerName();
        this.maxMemoryUsageBytes = settings.getMaxMemoryUsageBytes();
        this.maxBatchSize = settings.getMaxBatchSize();
        this.metrics = new ReaderMetrics(settings.getMeter(), settings.getConsumerName(), settings.getReaderName());
        this.readyMessages = readyMessages;
        this.readyMessageAgeMax = readyMessageAgeMax;
    }

    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    public Executor getDecompressor() {
        return decompressor;
    }

    public Executor getDataExecutor() {
        return processor;
    }

    public Executor getControlExecutor() {
        return control;
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
