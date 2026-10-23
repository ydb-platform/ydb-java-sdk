package tech.ydb.auth;

/**
 * Provides authentication identities using a helper supplied by the YDB transport.
 *
 * @param <T> the type of helper supplied by the transport
 */
public interface AuthRpcProvider<T> {
    /**
     * Returns an authentication identity for use by the transport.
     *
     * <p>The helper can be used to perform authentication requests. Implementations may
     * create an identity or reuse an existing one. The transport closes the returned
     * identity when it no longer needs it.
     *
     * @param rpc helper provided by the transport implementation
     * @return an authentication identity, or {@code null} for an anonymous connection
     */
    AuthIdentity createAuthIdentity(T rpc);
}
