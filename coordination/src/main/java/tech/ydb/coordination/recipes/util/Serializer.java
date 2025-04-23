package tech.ydb.coordination.recipes.util;

/**
 * Serializes and deserializes objects of type T.
 *
 * @param <T> the type of object to be serialized/deserialized
 */
public interface Serializer<T extends ByteSerializable> {
    /**
     * Serializes an object to a byte array.
     */
    byte[] serialize(T obj) throws SerializationException;

    /**
     * Deserializes a byte array to an object.
     */
    T deserialize(byte[] data) throws SerializationException;
}
