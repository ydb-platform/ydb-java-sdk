package tech.ydb.topic.description;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.impl.GzipCodec;
import tech.ydb.topic.impl.LzopCodec;
import tech.ydb.topic.impl.RawCodec;
import tech.ydb.topic.impl.ZstdCodec;

/**
 * Register for custom topic codec. Local to TopicClient
 *
 * @author Evgeny Kuvardin
 **/
public class CodecRegistry {

    private static final Logger logger = LoggerFactory.getLogger(CodecRegistry.class);

    final Map<Integer, Codec> customCodecMap;

    public CodecRegistry() {
        customCodecMap = new HashMap<>();
        customCodecMap.put(Codec.RAW, RawCodec.getInstance());
        customCodecMap.put(Codec.GZIP, GzipCodec.getInstance());
        customCodecMap.put(Codec.LZOP, LzopCodec.getInstance());
        customCodecMap.put(Codec.ZSTD, ZstdCodec.getInstance());
    }

    /**
     * Register codec implementation
     * @param codec codec implementation
     * @return previous implementation with associated codec
     */
    public Codec registerCodec(Codec codec) {
        assert codec != null;
        int codecId = codec.getId();

        Codec result = customCodecMap.put(codecId, codec);

        if (result != null) {
            logger.info(
                    "Replace codec which have already associated with this id. CodecId: {} Codec: {}",
                    codecId,
                    result);
        }

        return result;
    }

    /**
     * Get codec implementation by associated id
     * @param codecId codec identifier
     * @return codec implementation
     */
    public Codec getCodec(int codecId) {
        return customCodecMap.get(codecId);
    }

}
