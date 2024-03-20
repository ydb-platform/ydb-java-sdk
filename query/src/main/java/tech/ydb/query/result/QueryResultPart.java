package tech.ydb.query.result;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryResultPart {
    private final long index;
    private final ValueProtos.ResultSet resultSet;

    public QueryResultPart(long index, ValueProtos.ResultSet resultSet) {
        this.index = index;
        this.resultSet = resultSet;
    }

    public long getResultSetIndex() {
        return this.index;
    }

    public int getResultSetRowsCount() {
        return this.resultSet.getRowsCount();
    }

    public ResultSetReader getResultSetReader() {
        return ProtoValueReaders.forResultSet(resultSet);
    }
}
