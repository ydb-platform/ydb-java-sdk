package tech.ydb.table.impl;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.YdbTable;
import tech.ydb.table.impl.SessionImpl.State;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.CreateSessionSettings;


/**
 * @author Sergey Polovko
 */
final class TableClientImpl implements TableClient {

    private final TableRpc tableRpc;
    @Nullable
    private final SessionPool sessionPool;
    private final OperationTray operationTray;

    TableClientImpl(TableClientBuilderImpl builder) {
        this.tableRpc = builder.tableRpc;
        this.sessionPool = builder.sessionPoolOptions.getMaxSize() != 0
            ? new SessionPool(builder.sessionPoolOptions)
            : null;
        this.operationTray = tableRpc.getOperationTray();
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
                return operationTray.waitResult(
                    response.expect("createSession()").getOperation(),
                    YdbTable.CreateSessionResult.class,
                    result -> mapSessionResult(result, initialState));
            });
    }

    private Session mapSessionResult(YdbTable.CreateSessionResult result, State initialState) {
        if (sessionPool == null || initialState == State.STANDALONE) {
            return new SessionImpl(result.getSessionId(), tableRpc);
        }

        SessionImpl session = sessionPool.newSession(result.getSessionId(), tableRpc);
        session.switchState(State.STANDALONE, initialState);
        return session;
    }

    @Override
    public CompletableFuture<Result<Session>> getOrCreateSession(CreateSessionSettings settings) {
        if (sessionPool == null) {
            return createSession(settings);
        }
        return sessionPool.getOrSession(() -> createSessionImpl(settings, State.IN_USE));
    }

    @Override
    public CompletableFuture<Status> releaseSession(Session session) {
        if (sessionPool != null && sessionPool.releaseSession(session)) {
            return CompletableFuture.completedFuture(Status.SUCCESS);
        }

        return session.close();
    }

    @Override
    public void close() {
        if (sessionPool != null) {
            for (Session session : sessionPool.close()) {
                session.close();
            }
        }
        tableRpc.close();
    }
}
