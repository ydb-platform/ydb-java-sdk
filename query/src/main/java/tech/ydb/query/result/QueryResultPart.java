package tech.ydb.query.result;

import tech.ydb.proto.draft.query.YdbQuery;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryResultPart {
    private final long rsIndex;
    private final ResultSetReader rsReader;

    public QueryResultPart(YdbQuery.ExecuteQueryResponsePart response) {
        this.rsIndex = response.getResultSetIndex();
        this.rsReader = ProtoValueReaders.forResultSet(response.getResultSet());
    }

    public long getResultSetIndex() {
        return this.rsIndex;
    }

    public ResultSetReader getResultSetReader() {
        return this.rsReader;
    }
}
