package tech.ydb.core.auth;

/**
 * @author Sergey Polovko
 */
public class TokenAuthProvider implements AuthProvider {
    private final TokenIdentity identity;

    public TokenAuthProvider(String token) {
        this.identity = new TokenIdentity(token);
    }

    @Override
    public AuthIdentity createAuthIdentity(AuthRpc rpc) {
        return identity;
    }

    private static final class TokenIdentity implements AuthIdentity {
        private final String token;
        
        private TokenIdentity(String token) {
            this.token = token;
        }

        @Override
        public String getToken() {
            return token;
        }
    }
}
