package tech.ydb.query.result;

import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.QueryTx;
import tech.ydb.query.impl.TxImpl;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryResultPart {
    private final long rsIndex;
    private final ResultSetReader rsReader;
    private final QueryTx.Id txId;

    public QueryResultPart(YdbQuery.ExecuteQueryResponsePart response) {
        this.rsIndex = response.hasResultSet() ? response.getResultSetIndex() : -1;
        this.rsReader = response.hasResultSet() ? ProtoValueReaders.forResultSet(response.getResultSet()) : null;
        String id = response.hasTxMeta() ? response.getTxMeta().getId() : null;
        this.txId =  id != null && !id.isEmpty() ? TxImpl.id(id) : null;
    }

    public QueryTx.Id getTxId() {
        return this.txId;
    }

    public long getResultSetIndex() {
        return this.rsIndex;
    }

    public ResultSetReader getResultSetReader() {
        return this.rsReader;
    }
}
