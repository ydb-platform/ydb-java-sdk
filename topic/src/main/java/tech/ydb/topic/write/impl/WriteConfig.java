package tech.ydb.topic.write.impl;

import java.util.concurrent.Executor;

import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.settings.WriterSettings;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class WriteConfig {
    private final long bufferMaxSize;
    private final int bufferMaxMessagesCount;
    private final Codec codec;
    private final Executor compressor;
    private final WriterMetrics metrics;

    public WriteConfig(CodecRegistry codecRegistry, Executor compressor, WriterSettings settings) {
        this.bufferMaxSize = settings.getMaxSendBufferMemorySize();
        this.bufferMaxMessagesCount = settings.getMaxSendBufferMessagesCount();
        this.compressor = compressor;
        this.metrics = new WriterMetrics(settings.getMeter(), settings.getTopicPath(), settings.getWriterName());
        this.codec = codecRegistry.getCodec(settings.getCodec());
        if (codec == null) {
            throw new IllegalArgumentException("Unsupported codec: " + settings.getCodec());
        }
    }

    public long getBufferMaxSize() {
        return bufferMaxSize;
    }

    public int getBufferMaxMessagesCount() {
        return bufferMaxMessagesCount;
    }

    public Executor getCompressor() {
        return compressor;
    }

    public Codec getCodec() {
        return codec;
    }

    WriterMetrics getMetrics() {
        return metrics;
    }
}
