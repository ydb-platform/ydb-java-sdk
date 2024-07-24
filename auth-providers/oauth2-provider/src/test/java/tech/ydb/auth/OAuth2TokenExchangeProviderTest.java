package tech.ydb.auth;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.socket.PortFactory;
import org.mockserver.verify.VerificationTimes;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.impl.auth.GrpcAuthRpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OAuth2TokenExchangeProviderTest {
    private static final GrpcAuthRpc authRpc = Mockito.mock(GrpcAuthRpc.class);
    private static final ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
    private static final ClientAndServer mockClient = ClientAndServer.startClientAndServer(PortFactory.findFreePort());

    // Wednesday, June 1, 2022 00:00:00 UTC
    private static final Instant now = Instant.ofEpochSecond(1654041600);
    private static final List<CompletableFuture<?>> jobs = new ArrayList<>();

    @BeforeClass
    public static void validate() {
        Assert.assertTrue(mockClient.hasStarted());
        Assert.assertTrue(mockClient.isRunning());

        Mockito.when(authRpc.getExecutor()).thenReturn(scheduler);
        Mockito.when(scheduler.submit(Mockito.any(Runnable.class))).thenAnswer(iom -> {
            Runnable run = iom.getArgument(0, Runnable.class);
            CompletableFuture<?> future = CompletableFuture.runAsync(run);
            jobs.add(future);
            return future;
        });
    }

    @AfterClass
    public static void shutdown() {
        mockClient.close();
        scheduler.shutdown();
    }

    @Before
    public void reset() {
        mockClient.reset();
        jobs.clear();
    }

    private String testEndpoint() {
        return "http://127.0.0.1:" + mockClient.getLocalPort();
    }

    private static HttpResponse createResponse(String token) {
        return createResponse(token, "Bearer", null, 60);
    }

    private static HttpResponse createResponse(String token, String type) {
        return createResponse(token, type, null, 60);
    }

    private static HttpResponse createResponse(String token, String type, Integer expiresIn) {
        return createResponse(token, type, null, expiresIn);
    }

    private static HttpResponse createResponse(String token, String tokenType, String scope, Integer expiresIn) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"issued_token_type\": \"urn:ietf:params:oauth:token-type:access_token\"");
        if (token != null) {
            sb.append(",\"access_token\": ").append('"').append(token).append('"');
        }
        if (tokenType != null) {
            sb.append(",\"token_type\": ").append('"').append(tokenType).append('"');
        }
        if (expiresIn != null) {
            sb.append(",\"expires_in\": ").append(expiresIn);
        }
        if (scope != null) {
            sb.append(",\"scope\": ").append('"').append(scope).append('"');
        }
        sb.append("}");
        return HttpResponse.response(sb.toString());
    }

    private String requestForm(String token) {
        return Stream.of(
                "grant_type=" + OAuth2TokenExchangeProvider.GRANT_TYPE,
                "requested_token_type=" + OAuth2TokenSource.ACCESS_TOKEN,
                "subject_token=" + token,
                "subject_token_type=" + OAuth2TokenSource.JWT_TOKEN
        ).collect(Collectors.joining("&")).replace(":", "%3A");
    }

    @Test
    public void simpleTest() {
        mockClient.when(HttpRequest.request().withMethod("POST")).respond(createResponse("test_token"));

        OAuth2TokenSource token = OAuth2TokenSource.fromValue("Token1");
        OAuth2TokenExchangeProvider provider = OAuth2TokenExchangeProvider.newBuilder(testEndpoint(), token).build();
        try (AuthIdentity identity = provider.createAuthIdentity(authRpc)) {
            // token is cached
            Assert.assertEquals("Bearer test_token", identity.getToken());
            Assert.assertEquals("Bearer test_token", identity.getToken());

            mockClient.verify(HttpRequest.request().withMethod("POST")
                    .withHeader("Content-Type", "application/x-www-form-urlencoded")
                    .withBody(requestForm("Token1")),
                    VerificationTimes.exactly(1));
        }
    }

    @Test
    public void refreshTokensTest() {
        Clock clock = Mockito.mock(Clock.class);
        OAuth2TokenSource token = Mockito.mock(OAuth2TokenSource.class);

        Mockito.when(token.getType()).thenReturn(OAuth2TokenSource.JWT_TOKEN);
        Mockito.when(token.getExpireInSeconds()).thenReturn(40);
        Mockito.when(token.getToken()).thenReturn("jwt1", "jwt2");

        Mockito.when(clock.instant()).thenReturn(now);

        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once()).respond(createResponse("token1"));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once()).respond(createResponse("token2"));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once()).respond(createResponse("token3"));

        OAuth2TokenExchangeProvider provider = OAuth2TokenExchangeProvider.newBuilder(testEndpoint(), token)
                .withClock(clock)
                .withTimeoutSeconds(40)
                .build();
        try (AuthIdentity identity = provider.createAuthIdentity(authRpc)) {
            Assert.assertTrue(jobs.isEmpty());
            Assert.assertEquals("Bearer token1", identity.getToken());
            Assert.assertEquals(1, jobs.size());

            // token is cached
            mockClient.verify(HttpRequest.request().withMethod("POST").withBody(requestForm("jwt1")),
                    VerificationTimes.exactly(1));
            Mockito.verify(token, Mockito.times(1)).getToken();

            Assert.assertEquals("Bearer token1", identity.getToken());
            Mockito.verify(token, Mockito.times(1)).getToken();

            // move time close to half of token's life time, token is already cached
            Mockito.when(clock.instant()).thenReturn(now.plusSeconds(30));
            Assert.assertEquals("Bearer token1", identity.getToken());
            Assert.assertEquals(1, jobs.size());
            Mockito.verify(token, Mockito.times(1)).getToken();

            // move time to half of token's life time, token is already cached but update is started
            Mockito.when(clock.instant()).thenReturn(now.plusSeconds(31));
            Assert.assertEquals("Bearer token1", identity.getToken());
            Assert.assertEquals(2, jobs.size());

            // wait for updating finish
            jobs.get(1).join();
            Mockito.verify(token, Mockito.times(1)).getToken();
            mockClient.verify(HttpRequest.request().withMethod("POST").withBody(requestForm("jwt1")),
                    VerificationTimes.exactly(2));
            // Now we get a new token
            Assert.assertEquals("Bearer token2", identity.getToken());

            // move time to half of token's life time, token is already cached but update is started
            Mockito.when(clock.instant()).thenReturn(now.plusSeconds(62));
            Assert.assertEquals("Bearer token2", identity.getToken());
            Assert.assertEquals(3, jobs.size());

            // wait for updating finish
            jobs.get(2).join();
            Mockito.verify(token, Mockito.times(2)).getToken();
            mockClient.verify(HttpRequest.request().withMethod("POST").withBody(requestForm("jwt2")),
                    VerificationTimes.exactly(1));
            // Now we get a new token
            Assert.assertEquals("Bearer token3", identity.getToken());
        }
    }

    @Test
    public void customRequestTest() {
        mockClient.when(HttpRequest.request().withMethod("POST"))
                .respond(createResponse("custom_token", "bearer", "TestedScope", 10));

        OAuth2TokenSource token = OAuth2TokenSource.fromValue("Token1", "Test");
        OAuth2TokenExchangeProvider provider = OAuth2TokenExchangeProvider.newBuilder(testEndpoint(), token)
                .withActorTokenSource(OAuth2TokenSource.fromValue("actorToken", "actorType"))
                .withAudience("testAudience")
                .withResource("Resource")
                .withResource("OtherResource")
                .withScope("TestedScope")
                .withCustomGrantType(OAuth2TokenSource.ACCESS_TOKEN)
                .withCustomRequestedTokenType(OAuth2TokenSource.REFRESH_TOKEN)
                .build();

        try (AuthIdentity identity = provider.createAuthIdentity(authRpc)) {
            // token is cached
            Assert.assertEquals("Bearer custom_token", identity.getToken());
            Assert.assertEquals("Bearer custom_token", identity.getToken());

            String form = Stream.of(
                "grant_type=" + OAuth2TokenSource.ACCESS_TOKEN,
                "requested_token_type=" + OAuth2TokenSource.REFRESH_TOKEN,
                "resource=Resource",
                "resource=OtherResource",
                "audience=testAudience",
                "scope=TestedScope",
                "subject_token=Token1",
                "subject_token_type=Test",
                "actor_token=actorToken",
                "actor_token_type=actorType"
            ).collect(Collectors.joining("&")).replace(":", "%3A");

            mockClient.verify(HttpRequest.request().withMethod("POST")
                    .withHeader("Content-Type", "application/x-www-form-urlencoded")
                    .withBody(form),
                    VerificationTimes.exactly(1));
        }
    }

    @Test
    public void wrongTokenTypeTest() {
        mockClient.when(HttpRequest.request().withMethod("POST")).respond(createResponse("test_token", "Basic"));

        OAuth2TokenSource token = OAuth2TokenSource.fromValue("non");
        OAuth2TokenExchangeProvider provider = OAuth2TokenExchangeProvider.newBuilder(testEndpoint(), token).build();
        try (AuthIdentity identity = provider.createAuthIdentity(authRpc)) {
            UnexpectedResultException ex1 = Assert.assertThrows(UnexpectedResultException.class, identity::getToken);
            Assert.assertEquals(
                    "OAuth2 token exchange: unsupported token type: Basic, code: INTERNAL_ERROR",
                    ex1.getMessage()
            );
            Assert.assertEquals(Status.of(StatusCode.INTERNAL_ERROR), ex1.getStatus());

            // problem is not cached
            UnexpectedResultException ex2 = Assert.assertThrows(UnexpectedResultException.class, identity::getToken);
            UnexpectedResultException ex3 = Assert.assertThrows(UnexpectedResultException.class, identity::getToken);

            Assert.assertNotSame(ex1, ex2);
            Assert.assertNotSame(ex1, ex3);
            Assert.assertEquals(ex1.getMessage(), ex2.getMessage());
            Assert.assertEquals(ex1.getMessage(), ex3.getMessage());

            mockClient.verify(HttpRequest.request().withMethod("POST"), VerificationTimes.exactly(3));
        }
    }

    @Test
    public void wrongExpireIn() {
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(createResponse("test_token", "beAreR", null));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(createResponse("test_token", "BEARER", 0));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(createResponse("test_token", "Bearer", -1));

        OAuth2TokenSource token = OAuth2TokenSource.fromValue("non");
        OAuth2TokenExchangeProvider provider = OAuth2TokenExchangeProvider.newBuilder(testEndpoint(), token).build();
        try (AuthIdentity identity = provider.createAuthIdentity(authRpc)) {
            Assert.assertEquals(
                    "OAuth2 token exchange: incorrect expiration time: null, code: INTERNAL_ERROR",
                    Assert.assertThrows(UnexpectedResultException.class, identity::getToken).getMessage()
            );
            Assert.assertEquals(
                    "OAuth2 token exchange: incorrect expiration time: 0, code: INTERNAL_ERROR",
                    Assert.assertThrows(UnexpectedResultException.class, identity::getToken).getMessage()
            );
            Assert.assertEquals(
                    "OAuth2 token exchange: incorrect expiration time: -1, code: INTERNAL_ERROR",
                    Assert.assertThrows(UnexpectedResultException.class, identity::getToken).getMessage()
            );
            mockClient.verify(HttpRequest.request().withMethod("POST"), VerificationTimes.exactly(3));
        }
    }

    @Test
    public void wrongScope() {
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(createResponse("token2", "bearer", "other", 10));

        OAuth2TokenSource token = OAuth2TokenSource.fromValue("non");
        OAuth2TokenExchangeProvider provider = OAuth2TokenExchangeProvider.newBuilder(testEndpoint(), token)
                .withScope("scope")
                .build();
        try (AuthIdentity identity = provider.createAuthIdentity(authRpc)) {
            Assert.assertEquals(
                    "OAuth2 token exchange: different scope. Expected: scope, but got: other, code: INTERNAL_ERROR",
                    Assert.assertThrows(UnexpectedResultException.class, identity::getToken).getMessage()
            );
            mockClient.verify(HttpRequest.request().withMethod("POST"), VerificationTimes.exactly(1));
        }
    }

    @Test
    public void serverErrorsTest() {
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(HttpResponse.response().withStatusCode(400));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(HttpResponse.response().withStatusCode(401));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(HttpResponse.response().withStatusCode(403));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(HttpResponse.response().withStatusCode(404));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(HttpResponse.response().withStatusCode(500));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(HttpResponse.response().withStatusCode(501));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(HttpResponse.response().withStatusCode(503));
        mockClient.when(HttpRequest.request().withMethod("POST"), Times.once())
                .respond(HttpResponse.response().withStatusCode(504));

        OAuth2TokenSource token = OAuth2TokenSource.fromValue("non");
        OAuth2TokenExchangeProvider provider = OAuth2TokenExchangeProvider.newBuilder(testEndpoint(), token).build();
        try (AuthIdentity identity = provider.createAuthIdentity(authRpc)) {
            Supplier<String> next = () -> Assert.assertThrows(
                    UnexpectedResultException.class, identity::getToken
            ).getMessage();

            Assert.assertEquals("Cannot get OAuth2 token with code 400[Bad Request], code: BAD_REQUEST", next.get());
            Assert.assertEquals("Cannot get OAuth2 token with code 401[Unauthorized], code: UNAUTHORIZED", next.get());
            Assert.assertEquals("Cannot get OAuth2 token with code 403[Forbidden], code: UNAUTHORIZED", next.get());
            Assert.assertEquals("Cannot get OAuth2 token with code 404[Not Found], code: NOT_FOUND", next.get());

            Assert.assertEquals("Cannot get OAuth2 token with code 500[Internal Server Error], code: INTERNAL_ERROR", next.get());
            Assert.assertEquals("Cannot get OAuth2 token with code 501[Not Implemented], code: CLIENT_INTERNAL_ERROR", next.get());
            Assert.assertEquals("Cannot get OAuth2 token with code 503[Service Unavailable], code: UNAVAILABLE", next.get());
            Assert.assertEquals("Cannot get OAuth2 token with code 504[Gateway Timeout], code: UNAVAILABLE", next.get());

            mockClient.verify(HttpRequest.request().withMethod("POST"), VerificationTimes.exactly(8));
        }
    }

    private interface TokenExpectationChecker {
        public void check(String token, String tokenType) throws AssertionError, Exception;
    }

    private class FixedTokenExpectationChecker implements TokenExpectationChecker {
        String token;
        String tokenType;

        public FixedTokenExpectationChecker(String token, String tokenType) {
            this.token = token;
            this.tokenType = tokenType;
        }

        public void check(String token, String tokenType) throws AssertionError, Exception {
            Assert.assertEquals(this.token, token);
            Assert.assertEquals(this.tokenType, tokenType);
        }
    }

    private class JwtTokenExpectationChecker implements TokenExpectationChecker {
        private String alg;
        private String publicKey;
        private String kid;
        private String iss;
        private String sub;
        private String aud;
        private String jti;
        private int ttlSeconds;

        public JwtTokenExpectationChecker(
            String alg,
            String publicKey,
            String kid,
            String iss,
            String sub,
            String aud,
            String jti,
            int ttlSeconds
        )
        {
            this.alg = alg;
            this.publicKey = publicKey;
            this.kid = kid;
            this.iss = iss;
            this.sub = sub;
            this.aud = aud;
            this.jti = jti;
            this.ttlSeconds = ttlSeconds;
        }

        public void check(String token, String tokenType) throws AssertionError, Exception {
            Assert.assertEquals("urn:ietf:params:oauth:token-type:jwt", tokenType);

            Jwt<?, Claims> parsed = Jwts.parser()
                .setSigningKey(getKey())
                .parseClaimsJws(token);

            Assert.assertEquals("JWT", parsed.getHeader().getType());
            Assert.assertEquals(alg, parsed.getHeader().get("alg"));
            Assert.assertEquals(kid, parsed.getHeader().get("kid"));

            Assert.assertEquals(iss, parsed.getBody().getIssuer());
            Assert.assertEquals(sub, parsed.getBody().getSubject());
            Assert.assertEquals(aud, parsed.getBody().getAudience());
            Assert.assertEquals(jti, parsed.getBody().getId());

            Date now = new Date();
            Assert.assertTrue(now.getTime() - parsed.getBody().getIssuedAt().getTime() < 600 * 1000); // getTime() is in milliseconds
            Assert.assertEquals(ttlSeconds * 1000, parsed.getBody().getExpiration().getTime() - parsed.getBody().getIssuedAt().getTime());
        }

        private Key getKey() {
            if (alg == "HS256" || alg == "HS384" || alg == "HS512") {
                return OAuth2TokenTest.getHmacKey(alg, publicKey);
            }
            return OAuth2TokenTest.getPublicKeyFromPem(publicKey);
        }
    }

    private class RequestExpectationChecker implements ExpectationResponseCallback {
        String grantType;
        String requestedTokenType;
        String[] res;
        String[] aud;
        String scope;
        TokenExpectationChecker subjectTokenChecker;
        TokenExpectationChecker actorTokenChecker;

        public RequestExpectationChecker(String grantType, String requestedTokenType, String[] res, String[] aud, String scope, TokenExpectationChecker subjectChecker, TokenExpectationChecker actorChecker) {
            this.grantType = grantType;
            this.requestedTokenType = requestedTokenType;
            this.res = res;
            this.aud = aud;
            this.scope = scope;
            this.subjectTokenChecker = subjectChecker;
            this.actorTokenChecker = actorChecker;
        }

        public HttpResponse handle(HttpRequest req) throws Exception {
            try {
                List<NameValuePair> params = ParseQueryParams(req);
                Assert.assertEquals(grantType, GetSingleParam(params, "grant_type", true));
                Assert.assertEquals(requestedTokenType, GetSingleParam(params, "requested_token_type", true));
                Assert.assertArrayEquals(aud, GetAllParams(params, "audience"));
                Assert.assertArrayEquals(res, GetAllParams(params, "resource"));
                Assert.assertEquals(scope, GetSingleParam(params, "scope", false));

                if (subjectTokenChecker != null) {
                    subjectTokenChecker.check(GetSingleParam(params, "subject_token", true), GetSingleParam(params, "subject_token_type", true));
                } else {
                    Assert.assertNull("Expected no subject_token param", GetSingleParam(params, "subject_token", false));
                    Assert.assertNull("Expected no subject_token_type param", GetSingleParam(params, "subject_token_type", false));
                }

                if (actorTokenChecker != null) {
                    actorTokenChecker.check(GetSingleParam(params, "actor_token", true), GetSingleParam(params, "actor_token_type", true));
                } else {
                    Assert.assertNull("Expected no actor_token param", GetSingleParam(params, "actor_token", false));
                    Assert.assertNull("Expected no actor_token_type param", GetSingleParam(params, "actor_token_type", false));
                }

                return createResponse("test_token");
            } catch (Exception ex) {
                System.err.println(String.format("Exception in http handle: %s", ex.getMessage()));
                throw ex;
            } catch (AssertionError ex) {
                System.err.println(String.format("Assertion exception in http handle: %s", ex.getMessage()));
                throw ex;
            }
        }

        private List<NameValuePair> ParseQueryParams(HttpRequest req) throws Exception {
            System.err.println(String.format("Parsing http request body: %s", req.getBodyAsString()));
            URI uri = new URI("https", "", "/path", req.getBodyAsString(), "");
            List<NameValuePair> params = URLEncodedUtils.parse(uri, Charset.defaultCharset());
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            for (int i = 0; i < params.size(); i++) {
                list.add(new BasicNameValuePair(
                    params.get(i).getName(),
                    params.get(i).getValue().replace("%3A", ":")
                ));
            }
            return list;
        }

        private String GetSingleParam(List<NameValuePair> params, String name, boolean required) {
            int cnt = 0;
            String result = null;
            for (int i = 0; i < params.size(); i++) {
                if (name.equals(params.get(i).getName())) {
                    cnt++;
                    result = params.get(i).getValue();
                }
            }
            Assert.assertTrue("Expected not greater that 1 parameter " + name, cnt <= 1);
            Assert.assertTrue("Expected parameter " + name, !required || result != null);
            return result;
        }

        private String[] GetAllParams(List<NameValuePair> params, String name) {
            int cnt = 0;
            for (int i = 0; i < params.size(); i++) {
                if (name.equals(params.get(i).getName())) {
                    cnt++;
                }
            }
            if (cnt == 0) {
                return null;
            }
            String[] result = new String[cnt];
            for (int i = 0, j = 0; i < params.size() && j < cnt; i++) {
                if (name.equals(params.get(i).getName())) {
                    result[j++] = params.get(i).getValue();
                }
            }
            return result;
        }
    }

    private class FromConfigTestCase {
        private String cfg;
        private File cfgFile;
        private RequestExpectationChecker requestChecker;
        private String errorMessageOnCreate;
        private String errorMessageOnGet;

        public FromConfigTestCase(String cfg, RequestExpectationChecker checker, String errorMessageOnCreate, String errorMessageOnGet) {
            this.cfg = cfg;
            this.requestChecker = checker;
            this.errorMessageOnCreate = errorMessageOnCreate;
            this.errorMessageOnGet = errorMessageOnGet;
        }

        public FromConfigTestCase(File cfgFile, RequestExpectationChecker checker, String errorMessageOnCreate, String errorMessageOnGet) {
            this.cfgFile = cfgFile;
            this.requestChecker = checker;
            this.errorMessageOnCreate = errorMessageOnCreate;
            this.errorMessageOnGet = errorMessageOnGet;
        }

        public File getCfgFile() {
            if (this.cfgFile != null) {
                return this.cfgFile;
            }
            try {
                this.cfgFile = File.createTempFile("cfg", ".json");
                this.cfgFile.deleteOnExit();
                FileWriter writer = new FileWriter(this.cfgFile);
                writer.write(this.cfg);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to write config to temp file", e);
            }
            return this.cfgFile;
        }
    }

    @Test
    public void fromConfigTest() {
        FromConfigTestCase[] tests = new FromConfigTestCase[] {
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"res\": \"tEst\", " +
                        "\"grant-type\": \"grant\", " +
                        "\"subject-credentials\": {" +
                            "\"type\": \"fixed\", " +
                            "\"token\": \"test-token\", " +
                            "\"token-type\": \"test-token-type\"" +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                new RequestExpectationChecker(
                    "grant",
                    "urn:ietf:params:oauth:token-type:access_token",
                    new String[]{"tEst"},
                    null,
                    null,
                    new FixedTokenExpectationChecker("test-token", "test-token-type"),
                    null
                ),
                null,
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"res\": [\"r1\", \"r2\"], " +
                        "\"aud\": \"test-aud\", " +
                        "\"scope\": [\"s1\", \"s2\"], " +
                        "\"unknown-field\": [123], " +
                        "\"subject-credentials\": {" +
                            "\"type\": \"fixed\", " +
                            "\"token\": \"test-token\", " +
                            "\"token-type\": \"test-token-type\"" +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                new RequestExpectationChecker(
                    "urn:ietf:params:oauth:grant-type:token-exchange",
                    "urn:ietf:params:oauth:token-type:access_token",
                    new String[]{"r1", "r2"},
                    new String[]{"test-aud"},
                    "s1 s2",
                    new FixedTokenExpectationChecker("test-token", "test-token-type"),
                    null
                ),
                null,
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"requested-token-type\": \"access_token\", " +
                        "\"aud\": [\"test-aud\"], " +
                        "\"scope\": \"scope\", " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"Jwt\", " +
                            "\"alg\": \"ps256\", " +
					        "\"private-key\": \"%s\", " +
					        "\"aud\": \"test_audience\", " +
                            "\"jti\": \"123\", " +
                            "\"sub\": \"test_subject\", " +
                            "\"iss\": \"test_issuer\", " +
                            "\"kid\": \"test_key_id\", " +
                            "\"ttl\": \"24h\", " +
                            "\"unknown_field\": [123] " +
                        "}" +
                    "}",
                    testEndpoint(),
                    OAuth2TokenTest.TEST_RSA_PRIVATE_KEY_JSON
                ),
                new RequestExpectationChecker(
                    "urn:ietf:params:oauth:grant-type:token-exchange",
                    "access_token",
                    null,
                    new String[]{"test-aud"},
                    "scope",
                    null,
                    new JwtTokenExpectationChecker(
                        "PS256",
                        OAuth2TokenTest.TEST_RSA_PUBLIC_KEY,
                        "test_key_id",
                        "test_issuer",
                        "test_subject",
                        "test_audience",
                        "123",
                        24 * 60 * 60
                    )
                ),
                null,
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"subject-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"alg\": \"es256\", " +
					        "\"private-key\": \"%s\", " +
                            "\"ttl\": \"3m\" " +
                        "}" +
                    "}",
                    testEndpoint(),
                    OAuth2TokenTest.TEST_EC_PRIVATE_KEY_JSON
                ),
                new RequestExpectationChecker(
                    "urn:ietf:params:oauth:grant-type:token-exchange",
                    "urn:ietf:params:oauth:token-type:access_token",
                    null,
                    null,
                    null,
                    new JwtTokenExpectationChecker(
                        "ES256",
                        OAuth2TokenTest.TEST_EC_PUBLIC_KEY,
                        null,
                        null,
                        null,
                        null,
                        null,
                        3 * 60
                    ),
                    null
                ),
                null,
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"subject-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"alg\": \"hs512\", " +
					        "\"private-key\": \"%s\" " +
                        "}" +
                    "}",
                    testEndpoint(),
                    OAuth2TokenTest.TEST_HMAC_SECRET_KEY_BASE64
                ),
                new RequestExpectationChecker(
                    "urn:ietf:params:oauth:grant-type:token-exchange",
                    "urn:ietf:params:oauth:token-type:access_token",
                    null,
                    null,
                    null,
                    new JwtTokenExpectationChecker(
                        "HS512",
                        OAuth2TokenTest.TEST_HMAC_SECRET_KEY_BASE64,
                        null,
                        null,
                        null,
                        null,
                        null,
                        60 * 60
                    ),
                    null
                ),
                null,
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"subject-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"alg\": \"rs512\", " +
					        "\"private-key\": \"%s\" " +
                        "}" +
                    "}",
                    testEndpoint(),
                    OAuth2TokenTest.TEST_HMAC_SECRET_KEY_BASE64
                ),
                null,
                "Failed to parse PEM key",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"subject-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"alg\": \"es512\", " +
					        "\"private-key\": \"%s\" " +
                        "}" +
                    "}",
                    testEndpoint(),
                    OAuth2TokenTest.TEST_HMAC_SECRET_KEY_BASE64
                ),
                null,
                "Failed to parse PEM key",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"subject-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"alg\": \"es512\", " +
					        "\"private-key\": \"%s\" " +
                        "}" +
                    "}",
                    testEndpoint(),
                    OAuth2TokenTest.TEST_RSA_PRIVATE_KEY_JSON
                ),
                null,
                null,
                "ECDSA signing keys must be ECKey instances"
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"subject-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"alg\": \"HS512\", " +
					        "\"private-key\": \"<not base64>\" " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Illegal base64 character",
                null
            ),
            new FromConfigTestCase(
                new File("~/unknown-file.cfg"),
                null,
                "Unable to read config from",
                null
            ),
            new FromConfigTestCase(
                "{not json",
                null,
                "MalformedJsonException",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": \"\"" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Expected BEGIN_OBJECT but was STRING",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"ttl\": 123 " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Cannot parse ttl from json",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"ttl\": \"123\" " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Cannot parse ttl from json",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"ttl\": \"-3h\" " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Cannot parse ttl from json, negative duration is not allowed",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"alg\": \"HS384\" " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Key is required",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"private-key\": \"1234\" " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Algorithm is required",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"JWT\", " +
                            "\"alg\": \"unknown\", " +
                            "\"private-key\": \"1234\" " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Algorithm \"unknown\" is not supported. Supported algorithms: \"ES256\", \"ES384\", \"ES512\", \"HS256\", \"HS384\", \"HS512\", \"PS256\", \"PS384\", \"PS512\", \"RS256\", \"RS384\", \"RS512\"",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"aud\": {" +
                            "\"value\": \"wrong_format of aud: not string and not list\"" +
                        "}, " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"FIXED\", " +
                            "\"token\": \"test-token\", " +
                            "\"token-type\": \"test-token-type\"" +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Cannot parse config from json: field is expected to be string or array of nonempty strings",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"unknown\" " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Unsupported token source type: unknown",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": {" +
                            "\"token\": \"123\" " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "No token source type",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"FIXED\", " +
                            "\"token\": \"123\" " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Both token and token-type are required",
                null
            ),
            new FromConfigTestCase(
                String.format(
                    "{" +
                        "\"token-endpoint\": \"%s\", " +
                        "\"actor-credentials\": {" +
                            "\"type\": \"FIXED\", " +
                            "\"token-type\": \"t\" " +
                        "}" +
                    "}",
                    testEndpoint()
                ),
                null,
                "Both token and token-type are required",
                null
            ),
        };
        for (FromConfigTestCase test: tests) {
            try {
                mockClient.reset();
                OAuth2TokenExchangeProvider provider = null;

                File cfgFile = test.getCfgFile();
                try {
                    provider = OAuth2TokenExchangeProvider.fromFile(cfgFile).build();
                    Assert.assertNull(test.errorMessageOnCreate);
                    Assert.assertNotNull(provider);
                } catch (Exception ex) {
                    String msg = ex.getMessage();
                    Assert.assertNotNull(msg);
                    Assert.assertNotNull(String.format("Got exception while provider was initializing: %s", msg), test.errorMessageOnCreate);
                    Assert.assertTrue(String.format("Exception on creation [%s] is expected to contain substring [%s]", msg, test.errorMessageOnCreate), msg.indexOf(test.errorMessageOnCreate) >= 0);
                }

                if (provider == null) {
                    continue;
                }

                if (test.requestChecker != null) {
                    mockClient.when(HttpRequest.request().withMethod("POST")).respond(test.requestChecker);
                }

                try (AuthIdentity identity = provider.createAuthIdentity(authRpc)) {
                    // token is cached
                    String token = identity.getToken();
                    Assert.assertNotNull(test.requestChecker); // no exception is expected

                    Assert.assertEquals("Bearer test_token", token);

                    mockClient.verify(HttpRequest.request().withMethod("POST")
                            .withHeader("Content-Type", "application/x-www-form-urlencoded"),
                            VerificationTimes.exactly(1));
                } catch (Exception ex) {
                    String testMsg = String.format("Got exception while getting token: %s", ex.getMessage());
                    Assert.assertNull(testMsg, test.requestChecker);
                    Assert.assertNotNull(testMsg, test.errorMessageOnGet);
                    Assert.assertTrue(String.format("Exception on get token [%s] is expected to contain substring [%s]", ex.getMessage(), test.errorMessageOnGet), ex.getMessage().indexOf(test.errorMessageOnGet) >= 0);
                }
            } catch (AssertionError ex) {
                System.err.println(String.format("Test failed. File: %s, Cfg:\n%s", test.getCfgFile().getName(), test.cfg));
                throw ex;
            }
        }
    }
}
