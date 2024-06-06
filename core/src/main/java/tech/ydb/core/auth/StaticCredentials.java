package tech.ydb.core.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.auth.GrpcAuthRpc;
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.proto.auth.YdbAuth;
import tech.ydb.proto.auth.v1.AuthServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class StaticCredentials implements AuthRpcProvider<GrpcAuthRpc> {
    private static final Logger logger = LoggerFactory.getLogger(StaticCredentials.class);

    private static final int LOGIN_TIMEOUT_SECONDS = 10;
    private static final int MAX_RETRIES_COUNT = 5;

    private static final EnumSet<StatusCode> RETRYABLE_STATUSES = EnumSet.of(
        StatusCode.ABORTED,
        StatusCode.UNAVAILABLE,
        StatusCode.OVERLOADED,
        StatusCode.CLIENT_RESOURCE_EXHAUSTED,
        StatusCode.BAD_SESSION,
        StatusCode.SESSION_BUSY,
        StatusCode.UNDETERMINED,
        StatusCode.TRANSPORT_UNAVAILABLE
    );

    private final Clock clock;
    private final YdbAuth.LoginRequest request;

    @VisibleForTesting
    StaticCredentials(Clock clock, String username, String password) {
        this.clock = clock;
        YdbAuth.LoginRequest.Builder builder = YdbAuth.LoginRequest.newBuilder()
                .setUser(username);
        if (password != null) {
            builder.setPassword(password);
        }
        this.request = builder.build();
    }

    public StaticCredentials(String username, String password) {
        this(Clock.systemUTC(), username, password);
    }

    @Override
    public tech.ydb.auth.AuthIdentity createAuthIdentity(GrpcAuthRpc rpc) {
        logger.info("create static identity for database {}", rpc.getDatabase());
        return new BackgroundIdentity(clock, new LoginRpc(rpc));
    }

    private class LoginRpc implements BackgroundIdentity.Rpc {
        private final GrpcAuthRpc rpc;
        private final AtomicInteger retries = new AtomicInteger(MAX_RETRIES_COUNT);

        LoginRpc(GrpcAuthRpc rpc) {
            this.rpc = rpc;
        }

        private void handleResult(CompletableFuture<Token> future, Result<YdbAuth.LoginResult> resp) {
            if (resp.isSuccess()) {
                try {
                    Instant now = clock.instant();
                    String token = resp.getValue().getToken();
                    Instant expiredAt = JwtUtils.extractExpireAt(token, now);

                    long expiresIn = expiredAt.getEpochSecond() - now.getEpochSecond();
                    Instant updateAt = now.plus(expiresIn / 2, ChronoUnit.SECONDS);
                    updateAt = updateAt.isBefore(now) ? now : updateAt;

                    logger.debug("logged in with expired at {} and updating at {}", expiredAt, updateAt);
                    future.complete(new Token(token, expiredAt, updateAt));
                } catch (RuntimeException ex) {
                    future.completeExceptionally(ex);
                }
            } else {
                logger.error("Login request get wrong status {}", resp.getStatus());
                if (RETRYABLE_STATUSES.contains(resp.getStatus().getCode()) && retries.decrementAndGet() > 0) {
                    tryLogin(future);
                } else {
                    future.completeExceptionally(new UnexpectedResultException("Can't login", resp.getStatus()));
                }
            }
        }

        private void handleException(CompletableFuture<Token> future, Throwable th) {
            logger.error("Login request get exception {}", th.getMessage());
            if (retries.decrementAndGet() > 0) {
                tryLogin(future);
            } else {
                future.completeExceptionally(th);
            }
        }

        private void tryLogin(CompletableFuture<Token> future) {
            if (future.isCancelled() || future.isDone()) {
                return;
            }

            rpc.getExecutor().submit(() -> {
                try (GrpcTransport transport = rpc.createTransport()) {
                    GrpcRequestSettings grpcSettings = GrpcRequestSettings.newBuilder()
                            .withDeadline(Duration.ofSeconds(LOGIN_TIMEOUT_SECONDS))
                            .build();

                    transport.unaryCall(AuthServiceGrpc.getLoginMethod(), grpcSettings, request)
                            .thenApply(OperationBinder.bindSync(
                                    YdbAuth.LoginResponse::getOperation,
                                    YdbAuth.LoginResult.class
                            ))
                            .whenComplete((resp, th) -> {
                                if (resp != null) {
                                    handleResult(future, resp);
                                }
                                if (th != null) {
                                    handleException(future, th);
                                }
                            })
                            .join();
                }
            });
        }

        @Override
        public CompletableFuture<Token> getTokenAsync() {
            CompletableFuture<Token> tokenFuture = new CompletableFuture<>();
            tokenFuture.whenComplete((token, th) -> {
                if (token == null || th != null) {
                    rpc.changeEndpoint();
                }
            });

            tryLogin(tokenFuture);
            return tokenFuture;
        }

        @Override
        public int getTimeoutSeconds() {
            return LOGIN_TIMEOUT_SECONDS;
        }
    }
}
