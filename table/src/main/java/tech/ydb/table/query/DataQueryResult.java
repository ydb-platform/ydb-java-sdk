package tech.ydb.table.query;

import java.util.List;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.Session;
import tech.ydb.table.impl.TransactionImpl;
import tech.ydb.table.query.stats.QueryStats;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;
import tech.ydb.table.transaction.Transaction;

/**
 * @author Sergey Polovko
 */
public class DataQueryResult {

    private final String txId;
    private final Transaction transaction;
    private final List<ValueProtos.ResultSet> resultSets;
    private final QueryStats queryStats;

    /**
     * @deprecated
     * Use constructor with Session parameter instead.
     */
    @Deprecated
    public DataQueryResult(YdbTable.ExecuteQueryResult result) {
        this.txId = result.getTxMeta().getId();
        this.transaction = null;
        this.resultSets = result.getResultSetsList();
        queryStats = result.hasQueryStats() ? new QueryStats(result.getQueryStats()) : null;
    }

    public DataQueryResult(YdbTable.ExecuteQueryResult result, Session session) {
        this.txId = result.getTxMeta().getId();
        this.transaction = new TransactionImpl(session, result.getTxMeta().getId());
        this.resultSets = result.getResultSetsList();
        queryStats = result.hasQueryStats() ? new QueryStats(result.getQueryStats()) : null;
    }

    public String getTxId() {
        return txId;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public int getResultSetCount() {
        return resultSets.size();
    }

    public ResultSetReader getResultSet(int index) {
        return ProtoValueReaders.forResultSet(resultSets.get(index));
    }

    public boolean isTruncated(int index) {
        return resultSets.get(index).getTruncated();
    }

    public int getRowCount(int index) {
        return resultSets.get(index).getRowsCount();
    }

    public boolean isEmpty() {
        return txId.isEmpty() && resultSets.isEmpty();
    }

    public QueryStats getQueryStats() {
        return queryStats;
    }

    public boolean hasQueryStats() {
        return queryStats != null;
    }
}
