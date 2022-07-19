package tech.ydb.table.query;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import tech.ydb.ValueProtos;
import tech.ydb.common.CommonProtos;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;


/**
 * @author Sergey Polovko
 */
public class DataQueryResult {

    private final String txId;
    private final List<ValueProtos.ResultSet> resultSets;
    @Nullable
    private final CostInfo costInfo;

    public DataQueryResult(String txId, List<ValueProtos.ResultSet> resultSets) {
        this(txId, resultSets, null);
    }

    public DataQueryResult(String txId, List<ValueProtos.ResultSet> resultSets, @Nullable CostInfo costInfo) {
        this.txId = txId;
        this.resultSets = resultSets;
        this.costInfo = costInfo;
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

    @Nullable
    public CostInfo getCostInfo() {
        return costInfo;
    }

    public static class CostInfo {
        private final double consumedUnits;

        public CostInfo(@Nonnull CommonProtos.CostInfo costInfo) {
            this.consumedUnits = costInfo.getConsumedUnits();
        }

        public double getConsumedUnits() {
            return consumedUnits;
        }
    }
}
