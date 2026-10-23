package tech.ydb.auth;

/**
 * Provides authentication identities without requiring a transport rpc.
 */
public interface AuthProvider extends AuthRpcProvider<Object> {
    /**
     * Returns an authentication identity for use by the transport.
     *
     * <p>Implementations may create an identity or reuse an existing one. The transport
     * closes the returned identity when it no longer needs it.
     *
     * @return an authentication identity, or {@code null} for an anonymous connection
     */
    AuthIdentity createAuthIdentity();

    /**
     * Returns an authentication identity by delegating to {@link #createAuthIdentity()}.
     *
     * @param rpc transport-provided helper; ignored by this implementation
     * @return an authentication identity, or {@code null} for an anonymous connection
     */
    @Override
    default AuthIdentity createAuthIdentity(Object rpc) {
        return createAuthIdentity();
    }
}
