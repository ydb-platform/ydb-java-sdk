package tech.ydb.table.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import tech.ydb.core.Result;
import tech.ydb.table.Session;
import tech.ydb.table.impl.SessionImpl.State;
import tech.ydb.table.rpc.TableRpc;


/**
 * @author Sergey Polovko
 */
final class SessionPool {

    private final ArrayList<PooledSession> idleSessions = new ArrayList<>();
    private final SessionPoolOptions options;

    SessionPool(SessionPoolOptions options) {
        this.options = options;
    }

    CompletableFuture<Result<Session>> getOrSession(Supplier<CompletableFuture<Result<Session>>> sessionFactory) {
        PooledSession session = null;
        synchronized (idleSessions) {
            if (!idleSessions.isEmpty()) {
                session = idleSessions.remove(idleSessions.size() - 1);
                session.switchState(State.IDLE, State.IN_USE);
            }
        }

        if (session != null) {
            return CompletableFuture.completedFuture(Result.success(session));
        }

        return sessionFactory.get();
    }

    SessionImpl newSession(String id, TableRpc tableRpc) {
        return new PooledSession(id, tableRpc);
    }

    boolean releaseSession(Session session) {
        if (session instanceof PooledSession && ((PooledSession) session).switchState(State.IN_USE, State.IDLE)) {
            synchronized (idleSessions) {
                if (idleSessions.size() < options.getMaxSize()) {
                    idleSessions.add((PooledSession) session);
                    return true;
                }
            }
        }
        return false;
    }

    List<Session> close() {
        synchronized (idleSessions) {
            List<Session> sessions = new ArrayList<>(idleSessions);
            idleSessions.clear();
            return sessions;
        }
    }

    /**
     * POOLED SESSION
     */
    private static class PooledSession extends SessionImpl {
        PooledSession(String id, TableRpc tableRpc) {
            super(id, tableRpc);
        }
    }

}
