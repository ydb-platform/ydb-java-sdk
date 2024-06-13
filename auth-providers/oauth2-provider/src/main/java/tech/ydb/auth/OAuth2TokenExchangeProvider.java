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
import tech.ydb.core.impl.auth.GrpcAuthRpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OAuth2TokenExchangeProvider implements AuthRpcProvider<GrpcAuthRpc> {
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";

    private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenExchangeProvider.class);
    private static final Gson GSON = new Gson();

    private final Clock clock;
    private final String endpoint;
    private final OAuth2Token oauthToken;
    private final List<NameValuePair> httpForm;
    private final int timeoutSeconds;

    private OAuth2TokenExchangeProvider(
            Clock clock, String endpoint, OAuth2Token token, List<NameValuePair> form, int timeoutSeconds
    ) {
        this.clock = clock;
        this.endpoint = endpoint;
        this.oauthToken = token;
        this.httpForm = form;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public AuthIdentity createAuthIdentity(GrpcAuthRpc rpc) {
        return new BackgroundIdentity(clock, new OAuth2Rpc(rpc.getExecutor()));
    }

    private class OAuth2Rpc implements BackgroundIdentity.Rpc {
        private final ExecutorService executor;
        private final CloseableHttpClient client;

        private volatile Instant expiredAt;
        private volatile String tokenValue;

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
            if (expiredAt == null || clock.instant().isAfter(expiredAt)) {
                tokenValue = oauthToken.getToken();
                expiredAt = clock.instant().plusSeconds(oauthToken.getExpireInSeconds());
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

        private HttpEntity buildHttpForm() {
            List<NameValuePair> params = new ArrayList<>();
            params.addAll(httpForm);
            params.add(new BasicNameValuePair("subject_token", tokenValue));
            params.add(new BasicNameValuePair("subject_token_type", oauthToken.getType()));

            try {
                return new UrlEncodedFormEntity(params);
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("Cannot build OAuth2 http form", ex);
            }
        }
    }

    public static Builder newBuilder(String endpoint, OAuth2Token token) {
        return new Builder(endpoint, token);
    }

    public static class Builder {
        private final String endpoint;
        private final OAuth2Token token;

        private Clock clock = Clock.systemUTC();
        private int timeoutSeconds = 60;

        private String actorToken = null;
        private String actorType = null;

        private String scope = null;
        private String resource = null;
        private String audience = null;

        private String grantType = GRANT_TYPE;
        private String requestedTokenType = OAuth2Token.ACCESS_TOKEN;

        private Builder(String endpoint, OAuth2Token token) {
            this.endpoint = endpoint;
            this.token = token;
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
            return new OAuth2TokenExchangeProvider(clock, endpoint, token, fixedFormArgs(), timeoutSeconds);
        }

        private List<NameValuePair> fixedFormArgs() {
            List<NameValuePair> params = new ArrayList<>();

            // Required parameters
            params.add(new BasicNameValuePair("grand_type", grantType));
            params.add(new BasicNameValuePair("requested_token_type", requestedTokenType));

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
}
