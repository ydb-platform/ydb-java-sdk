package tech.ydb.query.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryStream;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.query.result.QueryStats;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.settings.QueryStatsMode;
import tech.ydb.table.Session;
import tech.ydb.table.SessionPoolStats;
import tech.ydb.table.TableClient;
import tech.ydb.table.impl.BaseSession;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.ExecuteDataQuerySettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TableClientImpl implements TableClient {
    private final TableRpc rpc;
    private final QueryClientImpl proxy;

    TableClientImpl(Builder builder) {
        this.rpc = builder.rpc;
        this.proxy = new QueryClientImpl(builder.query);
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return proxy.getScheduler();
    }

    @Override
    public SessionPoolStats sessionPoolStats() {
        return proxy.getSessionPoolStats();
    }

    @Override
    public CompletableFuture<Result<Session>> createSession(Duration duration) {
        return proxy.createSession(duration).thenApply(r -> r.map(TableSession::new));
    }

    @Override
    public void close() {
        proxy.close();
    }

    private YdbQuery.TransactionControl mapTxControl(YdbTable.TransactionControl tc) {
        if (tc.hasTxId()) {
            return TxControl.txIdCtrl(tc.getTxId(), tc.getCommitTx());
        }
        if (tc.hasBeginTx()) {
            if (tc.getBeginTx().hasSerializableReadWrite()) {
                return TxControl.txModeCtrl(TxMode.SERIALIZABLE_RW, tc.getCommitTx());
            }
            if (tc.getBeginTx().hasSnapshotReadOnly()) {
                return TxControl.txModeCtrl(TxMode.SNAPSHOT_RO, tc.getCommitTx());
            }
            if (tc.getBeginTx().hasStaleReadOnly()) {
                return TxControl.txModeCtrl(TxMode.STALE_RO, tc.getCommitTx());
            }
            if (tc.getBeginTx().hasOnlineReadOnly()) {
                if (tc.getBeginTx().getOnlineReadOnly().getAllowInconsistentReads()) {
                    return TxControl.txModeCtrl(TxMode.ONLINE_INCONSISTENT_RO, tc.getCommitTx());
                } else {
                    return TxControl.txModeCtrl(TxMode.ONLINE_RO, tc.getCommitTx());
                }
            }
        }

        return TxControl.txModeCtrl(TxMode.NONE, tc.getCommitTx());
    }

    private class TableSession extends BaseSession {
        private final SessionImpl querySession;

        TableSession(QuerySession session) {
            super(session.getId(), rpc, false);
            this.querySession = (SessionImpl) session;
        }

        @Override
        public CompletableFuture<Result<DataQueryResult>> executeDataQueryInternal(
                String query, YdbTable.TransactionControl tx, Params prms, ExecuteDataQuerySettings settings) {
            YdbQuery.TransactionControl tc = mapTxControl(tx);
            ExecuteQuerySettings qs = ExecuteQuerySettings.newBuilder()
                    .withTraceId(settings.getTraceId())
                    .withRequestTimeout(settings.getTimeoutDuration())
                    .withStatsMode(QueryStatsMode.valueOf(settings.collectStats().name()))
                    .build();

            final AtomicReference<String> txRef = new AtomicReference<>("");
            final List<Issue> issues = new ArrayList<>();
            final List<ValueProtos.ResultSet> results = new ArrayList<>();

            QueryStream stream = querySession.new StreamImpl(querySession.createGrpcStream(query, tc, prms, qs, null),
                    null) {
                @Override
                void handleTxMeta(String txID) {
                    txRef.set(txID);
                }
            };

            CompletableFuture<Result<QueryInfo>> future = stream.execute(new QueryStream.PartsHandler() {
                @Override
                public void onIssues(Issue[] issueArr) {
                    issues.addAll(Arrays.asList(issueArr));
                }

                @Override
                public void onNextPart(QueryResultPart part) { } // not used

                @Override
                public void onNextRawPart(long index, ValueProtos.ResultSet rs) {
                    int idx = (int) index;
                    while (results.size() <= idx) {
                        results.add(null);
                    }
                    if (results.get(idx) == null) {
                        results.set(idx, rs);
                    } else {
                        results.set(idx, results.get(idx).toBuilder().addAllRows(rs.getRowsList()).build());
                    }
                }
            });


            return future.thenApply(res -> {
                if (!res.isSuccess()) {
                    return res.map(v -> null);
                }
                QueryStats stats = res.getValue().getStats();
                String txId = txRef.get();
                Status status = res.getStatus().withIssues(issues.toArray(new Issue[0]));
                DataQueryResult value = new DataQueryResult(txId, results, stats != null ? stats.toProtobuf() : null);
                return Result.success(value, status);
            });
        }

        @Override
        protected void updateSessionState(Throwable th, StatusCode code, boolean shutdownHint) {
            if (code != null) {
                querySession.updateSessionState(Status.of(code));
                return;
            }
            while (th != null) {
                if (th instanceof UnexpectedResultException) {
                    UnexpectedResultException unexpected = (UnexpectedResultException) th;
                    querySession.updateSessionState(unexpected.getStatus());
                }
                th = th.getCause();
            }
        }

        @Override
        public void close() {
            querySession.close();
        }
    }

    public static Builder newClient(GrpcTransport transport) {
        return new Builder(transport);
    }

    public static class Builder implements TableClient.Builder {
        private final TableRpc rpc;
        private final QueryClientImpl.Builder query;

        protected Builder(GrpcTransport transport) {
            this.rpc = GrpcTableRpc.useTransport(transport);
            this.query = new QueryClientImpl.Builder(transport);
        }

        @Override
        public Builder keepQueryText(boolean keep) {
            // ignored
            return this;
        }

        @Override
        public Builder sessionPoolSize(int minSize, int maxSize) {
            query.sessionPoolMaxSize(maxSize).sessionPoolMinSize(minSize);
            return this;
        }

        @Override
        public Builder sessionKeepAliveTime(Duration duration) {
            // ignored
            return this;
        }

        @Override
        public Builder sessionMaxIdleTime(Duration duration) {
            query.sessionMaxIdleTime(duration);
            return this;
        }

        @Override
        public TableClientImpl build() {
            return new TableClientImpl(this);
        }
    }
}
