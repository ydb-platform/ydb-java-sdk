package tech.ydb.table.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import tech.ydb.core.Result;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.core.utils.Async;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.YdbTable;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.CreateSessionSettings;
import tech.ydb.table.stats.SessionPoolStats;
import tech.ydb.table.utils.OperationParamUtils;
import tech.ydb.table.utils.RequestSettingsUtils;
import io.grpc.Metadata;


/**
 * @author Sergey Polovko
 */
final class TableClientImpl implements TableClient {
    private final static String SERVER_BALANCER_HINT = "session-balancer";

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
        return createSessionImpl(settings, null);
    }

    @Override
    public SessionPoolStats getSessionPoolStats() {
        return sessionPool.getStats();
    }

    CompletableFuture<Result<Session>> createSessionImpl(CreateSessionSettings settings, @Nullable SessionPool sessionPool) {
        YdbTable.CreateSessionRequest request = YdbTable.CreateSessionRequest.newBuilder()
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .build();

        // Use server-side session balancer
        final GrpcRequestSettings grpcRequestSettings = GrpcRequestSettings.newBuilder()
                .withDeadlineAfter(RequestSettingsUtils.calculateDeadlineAfter(settings))
                .withExtraHeaders(getClientCapabilities())
                .build();

        return tableRpc.createSession(request, grpcRequestSettings)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationTray.waitResult(
                    response.expect("createSession()").getOperation(),
                    YdbTable.CreateSessionResult.class,
                    result -> new SessionImpl(result.getSessionId(), tableRpc, sessionPool, queryCacheSize, keepQueryText),
                        grpcRequestSettings);
            });
    }

    @Override
    public CompletableFuture<Result<Session>> getOrCreateSession(Duration timeout) {
        if (sessionPool == null) {
            return createSessionImpl(new CreateSessionSettings().setTimeout(timeout), null);
        }
        return sessionPool.acquire(timeout)
            .handle((s, t) -> {
                if (t == null) return Result.success(s);
                Throwable unwrapped = Async.unwrapCompletionException(t);
                if (unwrapped instanceof UnexpectedResultException) {
                    return Result.fail((UnexpectedResultException) unwrapped);
                } else {
                    return Result.error("cannot acquire session from pool", unwrapped);
                }
            });
    }

    @Override
    public void close() {
        if (sessionPool != null) {
            sessionPool.close();
        }
        tableRpc.close();
    }

    private Metadata getClientCapabilities() {
        Metadata metadata = new Metadata();
        metadata.put(YdbHeaders.YDB_CLIENT_CAPABILITIES, SERVER_BALANCER_HINT);
        return metadata;
    }
}
