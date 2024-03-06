package tech.ydb.query.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.core.impl.call.ProxyReadStream;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.core.utils.URITools;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryTx;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.query.settings.AttachSessionSettings;
import tech.ydb.query.settings.BeginTransactionSettings;
import tech.ydb.query.settings.CommitTransactionSettings;
import tech.ydb.query.settings.CreateSessionSettings;
import tech.ydb.query.settings.DeleteSessionSettings;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.settings.QueryExecMode;
import tech.ydb.query.settings.QueryStatsMode;
import tech.ydb.query.settings.RollbackTransactionSettings;
import tech.ydb.table.query.Params;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class QuerySessionImpl implements QuerySession {
    private static final Logger logger = LoggerFactory.getLogger(QuerySession.class);

    private static final StatusExtract<YdbQuery.CreateSessionResponse> CREATE_SESSION = StatusExtract.of(
            YdbQuery.CreateSessionResponse::getStatus, YdbQuery.CreateSessionResponse::getIssuesList
    );

    private static final StatusExtract<YdbQuery.DeleteSessionResponse> DELETE_SESSION = StatusExtract.of(
            YdbQuery.DeleteSessionResponse::getStatus, YdbQuery.DeleteSessionResponse::getIssuesList
    );

    private final QueryServiceRpc rpc;
    private final String id;
    private final long nodeID;

    QuerySessionImpl(QueryServiceRpc rpc, YdbQuery.CreateSessionResponse response) {
        this.rpc = rpc;
        this.id = response.getSessionId();
        this.nodeID = getNodeBySessionId(response.getSessionId(), response.getNodeId());
    }

    private static Long getNodeBySessionId(String sessionId, long defaultValue) {
        try {
            Map<String, List<String>> params = URITools.splitQuery(new URI(sessionId));
            List<String> nodeParam = params.get("node_id");
            if (nodeParam != null && !nodeParam.isEmpty()) {
                return Long.parseUnsignedLong(nodeParam.get(0));
            }
        } catch (URISyntaxException | RuntimeException e) {
//            logger.debug("Failed to parse session_id for node_id: {}", e.toString());
        }
        return defaultValue;
    }

    public String getId() {
        return this.id;
    }

    public abstract void updateSessionState(Status status);

    GrpcReadStream<Status> attach(AttachSessionSettings settings) {
        YdbQuery.AttachSessionRequest request = YdbQuery.AttachSessionRequest.newBuilder()
                .setSessionId(id)
                .build();
        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
        return new ProxyReadStream<>(rpc.attachSession(request, grpcSettings), (message, promise, observer) -> {
            logger.trace("session '{}' got attach stream message {}", id, message);
            Status status = Status.of(
                    StatusCode.fromProto(message.getStatus()),
                    null,
                    Issue.fromPb(message.getIssuesList())
            );
            updateSessionState(status);
            observer.onNext(status);
        });
    }

    private GrpcRequestSettings makeGrpcRequestSettings(BaseRequestSettings settings) {
        return GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withPreferredNodeID((int) nodeID)
                .build();
    }

    private static YdbQuery.ExecMode mapExecMode(QueryExecMode mode) {
        switch (mode) {
            case EXECUTE: return YdbQuery.ExecMode.EXEC_MODE_EXECUTE;
            case EXPLAIN: return YdbQuery.ExecMode.EXEC_MODE_EXPLAIN;
            case PARSE: return YdbQuery.ExecMode.EXEC_MODE_PARSE;
            case VALIDATE: return YdbQuery.ExecMode.EXEC_MODE_VALIDATE;

            case UNSPECIFIED:
            default:
                return YdbQuery.ExecMode.EXEC_MODE_UNSPECIFIED;
        }
    }

    private static YdbQuery.StatsMode mapStatsMode(QueryStatsMode mode) {
        switch (mode) {
            case NONE: return YdbQuery.StatsMode.STATS_MODE_NONE;
            case BASIC: return YdbQuery.StatsMode.STATS_MODE_BASIC;
            case FULL: return YdbQuery.StatsMode.STATS_MODE_FULL;
            case PROFILE: return YdbQuery.StatsMode.STATS_MODE_PROFILE;

            case UNSPECIFIED:
            default:
                return YdbQuery.StatsMode.STATS_MODE_UNSPECIFIED;
        }
    }

    @Override
    public GrpcReadStream<QueryResultPart> executeQuery(String query, QueryTx tx, Params prms, ExecuteQuerySettings s) {
        YdbQuery.ExecuteQueryRequest.Builder request = YdbQuery.ExecuteQueryRequest.newBuilder()
                .setSessionId(id)
                .setExecMode(mapExecMode(s.getExecMode()))
                .setStatsMode(mapStatsMode(s.getStatsMode()))
                .setQueryContent(YdbQuery.QueryContent.newBuilder()
                        .setSyntax(YdbQuery.Syntax.SYNTAX_YQL_V1)
                        .setText(query)
                        .build()
                )
                .putAllParameters(prms.toPb());

        YdbQuery.TransactionControl tc = tx.toTxControlPb();
        if (tc != null) {
            request.setTxControl(tc);
        }

        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(s);
        return new ProxyReadStream<>(rpc.executeQuery(request.build(), grpcSettings), (message, promise, observer) -> {
            logger.trace("session '{}' got query stream message {}", id, message);
            Status status = Status.of(
                    StatusCode.fromProto(message.getStatus()),
                    null,
                    Issue.fromPb(message.getIssuesList())
            );

            updateSessionState(status);

            if (!status.isSuccess()) {
                promise.complete(status);
            } else {
                observer.onNext(new QueryResultPart(message));
            }
        });
    }

    @Override
    public CompletableFuture<Result<QueryTx.Id>> beginTransaction(QueryTx.Mode tx, BeginTransactionSettings settings) {
        YdbQuery.BeginTransactionRequest request = YdbQuery.BeginTransactionRequest.newBuilder()
                .setSessionId(id)
                .setTxSettings(tx.toTxSettingsPb())
                .build();

        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
        return rpc.beginTransaction(request, grpcSettings).thenApply(result -> {
            updateSessionState(result.getStatus());
            return result.map(TxId::new);
        });
    }

    @Override
    public CompletableFuture<Status> commitTransaction(QueryTx.Id tx, CommitTransactionSettings settings) {
        YdbQuery.CommitTransactionRequest request = YdbQuery.CommitTransactionRequest.newBuilder()
                .setSessionId(id)
                .setTxId(tx.txId())
                .build();
        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
        return rpc.commitTransaction(request, grpcSettings).thenApply(result -> {
            updateSessionState(result.getStatus());
            return result.getStatus();
        });
    }

    @Override
    public CompletableFuture<Status> rollbackTransaction(QueryTx.Id tx, RollbackTransactionSettings settings) {
        YdbQuery.RollbackTransactionRequest request = YdbQuery.RollbackTransactionRequest.newBuilder()
                .setSessionId(id)
                .setTxId(tx.txId())
                .build();
        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
        return rpc.rollbackTransaction(request, grpcSettings).thenApply(result -> {
            updateSessionState(result.getStatus());
            return result.getStatus();
        });
    }

    public CompletableFuture<Result<YdbQuery.DeleteSessionResponse>> delete(DeleteSessionSettings settings) {
        YdbQuery.DeleteSessionRequest request = YdbQuery.DeleteSessionRequest.newBuilder()
                .setSessionId(id)
                .build();

        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
        return rpc.deleteSession(request, grpcSettings).thenApply(DELETE_SESSION);
    }

    static CompletableFuture<Result<YdbQuery.CreateSessionResponse>> createSession(
            QueryServiceRpc rpc,
            CreateSessionSettings settings,
            boolean useServerBalancer) {
        YdbQuery.CreateSessionRequest request = YdbQuery.CreateSessionRequest.newBuilder()
                .build();

        Metadata metadata = new Metadata();
        if (useServerBalancer) {
            metadata.put(YdbHeaders.YDB_CLIENT_CAPABILITIES, "session-balancer");
        }

        GrpcRequestSettings grpcSettings = GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withExtraHeaders(metadata)
                .build();

        return rpc.createSession(request, grpcSettings).thenApply(CREATE_SESSION);
    }
}
