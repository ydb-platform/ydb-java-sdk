package tech.ydb.topic.description;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.topic.impl.StandardCodecs;

/**
 * Register for custom topic codec. Local to TopicClient
 *
 * @author Evgeny Kuvardin
 **/
public class CodecRegistry {

    private static final Logger logger = LoggerFactory.getLogger(CodecRegistry.class);

    // registerCodec may be called at any moment, while getCodec is used by the compression and the
    // decompression threads of every reader and writer created from the same TopicClient
    private final Map<Integer, Codec> customCodecMap = new ConcurrentHashMap<>();

    public CodecRegistry() {
        for (Codec codec: StandardCodecs.getAvailableCodecs()) {
            customCodecMap.put(codec.getId(), codec);
        }
    }

    /**
     * Register codec implementation
     * @param codec codec implementation
     * @return previous implementation with associated codec
     */
    public Codec registerCodec(Codec codec) {
        if (codec == null) {
            throw new IllegalArgumentException("Codec must be not null");
        }
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
