package tech.ydb.coordination.recipes.util;

/**
 * Exception thrown during serialization/deserialization.
 */
public class SerializationException extends RuntimeException {
    SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
