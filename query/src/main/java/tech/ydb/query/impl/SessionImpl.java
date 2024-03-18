package tech.ydb.query.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.google.protobuf.TextFormat;
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
import tech.ydb.query.QueryStream;
import tech.ydb.query.QueryTransaction;
import tech.ydb.query.QueryTx;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.query.result.QueryStats;
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
abstract class SessionImpl implements QuerySession {
    private static final Logger logger = LoggerFactory.getLogger(QuerySession.class);

    private static final StatusExtract<YdbQuery.CreateSessionResponse> CREATE_SESSION = StatusExtract.of(
            YdbQuery.CreateSessionResponse::getStatus, YdbQuery.CreateSessionResponse::getIssuesList
    );

    private static final StatusExtract<YdbQuery.DeleteSessionResponse> DELETE_SESSION = StatusExtract.of(
            YdbQuery.DeleteSessionResponse::getStatus, YdbQuery.DeleteSessionResponse::getIssuesList
    );

    private final QueryServiceRpc rpc;
    private final String sessionId;
    private final long nodeID;
    private final boolean isTraceEnabled;
    private final AtomicReference<TransactionImpl> transaction;

    SessionImpl(QueryServiceRpc rpc, YdbQuery.CreateSessionResponse response) {
        this.rpc = rpc;
        this.sessionId = response.getSessionId();
        this.nodeID = getNodeBySessionId(response.getSessionId(), response.getNodeId());
        this.isTraceEnabled = logger.isTraceEnabled();
        this.transaction = new AtomicReference<>(new TransactionImpl(QueryTx.SERIALIZABLE_RW, null));
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

    @Override
    public String getId() {
        return this.sessionId;
    }

    @Override
    public String toString() {
        return "QuerySessionStream[" + sessionId + "]";
    }

    @Override
    public QueryTransaction currentTransaction() {
        return transaction.get();
    }

    @Override
    public QueryTransaction createNewTransaction(QueryTx txMode) {
        return updateTransaction(new TransactionImpl(txMode, null));
    }

    public abstract void updateSessionState(Status status);

    @Override
    public CompletableFuture<Result<QueryTransaction>> beginTransaction(QueryTx tx, BeginTransactionSettings settings) {
        YdbQuery.BeginTransactionRequest request = YdbQuery.BeginTransactionRequest.newBuilder()
                .setSessionId(sessionId)
                .setTxSettings(TxControl.txSettings(tx))
                .build();

        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
        return rpc.beginTransaction(request, grpcSettings).thenApply(result -> {
            updateSessionState(result.getStatus());
            return result.map(resp -> updateTransaction(new TransactionImpl(tx, resp.getTxMeta().getId())));
        });
    }

    private QueryTransaction updateTransaction(TransactionImpl newTx) {
        TransactionImpl oldTx = transaction.getAndSet(newTx);
        if (oldTx != null && oldTx.isActive()) {
            logger.warn("{} lost active transaction {}!!", this, oldTx);
        }
        return newTx;
    }

    GrpcReadStream<Status> attach(AttachSessionSettings settings) {
        YdbQuery.AttachSessionRequest request = YdbQuery.AttachSessionRequest.newBuilder()
                .setSessionId(sessionId)
                .build();
        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
        return new ProxyReadStream<>(rpc.attachSession(request, grpcSettings), (message, promise, observer) -> {
            logger.trace("session '{}' got attach stream message {}", sessionId, TextFormat.shortDebugString(message));
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

    GrpcReadStream<YdbQuery.ExecuteQueryResponsePart> createGrpcStream(
            String query, YdbQuery.TransactionControl tx, Params prms, ExecuteQuerySettings settings
    ) {
        YdbQuery.ExecuteQueryRequest.Builder requestBuilder = YdbQuery.ExecuteQueryRequest.newBuilder()
                .setSessionId(sessionId)
                .setExecMode(mapExecMode(settings.getExecMode()))
                .setStatsMode(mapStatsMode(settings.getStatsMode()))
                .setQueryContent(YdbQuery.QueryContent.newBuilder()
                        .setSyntax(YdbQuery.Syntax.SYNTAX_YQL_V1)
                        .setText(query)
                        .build()
                )
                .putAllParameters(prms.toPb());

        if (tx != null) {
            requestBuilder.setTxControl(tx);
        }

        YdbQuery.ExecuteQueryRequest request = requestBuilder.build();
        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
        return rpc.executeQuery(request, grpcSettings);
    }

    @Override
    public QueryStream createQuery(String query, QueryTx tx, Params prms, ExecuteQuerySettings settings) {
        YdbQuery.TransactionControl tc = TxControl.txModeCtrl(tx, true);
        return new StreamImpl(createGrpcStream(query, tc, prms, settings)) {
            @Override
            void handleTxMeta(YdbQuery.TransactionMeta meta) {
                String txID = meta == null ? null : meta.getId();
                if (txID != null && !txID.isEmpty()) {
                    logger.warn("{} got unexpected transaction id {}", SessionImpl.this, txID);
                }
            }
        };
    }

    public CompletableFuture<Result<YdbQuery.DeleteSessionResponse>> delete(DeleteSessionSettings settings) {
        YdbQuery.DeleteSessionRequest request = YdbQuery.DeleteSessionRequest.newBuilder()
                .setSessionId(sessionId)
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

    abstract class StreamImpl implements QueryStream {
        private final GrpcReadStream<YdbQuery.ExecuteQueryResponsePart> grpcStream;

        StreamImpl(GrpcReadStream<YdbQuery.ExecuteQueryResponsePart> grpcStream) {
            this.grpcStream = grpcStream;
        }

        abstract void handleTxMeta(YdbQuery.TransactionMeta meta);

        @Override
        public CompletableFuture<Result<QueryInfo>> execute(PartsHandler handler) {
            final CompletableFuture<Result<QueryInfo>> result = new CompletableFuture<>();
            final AtomicReference<QueryStats> stats = new AtomicReference<>();
            grpcStream.start(msg -> {
                if (isTraceEnabled) {
                    logger.trace("{} got stream message {}", SessionImpl.this, TextFormat.shortDebugString(msg));
                }
                Status status = Status.of(
                        StatusCode.fromProto(msg.getStatus()),
                        null,
                        Issue.fromPb(msg.getIssuesList())
                );

                updateSessionState(status);

                if (!status.isSuccess()) {
                    handleTxMeta(null);
                    result.complete(Result.fail(status));
                    return;
                }

                if (msg.hasTxMeta()) {
                    handleTxMeta(msg.getTxMeta());
                }
                if (msg.hasExecStats()) {
                    QueryStats old = stats.getAndSet(new QueryStats(msg.getExecStats()));
                    if (old != null) {
                        logger.warn("{} got lost previous exec stats {}", SessionImpl.this, old);
                    }
                }
                if (msg.hasResultSet()) {
                    long index = msg.getResultSetIndex();
                    if (handler != null) {
                        handler.onNextPart(new QueryResultPart(index, msg.getResultSet()));
                    } else {
                        logger.warn("{} got lost result set part with index {}", SessionImpl.this, index);
                    }
                }
            }).whenComplete((status, th) -> {
                if (th != null) {
                    result.completeExceptionally(th);
                }
                if (status != null) {
                    if (status.isSuccess()) {
                        result.complete(Result.success(new QueryInfo(stats.get()), status));
                    } else {
                        result.complete(Result.fail(status));
                    }
                }
            });
            return result;
        }

        @Override
        public void cancel() {
            grpcStream.cancel();
        }
    }

    class TransactionImpl implements QueryTransaction {
        private final QueryTx txMode;
        private final AtomicReference<String> txId;

        TransactionImpl(QueryTx tx, String txID) {
            this.txMode = tx;
            this.txId = new AtomicReference<>(txID);
        }

        @Override
        public String getId() {
            return txId.get();
        }

        @Override
        public QueryTx getQueryTx() {
            return txMode;
        }

        @Override
        public QuerySession getSession() {
            return SessionImpl.this;
        }

        @Override
        public QueryStream createQuery(String query, boolean commitAtEnd, Params prms, ExecuteQuerySettings settings) {
            final String currentId = txId.get();
            YdbQuery.TransactionControl tc = currentId != null
                    ? TxControl.txIdCtrl(currentId, commitAtEnd)
                    : TxControl.txModeCtrl(txMode, commitAtEnd);

            return new StreamImpl(createGrpcStream(query, tc, prms, settings)) {
                @Override
                void handleTxMeta(YdbQuery.TransactionMeta meta) {
                    String newId = meta == null || meta.getId() == null || meta.getId().isEmpty() ? null : meta.getId();
                    if (!txId.compareAndSet(currentId, newId)) {
                        logger.warn("{} lost transaction meta id {}", SessionImpl.this, newId);
                    }
                }
            };
        }

        @Override
        public CompletableFuture<Result<QueryInfo>> commit(CommitTransactionSettings settings) {
            final String trasactionId = txId.get();
            if (trasactionId == null) {
                Issue issue = Issue.of("Transaction is not started", Issue.Severity.WARNING);
                Result<QueryInfo> res = Result.success(new QueryInfo(null), Status.of(StatusCode.SUCCESS, null, issue));
                return CompletableFuture.completedFuture(res);
            }

            YdbQuery.CommitTransactionRequest request = YdbQuery.CommitTransactionRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setTxId(trasactionId)
                    .build();
            GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
            return rpc.commitTransaction(request, grpcSettings).thenApply(res -> {
                updateSessionState(res.getStatus());
                if (!txId.compareAndSet(trasactionId, null)) {
                    logger.warn("{} lost commit response for transaction {}", SessionImpl.this, trasactionId);
                }
                // TODO: CommitTrasactionResponse must contain exec_stats
                return res.map(resp -> new QueryInfo(null));
            });
        }

        @Override
        public CompletableFuture<Status> rollback(RollbackTransactionSettings settings) {
            final String trasactionId = txId.get();

            if (trasactionId == null) {
                Issue issue = Issue.of("Transaction is not started", Issue.Severity.WARNING);
                return CompletableFuture.completedFuture(Status.of(StatusCode.SUCCESS, null, issue));
            }

            YdbQuery.RollbackTransactionRequest request = YdbQuery.RollbackTransactionRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setTxId(trasactionId)
                    .build();
            GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
            return rpc.rollbackTransaction(request, grpcSettings).thenApply(result -> {
                updateSessionState(result.getStatus());
                if (!txId.compareAndSet(trasactionId, null)) {
                    logger.warn("{} lost rollback response for transaction {}", SessionImpl.this, trasactionId);
                }
                return result.getStatus();
            });
        }
    }
}
