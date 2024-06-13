package tech.ydb.auth;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.google.common.io.CharStreams;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class OAuth2Token {
    public static final String ACCESS_TOKEN = "urn:ietf:params:oauth:token-type:access_token";
    public static final String JWT_TOKEN = "urn:ietf:params:oauth:token-type:jwt";

    public static final String REFRESH_TOKEN = "urn:ietf:params:oauth:token-type:refresh_token";
    public static final String ID_TOKEN = "urn:ietf:params:oauth:token-type:id_token";
    public static final String SAML1_TOKEN = "urn:ietf:params:oauth:token-type:saml1";
    public static final String SAML2_TOKEN = "urn:ietf:params:oauth:token-type:saml2";

    private final String type;
    private final int expireInSeconds;

    protected OAuth2Token(String type, int expireInSeconds) {
        this.type = type;
        this.expireInSeconds = expireInSeconds;
    }

    public String getType() {
        return this.type;
    }

    public int getExpireInSeconds() {
        return this.expireInSeconds;
    }

    public abstract String getToken();

    public static OAuth2Token fromValue(String token) {
        return fromValue(token, JWT_TOKEN);
    }

    public static OAuth2Token fromValue(String token, String tokenType) {
        return new OAuth2Token(tokenType, 100 * 365 * 24 * 60 * 60) { // Expire in 100 year ~ never expired
            @Override
            public String getToken() {
                return token;
            }
        };
    }

    public static OAuth2Token fromFile(File tokenFile) {
        return fromFile(tokenFile, JWT_TOKEN);
    }

    public static OAuth2Token fromFile(File tokenFile, String tokenType) {
        return new OAuth2Token(tokenType, 24 * 60 * 60) { // Expire in 1 day
            @Override
            public String getToken() {
                try (FileReader reader = new FileReader(tokenFile)) {
                    return CharStreams.toString(reader).trim();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read token from " + tokenFile, e);
                }
            }
        };
    }
}
