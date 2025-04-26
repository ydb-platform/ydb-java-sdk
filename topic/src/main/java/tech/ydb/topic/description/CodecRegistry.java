package tech.ydb.topic.description;

/**
 * Interface for register custom codec
 *
 * @author Evgeny Kuvardin
 **/
public interface CodecRegistry {

    /**
     * Register codec implementation
     * @param codec codec identifier
     * @param customTopicCodec codec implementation
     * @return previous implementation with associated codec
     */
    CustomTopicCodec registerCustomCodec(int codec, CustomTopicCodec customTopicCodec);

    /**
     * Unregister codec implementation
     * @param codec codec identifier
     * @return previous implementation with associated codec
     */
    CustomTopicCodec unregisterCustomCodec(int codec);

    /**
     * Get codec implementation by associated id
     * @param codec codec identifier
     * @return codec implementation
     */
    CustomTopicCodec getCustomCodec(int codec);

}
