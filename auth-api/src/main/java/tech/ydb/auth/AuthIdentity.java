package tech.ydb.auth;

/**
 * Provides an authentication token for every request.
 */
public interface AuthIdentity extends AutoCloseable {

    String getToken();

    @Override
    default void close() {
    }
}

