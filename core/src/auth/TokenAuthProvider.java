package ru.yandex.ydb.core.auth;

/**
 * @author Sergey Polovko
 */
public class TokenAuthProvider implements AuthProvider {

    private final String token;

    public TokenAuthProvider(String token) {
        this.token = token;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public void close() {
    }
}
