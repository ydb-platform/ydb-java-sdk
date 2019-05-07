package ru.yandex.ydb.table;

import java.util.concurrent.CompletableFuture;

import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.Status;
import ru.yandex.ydb.table.SessionImpl.State;
import ru.yandex.ydb.table.rpc.TableRpc;
import ru.yandex.ydb.table.settings.CreateSessionSettings;


/**
 * @author Sergey Polovko
 */
final class TableClientImpl implements TableClient {

    private final TableRpc tableRpc;
    private final OperationsTray operationsTray;
    private final SessionPool sessionPool;

    TableClientImpl(TableRpc tableRpc, OperationsTray operationsTray, SessionPool sessionPool) {
        this.tableRpc = tableRpc;
        this.operationsTray = operationsTray;
        this.sessionPool = sessionPool;
    }

    @Override
    public CompletableFuture<Result<Session>> createSession(CreateSessionSettings settings) {
        return createSessionImpl(settings, State.STANDALONE);
    }

    private CompletableFuture<Result<Session>> createSessionImpl(CreateSessionSettings settings, State initialState) {
        YdbTable.CreateSessionRequest request = YdbTable.CreateSessionRequest.newBuilder()
            .build();

        return tableRpc.createSession(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationsTray.waitResult(
                    response.expect("createSession()").getOperation(),
                    YdbTable.CreateSessionResult.class,
                    result -> mapSessionResult(result, initialState));
            });
    }

    private Session mapSessionResult(YdbTable.CreateSessionResult result, State initialState) {
        if (initialState == State.STANDALONE) {
            return new SessionImpl(result.getSessionId(), tableRpc, operationsTray);
        }

        SessionImpl session = sessionPool.newSession(result.getSessionId(), tableRpc, operationsTray);
        session.switchState(State.STANDALONE, initialState);
        return session;
    }

    @Override
    public CompletableFuture<Result<Session>> getOrCreateSession(CreateSessionSettings settings) {
        if (sessionPool.getLimit() == 0) {
            return createSession(settings);
        }
        return sessionPool.getOrSession(() -> createSessionImpl(settings, State.IN_USE));
    }

    @Override
    public CompletableFuture<Status> releaseSession(Session session) {
        if (sessionPool.releaseSession(session)) {
            return CompletableFuture.completedFuture(Status.SUCCESS);
        }

        return session.close();
    }

    @Override
    public void close() {
        for (Session session : sessionPool.close()) {
            session.close();
        }
    }
}
