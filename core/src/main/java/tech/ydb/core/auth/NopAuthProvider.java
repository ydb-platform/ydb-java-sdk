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
        return EmptyIdentity.INSTANCE;
    }

    private static final class EmptyIdentity implements AuthIdentity {
        private static final EmptyIdentity INSTANCE = new EmptyIdentity();

        @Override
        public String getToken() {
            return "";
        }
    }
}
