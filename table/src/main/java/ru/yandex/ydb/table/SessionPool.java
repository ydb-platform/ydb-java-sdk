package ru.yandex.ydb.table;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.table.SessionImpl.State;
import ru.yandex.ydb.table.rpc.TableRpc;


/**
 * @author Sergey Polovko
 */
final class SessionPool {

    private final ArrayList<PooledSession> idleSessions = new ArrayList<>();
    private final int limit;

    SessionPool(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
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

    SessionImpl newSession(String id, TableRpc tableRpc, OperationsTray operationsTray) {
        return new PooledSession(id, tableRpc, operationsTray);
    }

    boolean releaseSession(Session session) {
        if (session instanceof PooledSession && ((PooledSession) session).switchState(State.IN_USE, State.IDLE)) {
            synchronized (idleSessions) {
                if (idleSessions.size() < limit) {
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
        PooledSession(String id, TableRpc tableRpc, OperationsTray operationsTray) {
            super(id, tableRpc, operationsTray);
        }
    }

}
