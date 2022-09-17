package tech.ydb.core.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import tech.ydb.auth.YdbAuth;
import tech.ydb.auth.v1.AuthServiceGrpc;
import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
class StaticCredentitalsRpc {
    private static final Logger logger = LoggerFactory.getLogger(StaticCredentitalsRpc.class);

    private static final long LOGIN_TIMEOUT_SECONDS = 10;
    private static final int MAX_RETRIES_COUNT = 5;
    private static final long WAIT_FOR_CLOSING_MS = 1000;

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

    private final AtomicInteger retries = new AtomicInteger(MAX_RETRIES_COUNT);

    private final AuthRpc rpc;
    private final YdbAuth.LoginRequest request;
    private final Clock clock;
    private final ExecutorService executor;

    StaticCredentitalsRpc(AuthRpc rpc, YdbAuth.LoginRequest request, Clock clock, Supplier<ExecutorService> executor) {
        this.rpc = rpc;
        this.request = request;
        this.clock = clock;
        this.executor = executor.get();
    }
    
    public void close() {
        String database = rpc.getDatabase();

        logger.info("close login rpc of {}", rpc.getDatabase());
        try {
            executor.shutdown();
            boolean closed = executor.awaitTermination(WAIT_FOR_CLOSING_MS, TimeUnit.MILLISECONDS);
            if (!closed) {
                logger.warn("static identity of {} closing timeout exceeded, terminate",database);
                executor.shutdownNow();
                closed = executor.awaitTermination(WAIT_FOR_CLOSING_MS, TimeUnit.MILLISECONDS);
                if (closed) {
                    logger.debug("static identity of {} shut down successfully", database);
                } else {
                    logger.warn("closing problem for static identity for database {}", database);
                }
            }
        } catch (InterruptedException e) {
            logger.warn("static identity of {} was interrupted: {}", database, e);
            Thread.currentThread().interrupt();
        }
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

        if (executor.isShutdown()) {
            future.completeExceptionally(new IllegalStateException("static credentitals rpc is already stopped"));
        }

        executor.submit(() -> {
            try (GrpcTransport transport = rpc.createTransport()) {
                GrpcRequestSettings grpcSettings = GrpcRequestSettings.newBuilder()
                        .withDeadlineAfter(System.nanoTime() + Duration.ofSeconds(LOGIN_TIMEOUT_SECONDS).toNanos())
                        .build();

                transport.unaryCall(AuthServiceGrpc.getLoginMethod(), grpcSettings, request)
                        .thenApply(Operations.resultUnwrapper(
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
    
    public CompletableFuture<Token> loginAsync() {
        CompletableFuture<Token> tokenFuture = new CompletableFuture<>();
        tryLogin(tokenFuture);
        return tokenFuture;
    }
    
    public <T> T unwrap(CompletableFuture<T> future) {
        try {
            return future.get(LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException ex) {
            logger.error("static authentication problem", ex);
            throw new RuntimeException("static authentication problem", ex);
        } catch (InterruptedException ex) {
            logger.error("validation of static credentials token is interupted", ex);
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    public static class Token {
        private final String token;
        private final Instant expiredAt;
        private final Instant updateAt;
        
        public Token(String token, Instant expiredAt, Instant updateAt) {
            this.token = token;
            this.expiredAt = expiredAt;
            this.updateAt = updateAt;
        }
        
        public String token() {
            return this.token;
        }

        public Instant expiredAt() {
            return this.expiredAt;
        }

        public Instant updateAt() {
            return this.updateAt;
        }
    }
}
