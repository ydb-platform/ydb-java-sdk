package tech.ydb.auth;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
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
import tech.ydb.core.grpc.GrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OAuth2TokenExchangeProvider implements AuthRpcProvider<GrpcTransport> {
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";

    public static final String ACCESS_TOKEN = "urn:ietf:params:oauth:token-type:access_token";
    public static final String JWT_TOKEN = "urn:ietf:params:oauth:token-type:jwt";

    public static final String REFRESH_TOKEN = "urn:ietf:params:oauth:token-type:refresh_token";
    public static final String ID_TOKEN = "urn:ietf:params:oauth:token-type:id_token";
    public static final String SAML1_TOKEN = "urn:ietf:params:oauth:token-type:saml1";
    public static final String SAML2_TOKEN = "urn:ietf:params:oauth:token-type:saml2";

    private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenExchangeProvider.class);
    private static final Gson GSON = new Gson();

    private final Clock clock;
    private final String endpoint;
    private final HttpEntity updateTokenForm;
    private final int timeoutSeconds;

    private OAuth2TokenExchangeProvider(Clock clock, String endpoint, HttpEntity entity, int timeoutSeconds) {
        this.clock = clock;
        this.endpoint = endpoint;
        this.updateTokenForm = entity;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public AuthIdentity createAuthIdentity(GrpcTransport rpc) {
        return new BackgroundIdentity(clock, new OAuth2Rpc(rpc.getScheduler()));
    }

    private class OAuth2Rpc implements BackgroundIdentity.Rpc {
        private final ExecutorService executor;
        private final CloseableHttpClient client;

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
            HttpPost post = new HttpPost(endpoint);
            post.setEntity(updateTokenForm);
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

                if (!"Bearer".equals(json.getTokenType())) {
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

                String token = json.getTokenType() + " " + json.getAccessToken();
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
    }

    public static Builder newBuilder(String endpoint, String jwtToken) {
        return new Builder(endpoint, jwtToken, JWT_TOKEN);
    }

    public static Builder newBuilder(String endpoint, String token, String type) {
        return new Builder(endpoint, token, type);
    }

    public static class Builder {
        private final String endpoint;
        private final String subjectToken;
        private final String subjectType;

        private Clock clock = Clock.systemUTC();
        private int timeoutSeconds = 60;

        private String actorToken = null;
        private String actorType = null;

        private String scope = null;
        private String resource = null;
        private String audience = null;

        private String grantType = GRANT_TYPE;
        private String requestedTokenType = ACCESS_TOKEN;

        private Builder(String endpoint, String subjectToken, String subjectType) {
            this.endpoint = endpoint;
            this.subjectToken = subjectToken;
            this.subjectType = subjectType;
        }

        @VisibleForTesting
        Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder withActor(String token, String type) {
            this.actorToken = token;
            this.actorType = type;
            return this;
        }

        public Builder withScope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder withResource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder withAudience(String audience) {
            this.audience = audience;
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
            return new OAuth2TokenExchangeProvider(clock, endpoint, buildUpdateHttpForm(), timeoutSeconds);
        }

        private HttpEntity buildUpdateHttpForm() {
            List<NameValuePair> params = new ArrayList<>();

            // Required parameters
            params.add(new BasicNameValuePair("grand_type", grantType));
            params.add(new BasicNameValuePair("requested_token_type", requestedTokenType));

            params.add(new BasicNameValuePair("subject_token", subjectToken));
            params.add(new BasicNameValuePair("subject_token_type", subjectType));

            // Optional parameters
            if (resource != null) {
                params.add(new BasicNameValuePair("resource", resource));
            }
            if (audience != null) {
                params.add(new BasicNameValuePair("audience", audience));
            }
            if (scope != null) {
                params.add(new BasicNameValuePair("scope", scope));
            }
            if (actorToken != null) {
                params.add(new BasicNameValuePair("actor_token", actorToken));
            }
            if (actorType != null) {
                params.add(new BasicNameValuePair("actor_token_type", actorType));
            }

            try {
                return new UrlEncodedFormEntity(params);
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("Cannot build OAuth2 http form", ex);
            }
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
}
