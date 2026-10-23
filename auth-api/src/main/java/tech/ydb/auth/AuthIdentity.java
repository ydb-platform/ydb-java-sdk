package tech.ydb.auth;

/**
 * Supplies authentication tokens for requests made by the YDB transport layer.
 *
 * <p>An identity may return a fixed token or obtain and refresh tokens as needed.
 * The transport closes the identity when it no longer needs it.
 */
public interface AuthIdentity extends AutoCloseable {

    /**
     * Returns the authentication token to use for a request.
     *
     * <p>Implementations may obtain or refresh the token during this call.
     *
     * @return the authentication token, or {@code null} if no token should be sent
     */
    String getToken();

    /**
     * Releases resources used by this identity.
     *
     * <p>The default implementation does nothing.
     */
    @Override
    default void close() {
    }
}

