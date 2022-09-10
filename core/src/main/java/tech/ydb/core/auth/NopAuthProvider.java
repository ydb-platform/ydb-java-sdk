package tech.ydb.core.auth;

/**
 * @author Sergey Polovko
 */
public class NopAuthProvider implements AuthProvider {
    public static final NopAuthProvider INSTANCE = new NopAuthProvider();

    private NopAuthProvider() {
    }

    @Override
    public AuthIdentity createAuthIdentity(AuthRpc rpc) {
        return null;
    }
}
