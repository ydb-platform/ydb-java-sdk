package tech.ydb.auth;

/**
 * Provides AuthIdentity instances for use in the operation of the YDB transport layer.
 *
 * @param <T> Optional type specification for a transport-provided helper.
 */
public interface AuthProvider<T> {
    /**
     * Create new instance of AuthIdentity
     * @param rpc helper provided by a transport implementation
     * @return new instance of AuthIdentity
     */
    AuthIdentity createAuthIdentity(T rpc);
}
