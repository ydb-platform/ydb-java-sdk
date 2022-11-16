package tech.ydb.auth;


/**
 * Implementation of AuthProvider for anonymous connections
 */
public class NopAuthProvider implements tech.ydb.auth.AuthProvider<Object> {
    public static final NopAuthProvider INSTANCE = new NopAuthProvider();

    private NopAuthProvider() {
    }

    @Override
    public tech.ydb.auth.AuthIdentity createAuthIdentity(Object rpc) {
        return null;
    }
}
