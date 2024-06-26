package tech.ydb.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.socket.PortFactory;
import org.mockserver.verify.VerificationTimes;

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

    private HttpResponse createResponse(String token) {
        return createResponse(token, "Bearer", null, 60);
    }

    private HttpResponse createResponse(String token, String type) {
        return createResponse(token, type, null, 60);
    }

    private HttpResponse createResponse(String token, String type, Integer expiresIn) {
        return createResponse(token, type, null, expiresIn);
    }

    private HttpResponse createResponse(String token, String tokenType, String scope, Integer expiresIn) {
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
}
