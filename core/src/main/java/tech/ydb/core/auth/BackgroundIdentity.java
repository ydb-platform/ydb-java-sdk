package tech.ydb.core.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BackgroundIdentity implements tech.ydb.auth.AuthIdentity {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundIdentity.class);

    // Delays before retrying a failed background update while the current token is still valid
    private static final long RETRY_MIN_DELAY_MS = 1_000;
    private static final long RETRY_MAX_DELAY_MS = 60_000;

    public interface Rpc extends AutoCloseable {
        class Token {
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

        CompletableFuture<Token> getTokenAsync();
        int getTimeoutSeconds();

        @Override
        default void close() {
        }
    }

    private interface State {
        void init();
        State validate(Instant now);
        String token();
    }

    private final AtomicReference<State> state = new AtomicReference<>(new NullState());
    private final Clock clock;
    private final Rpc rpc;

    public BackgroundIdentity(Clock clock, Rpc rpc) {
        this.clock = clock;
        this.rpc = rpc;
    }

    @Override
    public void close() {
        rpc.close();
    }

    private State updateState(State current, State next) {
        Objects.requireNonNull(next, "next state cannot be null");
        if (state.compareAndSet(current, next)) {
            next.init();
        }
        return state.get();
    }

    @Override
    public String getToken() {
        return state.get().validate(clock.instant()).token();
    }

    private static Instant nextRetry(Instant now, int failedUpdates) {
        // 1s, 2s, 4s, ... up to 1 minute
        long delayMs = RETRY_MIN_DELAY_MS << Math.min(failedUpdates - 1, 10);
        return now.plus(Duration.ofMillis(Math.min(delayMs, RETRY_MAX_DELAY_MS)));
    }

    private <T> T unwrap(CompletableFuture<T> future) {
        try {
            return future.get(rpc.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException ex) {
            logger.error("authentication update problem", ex);
            throw new RuntimeException("authentication update problem", ex);
        } catch (InterruptedException ex) {
            logger.error("updating of authentication token was interrupted", ex);
            Thread.currentThread().interrupt();
            // returning null here would poison the state reference and break every following getToken()
            throw new RuntimeException("authentication update was interrupted", ex);
        }
    }

    private class NullState implements State {
        @Override
        public void init() {
            // Nothing
        }

        @Override
        public String token() {
            throw new IllegalStateException("Get token for null state");
        }

        @Override
        public State validate(Instant now) {
            return updateState(this, new SyncLogin()).validate(now);
        }
    }

    private class SyncLogin implements State {
        private final CompletableFuture<State> future = new CompletableFuture<>();

        @Override
        public void init() {
            rpc.getTokenAsync().whenComplete((token, th) -> {
                if (token != null) {
                    future.complete(new LoggedInState(token));
                } else {
                    future.complete(new ErrorState(th));
                }
            });
        }

        @Override
        public String token() {
            throw new IllegalStateException("Get token for unfinished sync state");
        }

        @Override
        public State validate(Instant now) {
            return updateState(this, unwrap(future));
        }
    }

    private class BackgroundLogin implements State {
        private final Rpc.Token token;
        private final int failedUpdates;
        private final CompletableFuture<State> future = new CompletableFuture<>();

        BackgroundLogin(Rpc.Token token, int failedUpdates) {
            this.token = token;
            this.failedUpdates = failedUpdates;
        }

        @Override
        public void init() {
            rpc.getTokenAsync().whenComplete((nextToken, th) -> {
                if (nextToken != null) {
                    future.complete(new LoggedInState(nextToken));
                } else {
                    future.completeExceptionally(th);
                }
            });
        }

        @Override
        public String token() {
            return token.token();
        }

        @Override
        public State validate(Instant now) {
            if (future.isCompletedExceptionally()) {
                if (now.isAfter(token.expiredAt())) {
                    // If token had already expired, switch to sync mode and wait for finishing
                    return updateState(this, new SyncLogin()).validate(now);
                }

                // else retry background login after a delay, every getToken() would retry it immediately otherwise
                int failed = failedUpdates + 1;
                return updateState(this, new LoggedInState(token, nextRetry(now, failed), failed));
            }

            if (future.isDone()) {
                return updateState(this, future.join());
            }

            return this;
        }
    }

    private class LoggedInState implements State {
        private final Rpc.Token token;
        private final Instant updateAt;
        private final int failedUpdates;

        LoggedInState(Rpc.Token token) {
            this(token, token.updateAt(), 0);
        }

        LoggedInState(Rpc.Token token, Instant updateAt, int failedUpdates) {
            this.token = token;
            this.updateAt = updateAt;
            this.failedUpdates = failedUpdates;
        }

        @Override
        public void init() {
            if (failedUpdates > 0) {
                logger.warn("background token update failed {} time(s), next attempt at {}", failedUpdates, updateAt);
            }
        }

        @Override
        public String token() {
            return token.token();
        }

        @Override
        public State validate(Instant now) {
            if (now.isAfter(token.expiredAt())) {
                // If token had already expired, switch to sync mode and wait for finishing
                return updateState(this, new SyncLogin()).validate(now);
            }
            if (now.isAfter(updateAt)) {
                return updateState(this, new BackgroundLogin(token, failedUpdates));
            }
            return this;
        }
    }

    private class ErrorState implements State {
        private final RuntimeException ex;

        ErrorState(Throwable ex) {
            this.ex = ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException("can't login", ex);
        }

        @Override
        public void init() { }

        @Override
        public String token() {
            throw ex;
        }

        @Override
        public State validate(Instant now) {
            return updateState(this, new SyncLogin()).validate(now);
        }
    }
}

