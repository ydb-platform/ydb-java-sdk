package tech.ydb.table.query;

import tech.ydb.proto.table.YdbTable.ReadRowsResponse;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;

public class ReadRowsResult {
    private final ResultSetReader resultSetReader;

    public ReadRowsResult(ReadRowsResponse readRowsResponse) {
        this.resultSetReader = ProtoValueReaders.forResultSet(readRowsResponse.getResultSet());
    }

    public ResultSetReader getResultSetReader() {
        return resultSetReader;
    }
}
