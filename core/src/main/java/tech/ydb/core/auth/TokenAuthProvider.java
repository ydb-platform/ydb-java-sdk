package tech.ydb.core.auth;

/**
 * @deprecated
 * Use {@link tech.ydb.auth.TokenAuthProvider} instead.
 */
@Deprecated
public class TokenAuthProvider extends tech.ydb.auth.TokenAuthProvider {
    public TokenAuthProvider(String token) {
        super(token);
    }
}
