package tech.ydb.core.auth;

import java.time.Clock;
import java.time.Instant;
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
        if (state.compareAndSet(current, next)) {
            next.init();
        }
        return state.get();
    }

    @Override
    public String getToken() {
        return state.get().validate(clock.instant()).token();
    }

    private <T> T unwrap(CompletableFuture<T> future) {
        try {
            return future.get(rpc.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException ex) {
            logger.error("authentication update problem", ex);
            throw new RuntimeException("authentication update problem", ex);
        } catch (InterruptedException ex) {
            logger.error("updating of authentication token was interupted", ex);
            Thread.currentThread().interrupt();
            return null;
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
        private final CompletableFuture<State> future = new CompletableFuture<>();

        BackgroundLogin(Rpc.Token token) {
            this.token = token;
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
                // else retry background login
                return updateState(this, new BackgroundLogin(token));
            }

            if (future.isDone()) {
                return updateState(this, future.join());
            }

            return this;
        }
    }

    private class LoggedInState implements State {
        private final Rpc.Token token;

        LoggedInState(Rpc.Token token) {
            this.token = token;
        }

        @Override
        public void init() { }

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
            if (now.isAfter(token.updateAt())) {
                return updateState(this, new BackgroundLogin(token));
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

