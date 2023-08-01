package tech.ydb.table.query;

import java.util.List;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.query.stats.QueryStats;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;

/**
 * @author Sergey Polovko
 */
public class DataQueryResult {

    private final String txId;
    private final List<ValueProtos.ResultSet> resultSets;
    private final QueryStats queryStats;

    public DataQueryResult(YdbTable.ExecuteQueryResult result) {
        this.txId = result.getTxMeta().getId();
        this.resultSets = result.getResultSetsList();
        queryStats = result.hasQueryStats() ? new QueryStats(result.getQueryStats()) : null;
    }

    public String getTxId() {
        return txId;
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
