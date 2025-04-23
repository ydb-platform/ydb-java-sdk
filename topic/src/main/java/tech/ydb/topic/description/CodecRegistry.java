package tech.ydb.topic.description;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Register for custom topic codec. Local to TopicClient
 *
 * @author Evgeny Kuvardin
 **/
public class CodecRegistry {

    /**
     * Make customCodecMap concurrent since getter and setter may be from different threads
     */
    Map<Integer, CustomTopicCodec> customCodecMap = new ConcurrentHashMap<>();

    /**
     * Register codec implementation
     * @param codec codec identifier
     * @param customTopicCodec codec implementation
     * @return previous implementation with associated codec
     */
    public CustomTopicCodec registerCustomCodec(int codec, CustomTopicCodec customTopicCodec) {
        assert customTopicCodec != null;

        if (Codec.getInstance().isReserved(codec)) {
            throw new RuntimeException("Create custom codec for reserved code not allowed: " + codec + " .Use code more than 10000");
        }

        return customCodecMap.put(codec, customTopicCodec);
    }

    /**
     * Unregister codec implementation
     * @param codec codec identifier
     * @return previous implementation with associated codec
     */
    public CustomTopicCodec unregisterCustomCodec(int codec) {
        if (Codec.getInstance().isReserved(codec)) {
            throw new RuntimeException("Create custom codec for reserved code not allowed: " + codec + " .Use code more than 10000");
        }

        return customCodecMap.remove(codec);
    }

    /**
     * Get codec implementation by associated id
     * @param codec codec identifier
     * @return codec implementation
     */
    public CustomTopicCodec getCustomCodec(int codec) {
        return customCodecMap.get(codec);
    }
}
