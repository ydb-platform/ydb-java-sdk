package tech.ydb.auth;


/**
 * Implementation of AuthProvider for anonymous connections
 */
public class NopAuthProvider implements AuthProvider {
    public static final NopAuthProvider INSTANCE = new NopAuthProvider();

    protected NopAuthProvider() {
    }

    @Override
    public AuthIdentity createAuthIdentity() {
        return null;
    }
}
