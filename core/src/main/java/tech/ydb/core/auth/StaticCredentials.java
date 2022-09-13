package tech.ydb.core.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import tech.ydb.auth.YdbAuth;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class StaticCredentials implements AuthProvider {
    private static final Logger logger = LoggerFactory.getLogger(StaticCredentials.class);

    private final Clock clock;
    private final YdbAuth.LoginRequest request;
    
    @VisibleForTesting
    StaticCredentials(Clock clock, String username, String password) {
        this.clock = clock;
        this.request = YdbAuth.LoginRequest.newBuilder()
                .setUser(username)
                .setPassword(password)
                .build();
    }

    public StaticCredentials(String username, String password) {
        this(Clock.systemUTC(), username, password);
    }

    @Override
    public AuthIdentity createAuthIdentity(AuthRpc rpc) {
        logger.info("create static identity for database {}", rpc.getDatabase());
        return new IdentityImpl(rpc);
    }
    
    private interface State {
        void init();
        State validate(Instant now);
        String token();
    }

    private class IdentityImpl implements AuthIdentity {
        private final AtomicReference<State> state = new AtomicReference<>(new NullState());
        private final StaticCredentitalsRpc rpc;

        public IdentityImpl(AuthRpc authRpc) {
            this.rpc = new StaticCredentitalsRpc(authRpc, request, clock);
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
        
        @Override
        public void close() {
            rpc.close();
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
                return updateState(this, new SyncLoginState()).validate(now);
            }
        }

        private class SyncLoginState implements State {
            private final CompletableFuture<State> future = new CompletableFuture<>();

            @Override
            public void init() {
                rpc.loginAsync().whenComplete((token, th) -> {
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
            public State validate(Instant instant) {
                return updateState(this, rpc.unwrap(future));
            }
        }

        private class LoggedInState implements State {
            private final StaticCredentitalsRpc.Token token;

            public LoggedInState(StaticCredentitalsRpc.Token token) {
                this.token = token;
                logger.debug("logged in with expired at {} and updating at {}", token.expiredAt(), token.updateAt());
            }

            @Override
            public void init() { }

            @Override
            public String token() {
                return token.token();
            }

            @Override
            public State validate(Instant instant) {
                return this;
            }
        }

        private class ErrorState implements State {
            private final RuntimeException ex;

            public ErrorState(Throwable ex) {
                this.ex = ex instanceof RuntimeException ? 
                        (RuntimeException)ex : new RuntimeException("can't login", ex);
            }

            @Override
            public void init() { }

            @Override
            public String token() {
                throw ex;
            }

            @Override
            public State validate(Instant instant) {
                return this;
            }
        }
    }
}
