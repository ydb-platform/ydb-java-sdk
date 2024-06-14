package tech.ydb.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.CharStreams;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class OAuth2TokenSource {
    public static final String ACCESS_TOKEN = "urn:ietf:params:oauth:token-type:access_token";
    public static final String JWT_TOKEN = "urn:ietf:params:oauth:token-type:jwt";

    public static final String REFRESH_TOKEN = "urn:ietf:params:oauth:token-type:refresh_token";
    public static final String ID_TOKEN = "urn:ietf:params:oauth:token-type:id_token";
    public static final String SAML1_TOKEN = "urn:ietf:params:oauth:token-type:saml1";
    public static final String SAML2_TOKEN = "urn:ietf:params:oauth:token-type:saml2";

    private final String type;
    private final int expireInSeconds;

    protected OAuth2TokenSource(String type, int expireInSeconds) {
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

    public static OAuth2TokenSource fromValue(String token) {
        return fromValue(token, JWT_TOKEN);
    }

    public static OAuth2TokenSource fromValue(String token, String tokenType) {
        return new OAuth2TokenSource(tokenType, 100 * 365 * 24 * 60 * 60) { // Expire in 100 year ~ never expired
            @Override
            public String getToken() {
                return token;
            }
        };
    }

    public static OAuth2TokenSource fromFile(File tokenFile) {
        return fromFile(tokenFile, JWT_TOKEN);
    }

    public static OAuth2TokenSource fromFile(File tokenFile, String tokenType) {
        return new OAuth2TokenSource(tokenType, 24 * 60 * 60) { // Expire in 1 day
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

    public static JWTTokenBuilder fromPrivateKeyPemFile(File privateKeyPemFile) {
        try {
            StringBuilder base64 = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(privateKeyPemFile))) {
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    if (line.startsWith("-----")) {
                        continue;
                    }
                    base64.append(line);
                }
            }
            byte[] bytes = Base64.getDecoder().decode(base64.toString());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey key = kf.generatePrivate(new PKCS8EncodedKeySpec(bytes));
            return new JWTTokenBuilder(key);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Unable to read key from " + privateKeyPemFile, e);
        }
    }

    public static JWTTokenBuilder fromKey(Key key) {
        return new JWTTokenBuilder(key);
    }

    public static class JWTTokenBuilder {
        private final Key signingKey;
        private final SignatureAlgorithm alg;

        private Clock clock = Clock.systemUTC();
        private int ttlSeconds = 60 * 60; // 1 hour by default

        private String issuer = null;
        private String subject = null;
        private String audience = null;
        private String id = null;

        private JWTTokenBuilder(Key key) {
            this.signingKey = key;
            this.alg = SignatureAlgorithm.forSigningKey(key);
        }

        @VisibleForTesting
        JWTTokenBuilder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public JWTTokenBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public JWTTokenBuilder withIssuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public JWTTokenBuilder withSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public JWTTokenBuilder withAudience(String audience) {
            this.audience = audience;
            return this;
        }

        public JWTTokenBuilder withTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
            return this;
        }

        public OAuth2TokenSource build() {
            return new OAuth2TokenSource(JWT_TOKEN, ttlSeconds) {
                @Override
                public String getToken() {
                    Instant issuedAt = clock.instant();
                    Instant expiration = issuedAt.plusSeconds(ttlSeconds);

                    JwtBuilder jwt = Jwts.builder();
                    if (issuer != null) {
                        jwt = jwt.setIssuer(issuer);
                    }
                    if (subject != null) {
                        jwt = jwt.setSubject(subject);
                    }
                    if (audience != null) {
                        jwt = jwt.setAudience(audience);
                    }
                    if (id != null) {
                        jwt = jwt.setId(id);
                    }

                    return jwt
                            .setHeaderParam("alg", alg.name())
                            .setHeaderParam("typ", "JWT")
                            .setIssuedAt(Date.from(issuedAt))
                            .setExpiration(Date.from(expiration))
                            .signWith(signingKey, alg)
                            .compact();
                }
            };
        }
    }
}
