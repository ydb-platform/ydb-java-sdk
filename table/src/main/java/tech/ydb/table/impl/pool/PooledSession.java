package tech.ydb.table.impl.pool;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import tech.ydb.core.StatusCode;
import tech.ydb.table.impl.BaseSession;
import tech.ydb.table.rpc.TableRpc;

/**
 * @author Aleksandr Gorshenin
 */
class PooledSession extends BaseSession {
    enum PooledState {
        IDLE,
        BROKEN,
        ACTIVE,
        DISCONNECTED,
    }

    private static final AtomicReferenceFieldUpdater<PooledSession, PooledState> stateUpdater =
        AtomicReferenceFieldUpdater.newUpdater(PooledSession.class, PooledState.class, "state");
    
    private final Consumer<PooledSession> destroyer;

    @SuppressWarnings("unused")
    private volatile PooledState state = PooledState.IDLE;
    
    PooledSession(String id, TableRpc tableRpc, boolean keepQueryText, Consumer<PooledSession> destroyer) {
        super(id, tableRpc, keepQueryText);
        this.destroyer = destroyer;
    }
    
    @Override
    public void close() {
        destroyer.accept(this);
    }

    private PooledState getState() {
        return stateUpdater.get(this);
    }

    private boolean switchState(PooledState from, PooledState to) {
        return stateUpdater.compareAndSet(this, from, to);
    }
    
    boolean tryChangeToActive() {
        return switchState(PooledState.IDLE, PooledState.ACTIVE);
    }
    
    boolean tryChangeToIdle() {
        return switchState(PooledState.ACTIVE, PooledState.IDLE);
    }

    boolean tryRestoreToIdle() {
        return switchState(PooledState.DISCONNECTED, PooledState.IDLE);
    }

    @Override
    protected void updateSessionState(Throwable t, StatusCode code) {
        PooledState oldState = getState();
        if (t != null) {
            switchState(oldState, PooledState.BROKEN);
            return;
        }

        if (code.isTransportError() && code != StatusCode.CLIENT_RESOURCE_EXHAUSTED) {
            switchState(oldState, PooledState.DISCONNECTED);
        } else if (code == StatusCode.BAD_SESSION) {
            switchState(oldState, PooledState.BROKEN);
        } else if (code == StatusCode.SESSION_BUSY) {
            switchState(oldState, PooledState.BROKEN);
        } else if (code == StatusCode.INTERNAL_ERROR) {
            switchState(oldState, PooledState.BROKEN);
        }
    }

    @Override
    public String toString() {
        return "PooledSession{" + getId() + "}";
    }
}
