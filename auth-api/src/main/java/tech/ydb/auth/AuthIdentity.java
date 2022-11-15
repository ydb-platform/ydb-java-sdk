package tech.ydb.auth;

/**
 * @author Sergey Polovko
 */
public interface AuthIdentity extends AutoCloseable {

    String getToken();

    @Override
    default void close() {
    }
}

