package tech.ydb.topic.read.impl;

import java.time.Instant;
import java.util.Map;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;

/**
 * @author Nikolay Perfilov
 */
public class BatchMeta {
    private final String producerId;
    private final Map<String, String> writeSessionMeta;
    private final int codec;
    private final Instant writtenAt;
    private final long receivedAt;

    public BatchMeta(YdbTopic.StreamReadMessage.ReadResponse.Batch batch) {
        this(batch, System.nanoTime());
    }

    BatchMeta(YdbTopic.StreamReadMessage.ReadResponse.Batch batch, long receivedAt) {
        this.producerId = batch.getProducerId();
        this.writeSessionMeta = batch.getWriteSessionMetaMap();
        this.codec = batch.getCodec();
        this.writtenAt = ProtobufUtils.protoToInstant(batch.getWrittenAt());
        this.receivedAt = receivedAt;
    }

    long getReceivedAt() {
        return receivedAt;
    }

    public String getProducerId() {
        return producerId;
    }

    public Map<String, String> getWriteSessionMeta() {
        return writeSessionMeta;
    }

    public int getCodec() {
        return codec;
    }

    public Instant getWrittenAt() {
        return writtenAt;
    }
}
