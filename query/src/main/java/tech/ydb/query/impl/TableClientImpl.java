package tech.ydb.query.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryStream;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.table.Session;
import tech.ydb.table.SessionPoolStats;
import tech.ydb.table.TableClient;
import tech.ydb.table.impl.BaseSession;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.query.stats.QueryStats;
import tech.ydb.table.result.ResultSetReader;
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

    private static class ProxedDataQueryResult extends DataQueryResult {
        private final String txID;
        private final QueryReader reader;

        ProxedDataQueryResult(String txID, QueryReader reader) {
            super(YdbTable.ExecuteQueryResult.getDefaultInstance());
            this.txID = txID;
            this.reader = reader;
        }

        @Override
        public String getTxId() {
            return txID;
        }

        @Override
        public int getResultSetCount() {
            return reader.getResultSetCount();
        }

        @Override
        public ResultSetReader getResultSet(int index) {
            return reader.getResultSet(index);
        }

        @Override
        public boolean isTruncated(int index) {
            return false;
        }

        @Override
        public int getRowCount(int index) {
            return reader.getResultSet(index).getRowCount();
        }

        @Override
        public boolean isEmpty() {
            return txID.isEmpty() && reader.getResultSetCount() == 0;
        }

        @Override
        public QueryStats getQueryStats() {
            return null;
        }

        @Override
        public boolean hasQueryStats() {
            return false;
        }
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
                    .build();

            final AtomicReference<String> txRef = new AtomicReference<>("");
            QueryStream stream = querySession.new StreamImpl(querySession.createGrpcStream(query, tc, prms, qs)) {
                @Override
                void handleTxMeta(String txID) {
                    txRef.set(txID);
                }
            };

            return QueryReader.readFrom(stream)
                    .thenApply(r -> r.map(
                            reader -> new ProxedDataQueryResult(txRef.get(), reader)
                    ));
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
