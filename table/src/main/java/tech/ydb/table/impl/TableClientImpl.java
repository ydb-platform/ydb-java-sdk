package tech.ydb.table.impl;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import tech.ydb.core.Result;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.YdbTable;
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

    private final int queryCacheSize;
    private final boolean keepQueryText;

    TableClientImpl(TableClientBuilderImpl builder) {
        this.tableRpc = builder.tableRpc;
        this.sessionPool = builder.sessionPoolOptions.getMaxSize() != 0
            ? new SessionPool(this, builder.sessionPoolOptions)
            : null;
        this.operationTray = tableRpc.getOperationTray();

        this.queryCacheSize = builder.queryCacheSize;
        this.keepQueryText = builder.keepQueryText;
    }

    @Override
    public CompletableFuture<Result<Session>> createSession(CreateSessionSettings settings) {
        return createSessionImpl(settings);
    }

    CompletableFuture<Result<Session>> createSessionImpl(CreateSessionSettings settings) {
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
                    result -> new SessionImpl(result.getSessionId(), tableRpc, sessionPool, queryCacheSize, keepQueryText));
            });
    }

    @Override
    public CompletableFuture<Result<Session>> getOrCreateSession() {
        if (sessionPool == null) {
            return createSessionImpl(new CreateSessionSettings());
        }
        return sessionPool.acquire()
            .handle((s, t) -> {
                if (t == null) return Result.success(s);
                return Result.error("cannot acquire session from pool", t);
            });
    }

    @Override
    public void close() {
        if (sessionPool != null) {
            sessionPool.close();
        }
        tableRpc.close();
    }
}
