package tech.ydb.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.auth.BackgroundIdentity;
import tech.ydb.core.impl.auth.GrpcAuthRpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OAuth2TokenExchangeProvider implements AuthRpcProvider<GrpcAuthRpc> {
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";

    private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenExchangeProvider.class);
    private static final Gson GSON = new Gson();
    private static final Set<String> SUPPORTED_JWT_ALGS = new HashSet<>(Arrays.asList(
            "HS256", "HS384", "HS512",
            "RS256", "RS384", "RS512",
            "PS256", "PS384", "PS512",
            "ES256", "ES384", "ES512"
    ));

    private final Clock clock;
    private final String endpoint;
    private final String scope;
    private final OAuth2TokenSource subjectTokenSource;
    private final OAuth2TokenSource actorTokenSource;
    private final List<NameValuePair> httpForm;
    private final int timeoutSeconds;

    private OAuth2TokenExchangeProvider(Clock clock, String endpoint, String scope, OAuth2TokenSource subject,
            OAuth2TokenSource actor, List<NameValuePair> form, int timeoutSeconds) {
        this.clock = clock;
        this.endpoint = endpoint;
        this.scope = scope;
        this.subjectTokenSource = subject;
        this.actorTokenSource = actor;
        this.httpForm = form;
        this.timeoutSeconds = timeoutSeconds;
    }

    public static String[] getSupportedJwtAlgorithms() {
        String[] result = new String[SUPPORTED_JWT_ALGS.size()];
        int i = 0;
        for (String supportedJwtAlg : SUPPORTED_JWT_ALGS) {
            result[i++] = supportedJwtAlg;
        }
        Arrays.sort(result);
        return result;
    }

    private static OAuth2TokenSource buildFixedTokenSourceFromConfig(TokenSourceJsonConfig cfg) {
        if (cfg.getToken() == null || cfg.getToken().isEmpty()
                || cfg.getTokenType() == null || cfg.getTokenType().isEmpty()) {
            throw new RuntimeException("Both token and token-type are required");
        }
        return OAuth2TokenSource.fromValue(cfg.getToken(), cfg.getTokenType());
    }

    private static OAuth2TokenSource buildJwtTokenSourceFromConfig(TokenSourceJsonConfig cfg) {
        if (cfg.getAlg() == null || cfg.getAlg().isEmpty()) {
            throw new RuntimeException("Algorithm is required");
        }
        if (cfg.getPrivateKey() == null || cfg.getPrivateKey().isEmpty()) {
            throw new RuntimeException("Key is required");
        }

        String alg = cfg.getAlg().toUpperCase();
        if (!SUPPORTED_JWT_ALGS.contains(alg)) {
            String[] supportedAlgs = getSupportedJwtAlgorithms();
            StringBuilder lstMsg = new StringBuilder();
            for (int i = 0; i < supportedAlgs.length; i++) {
                if (lstMsg.length() > 0) {
                    lstMsg.append(", ");
                }
                lstMsg.append("\"");
                lstMsg.append(supportedAlgs[i]);
                lstMsg.append("\"");
            }
            throw new RuntimeException(
                String.format("Algorithm \"%s\" is not supported. Supported algorithms: %s",
                cfg.getAlg(),
                lstMsg
            ));
        }

        boolean isHmac = "HS256".equals(alg)
            || "HS384".equals(alg)
            || "HS512".equals(alg);
        OAuth2TokenSource.JWTTokenBuilder builder;
        if (isHmac) {
            builder = OAuth2TokenSource.withHmacPrivateKeyBase64(cfg.getPrivateKey(), alg);
        } else {
            builder = OAuth2TokenSource.withPrivateKeyPem(new StringReader(cfg.getPrivateKey()), alg);
        }

        if (cfg.getKeyId() != null) {
            builder.withKeyId(cfg.getKeyId());
        }

        if (cfg.getIssuer() != null) {
            builder.withIssuer(cfg.getIssuer());
        }

        if (cfg.getSubject() != null) {
            builder.withSubject(cfg.getSubject());
        }

        if (cfg.getAudience() != null && cfg.getAudience().length != 0) {
            if (cfg.getAudience().length > 1) {
                throw new RuntimeException("Multiple audience is not supported by current JWT library");
            }
            builder.withAudience(cfg.getAudience()[0]);
        }

        if (cfg.getId() != null) {
            builder.withId(cfg.getId());
        }

        if (cfg.getTtlSeconds() != null) {
            builder.withTtlSeconds(cfg.getTtlSeconds());
        }

        return builder.build();
    }

    private static OAuth2TokenSource buildTokenSourceFromConfig(TokenSourceJsonConfig cfg) {
        if (cfg.getType() == null) {
            throw new RuntimeException("No token source type");
        }
        if ("FIXED".equalsIgnoreCase(cfg.getType())) {
            return buildFixedTokenSourceFromConfig(cfg);
        }
        if ("JWT".equalsIgnoreCase(cfg.getType())) {
            return buildJwtTokenSourceFromConfig(cfg);
        }
        throw new RuntimeException("Unsupported token source type: " + cfg.getType());
    }

    public static Builder fromFile(File configFile) {
        Builder builder = new Builder();
        configFile = expandUserHomeDir(configFile);
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            JsonConfig cfg = GSON.fromJson(br, JsonConfig.class);
            if (cfg == null) {
                throw new RuntimeException("Empty config");
            }

            if (cfg.getTokenEndpoint() != null) {
                builder.withTokenEndpoint(cfg.getTokenEndpoint());
            }

            if (cfg.getGrantType() != null) {
                builder.withCustomGrantType(cfg.getGrantType());
            }

            if (cfg.getResource() != null) {
                for (String res: cfg.getResource()) {
                    builder.withResource(res);
                }
            }

            if (cfg.getAudience() != null) {
                for (String audience: cfg.getAudience()) {
                    builder.withAudience(audience);
                }
            }

            if (cfg.getScope() != null && cfg.getScope().length != 0) {
                builder.withScope(cfg.buildScope());
            }

            if (cfg.getRequestedTokenType() != null) {
                builder.withCustomRequestedTokenType(cfg.getRequestedTokenType());
            }

            if (cfg.getSubject() != null) {
                builder.withSubjectTokenSource(buildTokenSourceFromConfig(cfg.getSubject()));
            }

            if (cfg.getActor() != null) {
                builder.withActorTokenSource(buildTokenSourceFromConfig(cfg.getActor()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read config from " + configFile, e);
        }
        return builder;
    }

    private static File expandUserHomeDir(File file) {
        String path = file.getPath();
        if (path.startsWith("~" + File.separator)) { // "~/"
            path = System.getProperty("user.home") + path.substring(1);
            return new File(path);
        }
        return file;
    }

    @Override
    public AuthIdentity createAuthIdentity(GrpcAuthRpc rpc) {
        return new BackgroundIdentity(clock, new OAuth2Rpc(rpc.getExecutor()));
    }

    private class OAuth2Rpc implements BackgroundIdentity.Rpc {
        private final ExecutorService executor;
        private final CloseableHttpClient client;

        private volatile Instant subjectExpiredAt;
        private volatile String subjectToken;
        private volatile Instant actorExpiredAt;
        private volatile String actorToken;

        OAuth2Rpc(ExecutorService executor) {
            this.executor = executor;
            this.client = HttpClients.createDefault();
        }

        @Override
        public void close() {
            try {
                this.client.close();
            } catch (IOException e) {
                logger.error("io exception on closing of http client", e);
            }
        }

        private Token updateToken() throws IOException {
            if (subjectTokenSource != null) {
                if (subjectExpiredAt == null || clock.instant().isAfter(subjectExpiredAt)) {
                    subjectToken = subjectTokenSource.getToken();
                    subjectExpiredAt = clock.instant().plusSeconds(subjectTokenSource.getExpireInSeconds());
                }
            }
            if (actorTokenSource != null) {
                if (actorExpiredAt == null || clock.instant().isAfter(actorExpiredAt)) {
                    actorToken = actorTokenSource.getToken();
                    actorExpiredAt = clock.instant().plusSeconds(actorTokenSource.getExpireInSeconds());
                }
            }
            HttpPost post = new HttpPost(endpoint);
            post.setEntity(buildHttpForm());
            CloseableHttpResponse response = client.execute(post);

            int httpCode = response.getStatusLine().getStatusCode();
            if (httpCode != 200) {
                StatusCode code;
                switch (httpCode) {
                    case 400:
                        code = StatusCode.BAD_REQUEST;
                        break;
                    case 401:
                    case 403:
                        code = StatusCode.UNAUTHORIZED;
                        break;
                    case 404:
                        code = StatusCode.NOT_FOUND;
                        break;
                    case 500:
                        code = StatusCode.INTERNAL_ERROR;
                        break;
                    case 503:
                    case 504:
                        code = StatusCode.UNAVAILABLE;
                        break;
                    default:
                        code = StatusCode.CLIENT_INTERNAL_ERROR;
                        break;
                }
                String message = "Cannot get OAuth2 token with code " + httpCode
                        + "[" + response.getStatusLine().getReasonPhrase() + "]";
                throw new UnexpectedResultException(message, Status.of(code));
            }

            try (Reader reader = new InputStreamReader(response.getEntity().getContent())) {
                OAuth2Response json = GSON.fromJson(reader, OAuth2Response.class);
                if (json == null) {
                    throw new UnexpectedResultException("Empty OAuth2 response", Status.of(StatusCode.INTERNAL_ERROR));
                }

                if (!"Bearer".equalsIgnoreCase(json.getTokenType())) {
                    throw new UnexpectedResultException(
                            "OAuth2 token exchange: unsupported token type: " + json.getTokenType(),
                            Status.of(StatusCode.INTERNAL_ERROR)
                    );
                }
                if (json.getExpiredIn() == null || json.getExpiredIn() <= 0) {
                    throw new UnexpectedResultException(
                            "OAuth2 token exchange: incorrect expiration time: " + json.getExpiredIn(),
                            Status.of(StatusCode.INTERNAL_ERROR)
                    );
                }
                String jsonScope = json.getScope();
                if (scope != null && jsonScope != null && !scope.equals(jsonScope)) {
                    throw new UnexpectedResultException(
                            "OAuth2 token exchange: different scope. Expected: " + scope + ", but got: " + jsonScope,
                            Status.of(StatusCode.INTERNAL_ERROR)
                    );
                }

                String token = "Bearer " + json.getAccessToken();
                Instant expireAt = clock.instant().plusSeconds(json.getExpiredIn());
                Instant updateAt = clock.instant().plusSeconds(json.getExpiredIn() / 2);
                return new Token(token, expireAt, updateAt);
            }  finally {
                response.close();
            }
        }

        @Override
        public CompletableFuture<Token> getTokenAsync() {
            final CompletableFuture<Token> future = new CompletableFuture<>();
            executor.submit(() -> {
                try {
                    future.complete(updateToken());
                } catch (IOException | RuntimeException ex) {
                    future.completeExceptionally(ex);
                }
            });
            return future;
        }

        @Override
        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        private HttpEntity buildHttpForm() {
            List<NameValuePair> params = new ArrayList<>();
            params.addAll(httpForm);
            if (subjectToken != null) {
                params.add(new BasicNameValuePair("subject_token", subjectToken));
                params.add(new BasicNameValuePair("subject_token_type", subjectTokenSource.getType()));
            }

            if (actorToken != null) {
                params.add(new BasicNameValuePair("actor_token", actorToken));
                params.add(new BasicNameValuePair("actor_token_type", actorTokenSource.getType()));
            }

            try {
                return new UrlEncodedFormEntity(params);
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("Cannot build OAuth2 http form", ex);
            }
        }
    }

    public static Builder newBuilder(String endpoint, OAuth2TokenSource token) {
        return new Builder(endpoint, token);
    }

    public static class Builder {
        private String endpoint = null;
        private OAuth2TokenSource subject = null;

        private Clock clock = Clock.systemUTC();
        private int timeoutSeconds = 60;

        private OAuth2TokenSource actor = null;

        private String scope = null;
        private final List<String> resourceList = new ArrayList<>();
        private final List<String> audienceList = new ArrayList<>();

        private String grantType = GRANT_TYPE;
        private String requestedTokenType = OAuth2TokenSource.ACCESS_TOKEN;

        private Builder() {
        }

        private Builder(String endpoint, OAuth2TokenSource tokenSource) {
            this.endpoint = endpoint;
            this.subject = tokenSource;
        }

        @VisibleForTesting
        Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder withTokenEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder withSubjectTokenSource(OAuth2TokenSource subjectTokenSouce) {
            this.subject = subjectTokenSouce;
            return this;
        }

        public Builder withActorTokenSource(OAuth2TokenSource actorTokenSouce) {
            this.actor = actorTokenSouce;
            return this;
        }

        public Builder withScope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder withResource(String resource) {
            this.resourceList.add(resource);
            return this;
        }

        public Builder withAudience(String audience) {
            this.audienceList.add(audience);
            return this;
        }

        public Builder withTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder withCustomGrantType(String grantType) {
            this.grantType = grantType;
            return this;
        }

        public Builder withCustomRequestedTokenType(String tokenType) {
            this.requestedTokenType = tokenType;
            return this;
        }

        public OAuth2TokenExchangeProvider build() {
            return new OAuth2TokenExchangeProvider(clock, endpoint, scope, subject, actor, fixedArgs(), timeoutSeconds);
        }

        private List<NameValuePair> fixedArgs() {
            List<NameValuePair> params = new ArrayList<>();

            // Required parameters
            params.add(new BasicNameValuePair("grant_type", grantType));
            params.add(new BasicNameValuePair("requested_token_type", requestedTokenType));

            // Optional parameters
            for (String resource: resourceList) {
                params.add(new BasicNameValuePair("resource", resource));
            }
            for (String audience: audienceList) {
                params.add(new BasicNameValuePair("audience", audience));
            }
            if (scope != null) {
                params.add(new BasicNameValuePair("scope", scope));
            }

            return params;
        }
    }

    private static class OAuth2Response {
        @SerializedName("access_token")
        private String accessToken;
        @SerializedName("issued_token_type")
        private String issuedTokenType;
        @SerializedName("token_type")
        private String tokenType;
        @SerializedName("expires_in")
        private Long expiredIn;
        @SerializedName("scope")
        private String scope;
        @SerializedName("refresh_token")
        private String refreshToken;

        public String getAccessToken() {
            return this.accessToken;
        }

        public String getIssuedTokenType() {
            return this.issuedTokenType;
        }

        public String getTokenType() {
            return this.tokenType;
        }

        public Long getExpiredIn() {
            return this.expiredIn;
        }

        public String getScope() {
            return this.scope;
        }

        public String getRefreshToken() {
            return this.refreshToken;
        }
    }

    private static class SingleStringOrArrayOfStringsJsonConfigDeserializer implements JsonDeserializer<String[]> {
        public String[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json.isJsonArray()) {
                JsonArray arr = json.getAsJsonArray();
                if (arr.isEmpty()) {
                    return null;
                }
                String[] result = new String[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    result[i] = arr.get(i).getAsJsonPrimitive().getAsString();
                    if (result[i].isEmpty()) {
                        throw new RuntimeException("Cannot parse config from json: empty string");
                    }
                }
                return result;
            }
            if (json.isJsonPrimitive()) {
                String[] result = new String[1];
                result[0] = json.getAsJsonPrimitive().getAsString();
                if (result[0].isEmpty()) {
                    throw new RuntimeException("Cannot parse config from json: empty string");
                }
                return result;
            }
            throw new RuntimeException(
                "Cannot parse config from json: field is expected to be string or array of nonempty strings");
        }
    }

    private static class DurationJsonConfigDeserializer implements JsonDeserializer<Integer> {
        private Integer deserializeWithMultiplier(String value, double multiplier) {
            double parsed = Double.parseDouble(value);
            if (parsed < 0.0) {
                throw new RuntimeException(
                    String.format("Cannot parse ttl from json, negative duration is not allowed: \"%s\"", value));
            }
            return (int) (parsed * multiplier);
        }

        public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            String value = json.getAsJsonPrimitive().getAsString();
            if (value.endsWith("s")) {
                return deserializeWithMultiplier(value.substring(0, value.length() - 1), 1.0);
            }
            if (value.endsWith("m")) {
                return deserializeWithMultiplier(value.substring(0, value.length() - 1), 60.0);
            }
            if (value.endsWith("h")) {
                return deserializeWithMultiplier(value.substring(0, value.length() - 1), 3600.0);
            }
            if (value.endsWith("d")) {
                return deserializeWithMultiplier(value.substring(0, value.length() - 1), 3600.0 * 24.0);
            }
            if (value.endsWith("ms")) {
                return deserializeWithMultiplier(value.substring(0, value.length() - 2), 1.0 / 1000.0);
            }
            if (value.endsWith("us")) {
                return deserializeWithMultiplier(value.substring(0, value.length() - 2), 1.0 / 1000000.0);
            }
            if (value.endsWith("ns")) {
                return deserializeWithMultiplier(value.substring(0, value.length() - 2), 1.0 / 1000000000.0);
            }
            throw new RuntimeException(
                String.format("Cannot parse ttl from json: \"%s\"", value));
        }
    }

    private static class TokenSourceJsonConfig {
        @SerializedName("type")
        private String type = null;

        // Fixed
        @SerializedName("token")
        private String token = null;
        @SerializedName("token-type")
        private String tokenType = null;

        // Jwt
        @SerializedName("alg")
        private String alg = null;
        @SerializedName("private-key")
        private String privateKey = null;
        @SerializedName("kid")
        private String keyId = null;
        @SerializedName("iss")
        private String issuer = null;
        @SerializedName("sub")
        private String subject = null;
        @SerializedName("aud")
        @JsonAdapter(SingleStringOrArrayOfStringsJsonConfigDeserializer.class)
        private String[] audience = null;
        @SerializedName("jti")
        private String id = null;
        @SerializedName("ttl")
        @JsonAdapter(DurationJsonConfigDeserializer.class)
        private Integer ttlSeconds = null;

        public String getType() {
            return type;
        }

        public String getToken() {
            return token;
        }

        public String getTokenType() {
            return tokenType;
        }

        public String getAlg() {
            return alg;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public String getKeyId() {
            return keyId;
        }

        public String getIssuer() {
            return issuer;
        }

        public String getSubject() {
            return subject;
        }

        public String[] getAudience() {
            return audience;
        }

        public String getId() {
            return id;
        }

        public Integer getTtlSeconds() {
            return ttlSeconds;
        }
    }

    private static class JsonConfig {
        @SerializedName("token-endpoint")
        private String tokenEndpoint = null;
        @SerializedName("grant-type")
        private String grantType = null;
        @SerializedName("res")
        @JsonAdapter(SingleStringOrArrayOfStringsJsonConfigDeserializer.class)
        private String[] resource = null;
        @SerializedName("aud")
        @JsonAdapter(SingleStringOrArrayOfStringsJsonConfigDeserializer.class)
        private String[] audience = null;
        @SerializedName("scope")
        @JsonAdapter(SingleStringOrArrayOfStringsJsonConfigDeserializer.class)
        private String[] scope = null;
        @SerializedName("requested-token-type")
        private String requestedTokenType = null;
        @SerializedName("subject-credentials")
        private TokenSourceJsonConfig subject = null;
        @SerializedName("actor-credentials")
        private TokenSourceJsonConfig actor = null;

        public String getTokenEndpoint() {
            return this.tokenEndpoint;
        }

        public String getGrantType() {
            return this.grantType;
        }

        public String[] getResource() {
            return this.resource;
        }

        public String[] getAudience() {
            return this.audience;
        }

        public String[] getScope() {
            return this.scope;
        }

        public String buildScope() {
            StringBuilder result = new StringBuilder();
            for (String s: this.scope) {
                if (result.length() != 0) {
                    result.append(" ");
                }
                result.append(s);
            }
            return result.toString();
        }

        public String getRequestedTokenType() {
            return this.requestedTokenType;
        }

        public TokenSourceJsonConfig getSubject() {
            return subject;
        }

        public TokenSourceJsonConfig getActor() {
            return actor;
        }
    }
}
