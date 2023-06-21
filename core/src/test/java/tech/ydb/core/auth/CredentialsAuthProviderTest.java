package tech.ydb.core.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import com.google.common.truth.Truth;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Any;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.auth.GrpcAuthRpc;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.auth.YdbAuth;
import tech.ydb.proto.auth.v1.AuthServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CredentialsAuthProviderTest {
    private final Clock clock = Mockito.mock(Clock.class);
    private final GrpcTransport transport = Mockito.mock(GrpcTransport.class);
    private final GrpcAuthRpc rpc = Mockito.mock(GrpcAuthRpc.class);

    // Wednesday, June 1, 2022 00:00:00 UTC
    private final Instant now = Instant.ofEpochSecond(1654041600);

    @Before
    public void setup() {
        Mockito.when(rpc.getDatabase()).thenReturn("Mocked database name");
        Mockito.when(rpc.createTransport()).thenReturn(transport);
        Mockito.when(rpc.getExecutor()).thenReturn(MoreExecutors.newDirectExecutorService());
    }

    @Test
    public void credentialsTest() {
        String token = JwtBuilder.create(now.plus(Duration.ofHours(2)), now);
        Status unauthorized = Status.of(StatusCode.UNAUTHORIZED);

        Mockito.when(clock.instant()).thenReturn(now);

        Mockito.when(transport.unaryCall(
                Mockito.eq(AuthServiceGrpc.getLoginMethod()),
                Mockito.any(),
                Mockito.argThat(
                        req -> req.getUser().equals("user") && req.getPassword().equals("pass1")
                )
        )).thenReturn(CompletableFuture.completedFuture(Result.success(responseOk(token))));

        Mockito.when(transport.unaryCall(
                Mockito.eq(AuthServiceGrpc.getLoginMethod()),
                Mockito.any(),
                Mockito.argThat(
                        req -> !req.getUser().equals("user") || !req.getPassword().equals("pass1")
                )
        )).thenReturn(CompletableFuture.completedFuture(Result.fail(unauthorized)));

        // With correct credentitals
        try (tech.ydb.auth.AuthIdentity identity = createAuth("user", "pass1")) {
            Truth.assertThat(identity.getToken()).isEqualTo(token);
            Truth.assertThat(identity.getToken()).isEqualTo(token);
        }

        // With incorrect password
        try (tech.ydb.auth.AuthIdentity identity = createAuth("user", "pass2")) {
            UnexpectedResultException ex = Assert.assertThrows(
                    UnexpectedResultException.class,
                    () -> identity.getToken()
            );
            Truth.assertThat(ex.getStatus()).isEqualTo(unauthorized);

            UnexpectedResultException ex2 = Assert.assertThrows(
                    UnexpectedResultException.class,
                    () -> identity.getToken()
            );
            Truth.assertThat(ex2).isEqualTo(ex);
        }
    }

    private <T> CompletableFuture<T> failedFuture(Exception ex) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    @Test
    public void retriesTest() {
        String token = JwtBuilder.create(now.plus(Duration.ofHours(2)), now);
        Status overloaded = Status.of(StatusCode.OVERLOADED);
        Status unavailable = Status.of(StatusCode.UNAVAILABLE);

        Mockito.when(clock.instant()).thenReturn(now);

        Mockito.when(transport.unaryCall(
                Mockito.eq(AuthServiceGrpc.getLoginMethod()), Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(unavailable)))
                .thenReturn(failedFuture(new RuntimeException("error1")))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(overloaded)))
                .thenReturn(failedFuture(new RuntimeException("error2")))
                .thenReturn(CompletableFuture.completedFuture(Result.success(responseOk(token))));

        // With any credentials
        try (tech.ydb.auth.AuthIdentity identity = createAuth("user", null)) {
            Truth.assertThat(identity.getToken()).isEqualTo(token);
            Truth.assertThat(identity.getToken()).isEqualTo(token);
        }

        Mockito.when(transport.unaryCall(
                Mockito.eq(AuthServiceGrpc.getLoginMethod()), Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(unavailable)))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(unavailable)))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(overloaded)))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(overloaded)))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(overloaded)))
                .thenReturn(CompletableFuture.completedFuture(Result.success(responseOk(token))));

        // With any credentials
        try (tech.ydb.auth.AuthIdentity identity = createAuth("user", null)) {
            UnexpectedResultException ex = Assert.assertThrows(
                    UnexpectedResultException.class,
                    () -> identity.getToken()
            );
            Truth.assertThat(ex.getStatus()).isEqualTo(overloaded);
        }
    }

    @Test
    public void refreshTokenTest() {
        Status unavailable = Status.of(StatusCode.UNAVAILABLE);

        Duration expireTime = Duration.ofHours(2);
        Instant firstHour = now.plus(Duration.ofHours(1));
        Instant secondHour = now.plus(Duration.ofHours(2));

        String token1 = JwtBuilder.create(now.plus(expireTime), now);
        String token2 = JwtBuilder.create(firstHour.plus(expireTime), firstHour);
        String token3 = JwtBuilder.create(secondHour.plus(expireTime), secondHour);

        Mockito.when(transport.unaryCall(
                Mockito.eq(AuthServiceGrpc.getLoginMethod()), Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Result.success(responseOk(token1))))
                .thenReturn(CompletableFuture.completedFuture(Result.success(responseOk(token2))))
                .thenReturn(CompletableFuture.completedFuture(Result.fail(unavailable)))
                .thenReturn(failedFuture(new RuntimeException("error")))
                .thenReturn(CompletableFuture.completedFuture(Result.success(responseOk(token3))));

        Mockito.when(clock.instant()).thenReturn(now);

        tech.ydb.auth.AuthIdentity identity = createAuth("user", "password");

        Truth.assertThat(identity.getToken()).isEqualTo(token1);

        // When now is firstHour - token1 doesn't need to update
        Mockito.when(clock.instant()).thenReturn(firstHour);
        Truth.assertThat(identity.getToken()).isEqualTo(token1);
        Truth.assertThat(identity.getToken()).isEqualTo(token1);

        Mockito.when(clock.instant()).thenReturn(firstHour.plusMillis(5));
        Truth.assertThat(identity.getToken()).isEqualTo(token1);
        Truth.assertThat(identity.getToken()).isEqualTo(token2);

        Mockito.when(clock.instant()).thenReturn(secondHour);
        Truth.assertThat(identity.getToken()).isEqualTo(token2);
        Truth.assertThat(identity.getToken()).isEqualTo(token2);

        Mockito.when(clock.instant()).thenReturn(secondHour.plusMillis(10));
        Truth.assertThat(identity.getToken()).isEqualTo(token2);
        Truth.assertThat(identity.getToken()).isEqualTo(token3);

        identity.close();
    }

    @Test
    public void syncRefreshTokenTest() {
        Duration expireTime = Duration.ofHours(2);
        Instant secondHour = now.plus(expireTime);

        String token1 = JwtBuilder.create(now.plus(expireTime), now);
        String token2 = JwtBuilder.create(secondHour.plus(expireTime), secondHour);

        Mockito.when(transport.unaryCall(
                Mockito.eq(AuthServiceGrpc.getLoginMethod()), Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Result.success(responseOk(token1))))
                .thenReturn(CompletableFuture.completedFuture(Result.success(responseOk(token2))));

        Mockito.when(clock.instant()).thenReturn(now);

        tech.ydb.auth.AuthIdentity identity = createAuth("user", "password");

        Truth.assertThat(identity.getToken()).isEqualTo(token1);

        Mockito.when(clock.instant()).thenReturn(secondHour.plusMillis(1));
        // token1 is already expired, use sync request to new token
        Truth.assertThat(identity.getToken()).isEqualTo(token2);

        identity.close();
    }

    private tech.ydb.auth.AuthIdentity createAuth(String login, String password) {
        return new StaticCredentials(clock, login, password)
                .createAuthIdentity(rpc);
    }

    private static YdbAuth.LoginResponse responseOk(String token) {
        YdbAuth.LoginResult result = YdbAuth.LoginResult.newBuilder()
                .setToken(token)
                .build();

        OperationProtos.Operation operation = OperationProtos.Operation.newBuilder()
                .setId("good_id")
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setReady(true)
                .setResult(Any.pack(result))
                .build();

        return YdbAuth.LoginResponse.newBuilder()
                .setOperation(operation)
                .build();
    }
}
