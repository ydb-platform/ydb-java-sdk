package tech.ydb.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.Key;
import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.spec.SecretKeySpec;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.CharStreams;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.MacAlgorithm;
import io.jsonwebtoken.security.SignatureAlgorithm;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

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

    private static SecretKeySpec readSecretKeyBase64(String data, MacAlgorithm algorithm) {
        String jcpName = "HmacSHA" + algorithm.getKeyBitLength();
        byte[] key = Base64.getDecoder().decode(data);
        return new SecretKeySpec(key, jcpName);
    }

    private static PrivateKey readPrivateKeyPemFile(Reader data) {
        try (PEMParser parser = new PEMParser(new BufferedReader(data))) {
            Object parsed = parser.readObject();
            if (parsed == null) {
                throw new RuntimeException("Failed to parse PEM key");
            }

            PrivateKeyInfo info;
            if (parsed instanceof PrivateKeyInfo) {
                info = (PrivateKeyInfo) parsed;
            } else if (parsed instanceof PEMKeyPair) {
                PEMKeyPair keyPair = (PEMKeyPair) parsed;
                info = keyPair.getPrivateKeyInfo();
            } else {
                throw new RuntimeException("Unknown key PEM format");
            }

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            return converter.getPrivateKey(info);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to read key: %s", e.getMessage()), e);
        }
    }

    public static JWTTokenBuilder withHmacPrivateKeyBase64(String data, MacAlgorithm algorithm) {
        SecretKeySpec key = readSecretKeyBase64(data, algorithm);
        return new JWTTokenBuilder(jwt -> jwt.signWith(key, algorithm));
    }

    public static JWTTokenBuilder withPrivateKeyPem(Reader data, SignatureAlgorithm algorithm) {
        PrivateKey key = readPrivateKeyPemFile(data);
        return new JWTTokenBuilder(jwt -> jwt.signWith(key, algorithm));
    }

    public static JWTTokenBuilder withPrivateKeyPem(Reader data) {
        PrivateKey key = readPrivateKeyPemFile(data);
        return new JWTTokenBuilder(jwt -> jwt.signWith(key));
    }

    public static JWTTokenBuilder fromKey(Key key) {
        return new JWTTokenBuilder(jwt -> jwt.signWith(key));
    }

    public static class JWTTokenBuilder {
        private final Function<JwtBuilder, JwtBuilder> signingFunc;
        private Clock clock = Clock.systemUTC();
        private int ttlSeconds = 60 * 60; // 1 hour by default

        private String issuer = null;
        private String subject = null;
        private String audience = null;
        private String id = null;
        private String keyId = null;

        private final Map<String, Object> claims = new HashMap<>();


        private JWTTokenBuilder(Function<JwtBuilder, JwtBuilder> signingFunc) {
            this.signingFunc = signingFunc;
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

        public JWTTokenBuilder withKeyId(String keyId) {
            this.keyId = keyId;
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

        public JWTTokenBuilder withClaim(String key, String value) {
            this.claims.put(key, value);
            return this;
        }

        public OAuth2TokenSource build() {
            return new OAuth2TokenSource(JWT_TOKEN, ttlSeconds) {
                @Override
                public String getToken() {
                    Instant issuedAt = clock.instant();
                    Instant expiration = issuedAt.plusSeconds(ttlSeconds);

                    JwtBuilder jwt = Jwts.builder().claims(claims);

                    if (issuer != null) {
                        jwt = jwt.issuer(issuer);
                    }
                    if (subject != null) {
                        jwt = jwt.subject(subject);
                    }
                    if (audience != null) {
                        jwt = jwt.audience().add(audience).and();
                    }
                    if (id != null) {
                        jwt = jwt.id(id);
                    }

                    if (keyId != null) {
                        jwt = jwt.header().keyId(keyId).and();
                    }

                    // the "alg" header is written by the library itself on signing
                    jwt = jwt.header().type("JWT").and()
                            .issuedAt(Date.from(issuedAt))
                            .expiration(Date.from(expiration));

                    return signingFunc.apply(jwt).compact();
                }
            };
        }
    }
}
