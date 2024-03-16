package tech.ydb.query.result;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryResponsePart {
    private final long rsIdx;
    private final ValueProtos.ResultSet rs;

    public QueryResponsePart(YdbQuery.ExecuteQueryResponsePart response) {
        this.rsIdx = response.getResultSetIndex();
        this.rs = response.getResultSet();
    }

    public long getResultSetIndex() {
        return this.rsIdx;
    }

    public int getResultSetRowsCount() {
        return rs.getRowsCount();
    }

    public ResultSetReader getResultSetReader() {
        return ProtoValueReaders.forResultSet(rs);
    }
}
