package tech.ydb.table.impl.pool;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.StatusCode;
import tech.ydb.table.impl.BaseSession;
import tech.ydb.table.rpc.TableRpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
@ThreadSafe
abstract class StatefulSession extends BaseSession {
    private static final Logger logger = LoggerFactory.getLogger(SessionPool.class);

    private final Clock clock;
    private final AtomicReference<State> state;

    protected StatefulSession(String id, Clock clock, TableRpc tableRpc, boolean keepQueryText) {
        super(id, tableRpc, keepQueryText);
        this.clock = clock;
        this.state = new AtomicReference<>(new State(Status.IDLE, clock.instant()));
    }

    @Override
    protected void updateSessionState(Throwable th, StatusCode code, boolean gracefulShutdown) {
        State current = state.get();
        while (!state.compareAndSet(current, current.updated(clock.instant(), th, code, gracefulShutdown))) {
            current = state.get();
        }
        if (logger.isTraceEnabled()) {
            logger.trace("{} updated => {}, {}", this, state.get().status, state.get().lastUpdate);
        }
    }

    public State state() {
        return state.get();
    }

    private boolean switchState(State current, State next) {
        return next != null && state.compareAndSet(current, next);
    }

    private enum Status {
        IDLE,
        ACTIVE,
        NEED_SHUTDOWN,
        KEEPALIVE,
        BROKEN;
    }

    public class State {
        private final Status status;
        private final Instant lastActive;
        private final Instant lastUpdate;

        private State(Status status, Instant now) {
            this.status = status;
            this.lastActive = now;
            this.lastUpdate = now;
        }

        private State(Status status, Instant lastActive, Instant lastUpdate) {
            this.status = status;
            this.lastActive = lastActive;
            this.lastUpdate = lastUpdate;
        }

        public Instant lastActive() {
            return this.lastActive;
        }
        public Instant lastUpdate() {
            return this.lastUpdate;
        }

        public boolean needShutdown() {
            return this.status == Status.BROKEN || this.status == Status.NEED_SHUTDOWN;
        }

        public boolean switchToActive(Instant now) {
            return switchState(this, nextState(Status.ACTIVE, now));
        }

        public boolean switchToKeepAlive(Instant now) {
            return switchState(this, nextState(Status.KEEPALIVE, now));
        }

        public boolean switchToIdle(Instant now) {
            return switchState(this, nextState(Status.IDLE, now));
        }

        public boolean switchToBroken(Instant now) {
            return switchState(this, nextState(Status.BROKEN, now));
        }

        private State updated(Instant now, Throwable th, StatusCode code, boolean shutdownHint) {
            // Broken state never will be updated
            if (status == Status.BROKEN) {
                return this;
            }

            // Check problems
            boolean broken = th != null
                    || (code.isTransportError() && code != StatusCode.CLIENT_RESOURCE_EXHAUSTED)
                    || code == StatusCode.BAD_SESSION
                    || code == StatusCode.SESSION_BUSY
                    || code == StatusCode.INTERNAL_ERROR;

            // and if we found it state switch to broken status
            if (broken) {
                return new State(Status.BROKEN, lastActive, now);
            }

            if (status == Status.NEED_SHUTDOWN) {
                return new State(status, now);
            }

            if (status == Status.ACTIVE) {
                if (shutdownHint) {
                    return new State(Status.NEED_SHUTDOWN, now);
                }
                return new State(status, now);
            }

            return new State(status, lastActive, now);
        }

        private State nextState(Status nextStatus, Instant now) {
            // Broken and need shutdown states never will be changed
            if (status == Status.BROKEN || status == Status.NEED_SHUTDOWN) {
                return null;
            }

            if (nextStatus == Status.BROKEN) {
                return new State(Status.BROKEN, lastActive, now);
            }

            if (nextStatus == Status.ACTIVE) {
                return new State(nextStatus, now);
            }

            return new State(nextStatus, lastActive, now);
        }
    }
}
