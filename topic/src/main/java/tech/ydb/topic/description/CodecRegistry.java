package tech.ydb.topic.description;

import java.util.Collection;
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

    private final Map<Integer, Codec> codecMap = new ConcurrentHashMap<>();

    public CodecRegistry() {
        this(StandardCodecs.getAvailableCodecs());
    }

    public CodecRegistry(Collection<Codec> codecs) {
        for (Codec codec: codecs) {
            int id = codec.getId();
            Codec old = codecMap.put(id, codec);
            if (old != null) {
                logger.info("Replace codec which have already associated with this id. CodecId: {} Codec: {}", id, old);
            }
        }
    }

    /**
     * Register codec implementation
     * @param codec codec implementation
     * @return previous implementation with associated codec
     */
    @Deprecated
    public Codec registerCodec(Codec codec) {
        if (codec == null) {
            throw new IllegalArgumentException("Codec must be not null");
        }
        int codecId = codec.getId();

        Codec result = codecMap.put(codecId, codec);
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
        return codecMap.get(codecId);
    }
}
