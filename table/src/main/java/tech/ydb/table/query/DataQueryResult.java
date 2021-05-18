package tech.ydb.table.query;

import java.util.List;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;


/**
 * @author Sergey Polovko
 */
public class DataQueryResult {

    private final String txId;
    private final List<ValueProtos.ResultSet> resultSets;

    public DataQueryResult(String txId, List<ValueProtos.ResultSet> resultSets) {
        this.txId = txId;
        this.resultSets = resultSets;
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
}
