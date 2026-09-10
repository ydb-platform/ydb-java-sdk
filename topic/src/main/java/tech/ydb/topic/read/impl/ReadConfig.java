package tech.ydb.topic.read.impl;

import java.util.concurrent.Executor;

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

    public ReadConfig(CodecRegistry codecRegistry, Executor processor, Executor decompressor, ReaderSettings settings) {
        this.codecRegistry = codecRegistry;
        this.processor = processor;
        this.decompressor = decompressor;
        this.consumerName = settings.getConsumerName();
        this.maxMemoryUsageBytes = settings.getMaxMemoryUsageBytes();
        this.maxBatchSize = settings.getMaxBatchSize();
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
}
