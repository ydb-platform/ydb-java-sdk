package tech.ydb.table.impl.pool;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.concurrent.ThreadSafe;
import tech.ydb.core.StatusCode;
import tech.ydb.table.impl.BaseSession;
import tech.ydb.table.rpc.TableRpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
@ThreadSafe
abstract class StatefulSession extends BaseSession {
    private final AtomicReference<State> state;

    protected StatefulSession(String id, TableRpc tableRpc, boolean keepQueryText) {
        super(id, tableRpc, keepQueryText);
        Instant now = Instant.now();
        this.state = new AtomicReference<>(new State(Status.IDLE, now, now));
    }

    @Override
    protected void updateSessionState(Throwable th, StatusCode code) {
        State current = state.get();
        while (!state.compareAndSet(current, current.updated(Instant.now(), th, code))) {
            current = state.get();
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
        BROKEN;
    }

    public class State {
        private final Status status;
        private final Instant lastActive;
        private final Instant lastUpdate;
        
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
            return this.status == Status.BROKEN;
        }
        
        public boolean switchToActive(Instant now) {
            return switchState(this, nextState(Status.ACTIVE, now));
        }

        public boolean switchToIdle(Instant now) {
            return switchState(this, nextState(Status.IDLE, now));
        }

        public boolean switchToBroken(Instant now) {
            return switchState(this, nextState(Status.BROKEN, now));
        }

        private State updated(Instant now, Throwable tw, StatusCode code) {
            // Broken state never will be updated
            if (status == Status.BROKEN) {
                return this;
            }

            // Check problems
            boolean broken = tw != null
                    || (code.isTransportError() && code != StatusCode.CLIENT_RESOURCE_EXHAUSTED)
                    || code == StatusCode.BAD_SESSION
                    || code == StatusCode.SESSION_BUSY
                    || code == StatusCode.INTERNAL_ERROR;

            // and if we found it state switch to broken status
            if (broken) {
                return new State(Status.BROKEN, lastActive, now);
            }

            if (status == Status.ACTIVE) {
                return new State(status, now, now);
            }

            return new State(status, lastActive, now);
        }
        
        private State nextState(Status nextStatus, Instant now) {
            // Broken state never will be changed
            if (status == Status.BROKEN) {
                return null;
            }
            
            if (nextStatus == Status.BROKEN) {
                return new State(Status.BROKEN, lastActive, now);
            }
            
            //switching to the same status is forbidden
            if (nextStatus == status) {
                return null;
            }
            
            if (nextStatus == Status.ACTIVE) {
                return new State(nextStatus, now, now);
            }
            
            return new State(nextStatus, lastActive, now);
        }
    }
}
