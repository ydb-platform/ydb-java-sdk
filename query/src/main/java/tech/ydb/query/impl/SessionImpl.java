package tech.ydb.query.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.google.protobuf.TextFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.common.transaction.impl.YdbTransactionImpl;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.impl.call.ProxyReadStream;
import tech.ydb.core.operation.StatusExtractor;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.core.utils.URITools;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryStream;
import tech.ydb.query.QueryTransaction;
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
    private static final String SERVER_BALANCER_HINT = "session-balancer";
    private static final Logger logger = LoggerFactory.getLogger(QuerySession.class);

    private static final StatusExtractor<YdbQuery.CreateSessionResponse> CREATE_SESSION = StatusExtractor.of(
            YdbQuery.CreateSessionResponse::getStatus, YdbQuery.CreateSessionResponse::getIssuesList
    );

    private static final StatusExtractor<YdbQuery.DeleteSessionResponse> DELETE_SESSION = StatusExtractor.of(
            YdbQuery.DeleteSessionResponse::getStatus, YdbQuery.DeleteSessionResponse::getIssuesList
    );

    private static final Status CANCELLED = Status.of(
            StatusCode.CLIENT_CANCELLED,
            Issue.of("Stream was cancelled by client, session will be removed", Issue.Severity.WARNING)
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
        this.transaction = new AtomicReference<>(new TransactionImpl(TxMode.SERIALIZABLE_RW, null));
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
    public QueryTransaction createNewTransaction(TxMode txMode) {
        return updateTransaction(new TransactionImpl(txMode, null));
    }

    public abstract void updateSessionState(Status status);

    @Override
    public CompletableFuture<Result<QueryTransaction>> beginTransaction(TxMode tx, BeginTransactionSettings settings) {
        YdbQuery.BeginTransactionRequest request = YdbQuery.BeginTransactionRequest.newBuilder()
                .setSessionId(sessionId)
                .setTxSettings(TxControl.txSettings(tx))
                .build();

        return rpc.beginTransaction(request, makeOptions(settings).build()).thenApply(result -> {
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
        GrpcRequestSettings grpcSettings = makeOptions(settings).build();
        return new ProxyReadStream<>(rpc.attachSession(request, grpcSettings), (message, promise, observer) -> {
            logger.trace("session '{}' got attach stream message {}", sessionId, TextFormat.shortDebugString(message));
            Status status = Status.of(StatusCode.fromProto(message.getStatus()), Issue.fromPb(message.getIssuesList()));
            updateSessionState(status);
            observer.onNext(status);
        });
    }

    private GrpcRequestSettings.Builder makeOptions(BaseRequestSettings settings) {
        String traceId = settings.getTraceId() == null ? UUID.randomUUID().toString() : settings.getTraceId();
        return GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withPreferredNodeID((int) nodeID)
                .withTraceId(traceId);
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
        YdbQuery.ExecuteQueryRequest.Builder request = YdbQuery.ExecuteQueryRequest.newBuilder()
                .setSessionId(sessionId)
                .setExecMode(mapExecMode(settings.getExecMode()))
                .setStatsMode(mapStatsMode(settings.getStatsMode()))
                .setConcurrentResultSets(settings.isConcurrentResultSets())
                .setQueryContent(YdbQuery.QueryContent.newBuilder()
                        .setSyntax(YdbQuery.Syntax.SYNTAX_YQL_V1)
                        .setText(query)
                        .build()
                )
                .putAllParameters(prms.toPb());

        String resourcePool = settings.getResourcePool();
        if (resourcePool != null && !resourcePool.isEmpty()) {
            request.setPoolId(resourcePool);
        }

        if (settings.getPartBytesLimit() >= 0) {
            request.setResponsePartLimitBytes(settings.getPartBytesLimit());
        }

        if (tx != null) {
            request.setTxControl(tx);
        }

        GrpcRequestSettings.Builder options = makeOptions(settings);
        if (settings.getGrpcFlowControl() != null) {
            options = options.withFlowControl(settings.getGrpcFlowControl());
        }

        return rpc.executeQuery(request.build(), options.build());
    }

    @Override
    public QueryStream createQuery(String query, TxMode tx, Params prms, ExecuteQuerySettings settings) {
        YdbQuery.TransactionControl tc = TxControl.txModeCtrl(tx, true);
        return new StreamImpl(createGrpcStream(query, tc, prms, settings)) {
            @Override
            void handleTxMeta(String txID) {
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

        return rpc.deleteSession(request, makeOptions(settings).build()).thenApply(DELETE_SESSION);
    }

    static CompletableFuture<Result<YdbQuery.CreateSessionResponse>> createSession(
            QueryServiceRpc rpc,
            CreateSessionSettings settings,
            boolean useServerBalancer) {
        YdbQuery.CreateSessionRequest request = YdbQuery.CreateSessionRequest.newBuilder()
                .build();

        String traceId = settings.getTraceId() == null ? UUID.randomUUID().toString() : settings.getTraceId();
        GrpcRequestSettings.Builder grpcSettingsBuilder = GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withTraceId(traceId);
        if (useServerBalancer) {
            grpcSettingsBuilder.addClientCapability(SERVER_BALANCER_HINT);
        }

        return rpc.createSession(request, grpcSettingsBuilder.build()).thenApply(CREATE_SESSION);
    }

    abstract class StreamImpl implements QueryStream {
        private final GrpcReadStream<YdbQuery.ExecuteQueryResponsePart> grpcStream;

        StreamImpl(GrpcReadStream<YdbQuery.ExecuteQueryResponsePart> grpcStream) {
            this.grpcStream = grpcStream;
        }

        abstract void handleTxMeta(String txId);
        void handleCompletion(Status status, Throwable th) { }

        @Override
        public CompletableFuture<Result<QueryInfo>> execute(PartsHandler handler) {
            final CompletableFuture<Result<QueryInfo>> result = new CompletableFuture<>();
            final AtomicReference<QueryStats> stats = new AtomicReference<>();
            grpcStream.start(msg -> {
                if (isTraceEnabled) {
                    logger.trace("{} got stream message {}", SessionImpl.this, TextFormat.shortDebugString(msg));
                }
                Issue[] issues = Issue.fromPb(msg.getIssuesList());
                Status status = Status.of(StatusCode.fromProto(msg.getStatus()), issues);

                updateSessionState(status);

                if (!status.isSuccess()) {
                    handleTxMeta(null);
                    result.complete(Result.fail(status));
                    return;
                }

                if (msg.hasTxMeta()) {
                    handleTxMeta(msg.getTxMeta().getId());
                }
                if (issues.length > 0) {
                    if (handler != null) {
                        handler.onIssues(issues);
                    } else {
                        logger.trace("{} lost issues message", SessionImpl.this);
                    }
                }
                if (msg.hasExecStats()) {
                    QueryStats old = stats.getAndSet(new QueryStats(msg.getExecStats()));
                    if (old != null) {
                        logger.warn("{} lost previous exec stats {}", SessionImpl.this, old);
                    }
                }

                if (msg.hasResultSet()) {
                    long index = msg.getResultSetIndex();
                    if (handler != null) {
                        handler.onNextPart(new QueryResultPart(index, msg.getResultSet()));
                    } else {
                        logger.trace("{} lost result set part with index {}", SessionImpl.this, index);
                    }
                }
            }).whenComplete((status, th) -> {
                handleCompletion(status, th);
                if (th != null) {
                    result.completeExceptionally(th);
                }
                if (status != null) {
                    updateSessionState(status);
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
            updateSessionState(CANCELLED);
            grpcStream.cancel();
        }
    }

    class TransactionImpl extends YdbTransactionImpl implements QueryTransaction {

        TransactionImpl(TxMode txMode, String txId) {
            super(txMode, txId);
        }

        @Override
        public String getSessionId() {
            return sessionId;
        }

        @Override
        public QuerySession getSession() {
            return SessionImpl.this;
        }

        @Override
        public QueryStream createQuery(String query, boolean commitAtEnd, Params prms, ExecuteQuerySettings settings) {
            // If we intend to commit, statusFuture is reset to reflect only future actions in transaction
            CompletableFuture<Status> currentStatusFuture = commitAtEnd
                    ? statusFuture.getAndSet(new CompletableFuture<>())
                    : statusFuture.get();
            final String currentId = txId.get();
            YdbQuery.TransactionControl tc = currentId != null
                    ? TxControl.txIdCtrl(currentId, commitAtEnd)
                    : TxControl.txModeCtrl(txMode, commitAtEnd);

            return new StreamImpl(createGrpcStream(query, tc, prms, settings)) {
                @Override
                void handleTxMeta(String txID) {
                    String newId = txID == null || txID.isEmpty() ? null : txID;
                    if (!txId.compareAndSet(currentId, newId)) {
                        logger.warn("{} lost transaction meta id {}", SessionImpl.this, newId);
                    }
                }
                @Override
                void handleCompletion(Status status, Throwable th) {
                    if (th != null) {
                        currentStatusFuture.completeExceptionally(
                                new RuntimeException("Query on transaction failed with exception ", th));
                    }
                    if (status.isSuccess()) {
                        if (commitAtEnd) {
                            currentStatusFuture.complete(Status.SUCCESS);
                        }
                    } else {
                        currentStatusFuture.complete(Status
                                .of(StatusCode.ABORTED)
                                .withIssues(Issue.of("Query on transaction failed with status "
                                        + status, Issue.Severity.ERROR)));
                    }
                }

                @Override
                public void cancel() {
                    super.cancel();
                    if (txId.compareAndSet(currentId, null)) {
                        logger.warn("{} transaction with id {} was cancelled", SessionImpl.this, currentId);
                    }
                }
            };
        }

        @Override
        public CompletableFuture<Result<QueryInfo>> commit(CommitTransactionSettings settings) {
            CompletableFuture<Status> currentStatusFuture = statusFuture.getAndSet(new CompletableFuture<>());
            final String transactionId = txId.get();
            if (transactionId == null) {
                Issue issue = Issue.of("Transaction is not started", Issue.Severity.WARNING);
                Result<QueryInfo> res = Result.success(new QueryInfo(null), Status.of(StatusCode.SUCCESS, issue));
                return CompletableFuture.completedFuture(res);
            }

            YdbQuery.CommitTransactionRequest request = YdbQuery.CommitTransactionRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setTxId(transactionId)
                    .build();
            return rpc.commitTransaction(request, makeOptions(settings).build())
                    .thenApply(res -> {
                        Status status = res.getStatus();
                        currentStatusFuture.complete(status);
                        updateSessionState(status);
                        if (!txId.compareAndSet(transactionId, null)) {
                            logger.warn("{} lost commit response for transaction {}", SessionImpl.this, transactionId);
                        }
                        // TODO: CommitTransactionResponse must contain exec_stats
                        return res.map(resp -> new QueryInfo(null));
                    }).whenComplete(((status, th) -> {
                        if (th != null) {
                            currentStatusFuture.completeExceptionally(
                                    new RuntimeException("Transaction commit failed with exception", th));
                        }
                    }));
        }

        @Override
        public CompletableFuture<Status> rollback(RollbackTransactionSettings settings) {
            CompletableFuture<Status> currentStatusFuture = statusFuture.getAndSet(new CompletableFuture<>());
            final String transactionId = txId.get();

            if (transactionId == null) {
                Issue issue = Issue.of("Transaction is not started", Issue.Severity.WARNING);
                return CompletableFuture.completedFuture(Status.of(StatusCode.SUCCESS, issue));
            }

            YdbQuery.RollbackTransactionRequest request = YdbQuery.RollbackTransactionRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setTxId(transactionId)
                    .build();
            return rpc.rollbackTransaction(request, makeOptions(settings).build())
                    .thenApply(result -> {
                        updateSessionState(result.getStatus());
                        if (!txId.compareAndSet(transactionId, null)) {
                            logger.warn("{} lost rollback response for transaction {}", SessionImpl.this,
                                    transactionId);
                        }
                        return result.getStatus();
                    })
                    .whenComplete((status, th) -> currentStatusFuture.complete(Status
                            .of(StatusCode.ABORTED)
                            .withIssues(Issue.of("Transaction was rolled back", Issue.Severity.ERROR))));
        }
    }
}
