package tech.ydb.topic.description;

import java.util.HashMap;
import java.util.Map;

public class CodecRegistry {

    Map<Integer, CustomTopicCodec> customCodecMap = new HashMap<>();

    /**
     * @param i
     * @param codec
     */
    public CustomTopicCodec registerCustomCodec(int i, CustomTopicCodec codec) {
        assert codec != null;

        if (i <= 10000) {
            throw new RuntimeException("Create custom codec for reserved code not allowed: " + i + " .Use code more than 10000");
        }

        return customCodecMap.put(i, codec);
    }

    /**
     * @param codec
     * @return
     */
    public CustomTopicCodec getCustomCodec(int codec) {
        return customCodecMap.get(codec);
    }
}
