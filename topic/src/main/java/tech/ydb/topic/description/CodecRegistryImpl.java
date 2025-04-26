package tech.ydb.topic.description;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Register for custom topic codec. Local to TopicClient
 *
 * @author Evgeny Kuvardin
 **/
public class CodecRegistryImpl implements CodecRegistry {

    /**
     * Make customCodecMap concurrent since register/unregister/read can be from different threads
     */
    final Map<Integer, CustomTopicCodec> customCodecMap;

    public CodecRegistryImpl() {
        customCodecMap = new ConcurrentHashMap<>();
    }

    @Override
    public CustomTopicCodec registerCustomCodec(int codec, CustomTopicCodec customTopicCodec) {
        assert customTopicCodec != null;

        if (Codec.getInstance().isReserved(codec)) {
            throw new RuntimeException(
                    "Create custom codec for reserved code not allowed: " + codec + " .Use code more than 10000");
        }

        return customCodecMap.put(codec, customTopicCodec);
    }

    @Override
    public CustomTopicCodec unregisterCustomCodec(int codec) {
        if (Codec.getInstance().isReserved(codec)) {
            throw new RuntimeException(
                    "Create custom codec for reserved code not allowed: " + codec + " .Use code more than 10000");
        }

        return customCodecMap.remove(codec);
    }

    @Override
    public CustomTopicCodec getCustomCodec(int codec) {
        return customCodecMap.get(codec);
    }
}
