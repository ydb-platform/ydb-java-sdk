package tech.ydb.query.result.array;

import tech.ydb.query.result.QueryResultPart;
import tech.ydb.table.result.ResultSetReader;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ArrayQueryResultPart extends QueryResultPart {
    private final ArrayResultSetReader resultSetReader;

    public ArrayQueryResultPart(long index, ArrayResultSetReader resultSetReader) {
        super(index, null);
        this.resultSetReader = resultSetReader;
    }

    @Override
    public ResultSetReader getResultSetReader() {
        return resultSetReader;
    }
}
