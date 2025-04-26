package tech.ydb.topic.impl;

import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.CustomTopicCodec;

/**
 * UnModifiable registry
 *  @author Evgeny Kuvardin
 */
public class UnModifiableRegistry implements CodecRegistry {
    private static final UnModifiableRegistry INSTANCE = new UnModifiableRegistry();

    private UnModifiableRegistry() {
    }

    @Override
    public CustomTopicCodec registerCustomCodec(int codec, CustomTopicCodec customTopicCodec) {
        throw new RuntimeException("Couldn't modify registry. Use another implementation");
    }

    @Override
    public CustomTopicCodec unregisterCustomCodec(int codec) {
        throw new RuntimeException("Couldn't modify registry. Use another implementation");
    }

    @Override
    public CustomTopicCodec getCustomCodec(int codec) {
        return null;
    }

    public static UnModifiableRegistry getInstance() {
        return INSTANCE;
    }
}
