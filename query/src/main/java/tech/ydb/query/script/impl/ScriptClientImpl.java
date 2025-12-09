package tech.ydb.query.script.impl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import com.google.protobuf.Duration;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.script.ScriptClient;
import tech.ydb.query.script.ScriptRpc;
import tech.ydb.query.script.result.ScriptResultPart;
import tech.ydb.query.script.settings.ExecuteScriptSettings;
import tech.ydb.query.script.settings.FetchScriptSettings;
import tech.ydb.query.script.settings.FindScriptSettings;
import tech.ydb.query.settings.QueryExecMode;
import tech.ydb.query.settings.QueryStatsMode;
import tech.ydb.table.query.Params;

/**
 * Default implementation of {@link ScriptClient} using {@link ScriptRpc} for RPC calls.
 * <p>
 * Handles script execution lifecycle: starting scripts, polling their status,
 * and retrieving result sets in streaming fashion.
 *
 * <p>Author: Evgeny Kuvardin
 */
public class ScriptClientImpl implements ScriptClient {

    private final ScriptRpc scriptRpc;

    ScriptClientImpl(ScriptRpc scriptRpc) {
        this.scriptRpc = scriptRpc;
    }

    public static ScriptClient newClient(@WillNotClose GrpcTransport transport) {
        return new ScriptClientImpl(ScriptRpcImpl.useTransport(transport));
    }

    @Override
    public CompletableFuture<Operation<Status>> findQueryScript(String operationId, FindScriptSettings settings) {
        return scriptRpc.getOperation(operationId);
    }

    @Override
    public CompletableFuture<Operation<Status>> startQueryScript(String query,
                                                                 Params params,
                                                                 ExecuteScriptSettings settings) {
        YdbQuery.ExecuteScriptRequest.Builder request = YdbQuery.ExecuteScriptRequest.newBuilder()
                .setExecMode(mapExecMode(settings.getExecMode()))
                .setStatsMode(mapStatsMode(settings.getStatsMode()))
                .setScriptContent(YdbQuery.QueryContent.newBuilder()
                        .setSyntax(YdbQuery.Syntax.SYNTAX_YQL_V1)
                        .setText(query)
                        .build());

        java.time.Duration ttl = settings.getTtl();
        if (ttl != null) {
            request.setResultsTtl(Duration.newBuilder().setNanos(settings.getTtl().getNano()));
        }

        String resourcePool = settings.getResourcePool();
        if (resourcePool != null && !resourcePool.isEmpty()) {
            request.setPoolId(resourcePool);
        }

        request.putAllParameters(params.toPb());

        GrpcRequestSettings options = makeGrpcRequestSettings(settings);

        return scriptRpc.executeScript(request.build(), options);
    }

    @Override
    public CompletableFuture<Result<ScriptResultPart>> fetchQueryScriptResult(@Nonnull Operation<Status> operation,
                                                                              @Nullable ScriptResultPart previous,
                                                                              FetchScriptSettings settings) {
        YdbQuery.FetchScriptResultsRequest.Builder requestBuilder = YdbQuery.FetchScriptResultsRequest.newBuilder();

        if (previous != null && previous.getNextFetchToken() != null) {
            requestBuilder.setFetchToken(previous.getNextFetchToken());
        }

        if (settings.getRowsLimit() > 0) {
            requestBuilder.setRowsLimit(settings.getRowsLimit());
        }

        requestBuilder.setOperationId(operation.getId());

        if (settings.getSetResultSetIndex() >= 0) {
            requestBuilder.setResultSetIndex(settings.getSetResultSetIndex());
        }

        GrpcRequestSettings options = makeGrpcRequestSettings(settings);

        return scriptRpc.fetchScriptResults(requestBuilder.build(), options)
                .thenApply(p -> p.map(ScriptResultPart::new));
    }

    private GrpcRequestSettings makeGrpcRequestSettings(BaseRequestSettings settings) {
        String traceId = settings.getTraceId() == null ? UUID.randomUUID().toString() : settings.getTraceId();
        return GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withTraceId(traceId)
                .build();
    }

    private static YdbQuery.ExecMode mapExecMode(QueryExecMode mode) {
        switch (mode) {
            case EXECUTE:
                return YdbQuery.ExecMode.EXEC_MODE_EXECUTE;
            case EXPLAIN:
                return YdbQuery.ExecMode.EXEC_MODE_EXPLAIN;
            case PARSE:
                return YdbQuery.ExecMode.EXEC_MODE_PARSE;
            case VALIDATE:
                return YdbQuery.ExecMode.EXEC_MODE_VALIDATE;

            case UNSPECIFIED:
            default:
                return YdbQuery.ExecMode.EXEC_MODE_UNSPECIFIED;
        }
    }

    private static YdbQuery.StatsMode mapStatsMode(QueryStatsMode mode) {
        switch (mode) {
            case NONE:
                return YdbQuery.StatsMode.STATS_MODE_NONE;
            case BASIC:
                return YdbQuery.StatsMode.STATS_MODE_BASIC;
            case FULL:
                return YdbQuery.StatsMode.STATS_MODE_FULL;
            case PROFILE:
                return YdbQuery.StatsMode.STATS_MODE_PROFILE;

            case UNSPECIFIED:
            default:
                return YdbQuery.StatsMode.STATS_MODE_UNSPECIFIED;
        }
    }
}
