package tech.ydb.auth;

/**
 * Provides AuthIdentity instances for use in the operation of the YDB transport layer.
 */
public interface AuthProvider extends AuthRpcProvider<Object> {
    /**
     * Create new instance of AuthIdentity
     * @return new instance of AuthIdentity
     */
    AuthIdentity createAuthIdentity();

    @Override
    default AuthIdentity createAuthIdentity(Object rpc) {
        return createAuthIdentity();
    }
}
