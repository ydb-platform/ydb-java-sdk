package tech.ydb.topic.description;

import java.util.HashMap;
import java.util.Map;

public class CodecRegister {

    Map<Integer, TopicCodec> codecMap = new HashMap<>();

    final static CodecRegister instance = new CodecRegister();

    private CodecRegister() {
        registerInternal(Codec.RAW, new RawCodec());
        registerInternal(Codec.GZIP, new GzipCode());
        registerInternal(Codec.LZOP, new LzopCodec());
        registerInternal(Codec.ZSTD, new ZctdCodec());
    }

    public static CodecRegister getInstance() {
        return instance;
    }

    private void registerInternal(int codec, TopicCodec topicCodec) {
        codecMap.put(codec, topicCodec);
    }

    public TopicCodec registerCodec(int id, TopicCodec topicCodec) {
        if (id <= 10000) {
            throw new RuntimeException("Id must be greater than 10000");
        }
        return codecMap.put(id, topicCodec);

    }

    public TopicCodec unRegisterCodec(int id) {
        if (id <= 10000) {
            throw new RuntimeException("Id must be greater than 10000");
        }

        return codecMap.remove(id);
    }

    public TopicCodec get(int key) {
        return codecMap.get(key);
    }

    public boolean isCustomCodec(int codec) {
        return codec > 10000;
    }
}
