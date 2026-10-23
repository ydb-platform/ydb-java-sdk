package tech.ydb.auth;


/**
 * Authentication provider for anonymous connections.
 *
 * <p>This provider returns no identity, so the transport sends no authentication token.
 */
public class NopAuthProvider implements AuthProvider {
    /**
     * Shared provider instance for anonymous connections.
     */
    public static final NopAuthProvider INSTANCE = new NopAuthProvider();

    /**
     * Creates a provider for anonymous connections.
     */
    protected NopAuthProvider() {
    }

    /**
     * Returns no identity for an anonymous connection.
     *
     * @return {@code null}
     */
    @Override
    public AuthIdentity createAuthIdentity() {
        return null;
    }
}
