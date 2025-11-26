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

    public BatchMeta(YdbTopic.StreamReadMessage.ReadResponse.Batch batch) {
        this.producerId = batch.getProducerId();
        this.writeSessionMeta = batch.getWriteSessionMetaMap();
        this.codec = batch.getCodec();
        this.writtenAt = ProtobufUtils.protoToInstant(batch.getWrittenAt());
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
