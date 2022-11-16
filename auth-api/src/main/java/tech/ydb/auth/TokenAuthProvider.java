package tech.ydb.auth;

/**
 * Implementation of AuthProvider for connections with static token value
 */
public class TokenAuthProvider implements tech.ydb.auth.AuthProvider<Object> {
    private final TokenIdentity identity;

    public TokenAuthProvider(String token) {
        this.identity = new TokenIdentity(token);
    }

    @Override
    public tech.ydb.auth.AuthIdentity createAuthIdentity(Object rpc) {
        return identity;
    }

    private static final class TokenIdentity implements tech.ydb.auth.AuthIdentity {
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
