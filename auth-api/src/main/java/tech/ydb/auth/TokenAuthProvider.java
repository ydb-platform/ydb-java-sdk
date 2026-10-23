package tech.ydb.auth;

/**
 * Authentication provider that uses a fixed token value.
 *
 * <p>The token is returned unchanged and is never refreshed by this provider.
 */
public class TokenAuthProvider implements AuthProvider {
    private final TokenIdentity identity;

    /**
     * Creates a provider that uses the supplied token for every request.
     *
     * @param token the authentication token, or {@code null} to send no token
     */
    public TokenAuthProvider(String token) {
        this.identity = new TokenIdentity(token);
    }

    /**
     * Returns the identity holding this provider's fixed token.
     *
     * <p>Every call returns the same identity. Closing it has no effect.
     *
     * @return the shared identity holding the token supplied to the constructor
     */
    @Override
    public AuthIdentity createAuthIdentity() {
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
