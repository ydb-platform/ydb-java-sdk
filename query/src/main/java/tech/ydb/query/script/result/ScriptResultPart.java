package tech.ydb.query.script.result;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;

/**
 * Represents a single portion of script execution results.
 * <p>
 * Contains:
 * <ul>
 *     <li>a result set reader for the retrieved rows</li>
 *     <li>index of the result set within the script</li>
 *     <li>fetch token for retrieving the next portion</li>
 * </ul>
 */
public class ScriptResultPart {
    private final ValueProtos.ResultSet resultSet;
    private final long resultSetIndex;
    private final String nextFetchToken;

    public ScriptResultPart(YdbQuery.FetchScriptResultsResponse value) {
        this.resultSet = value.getResultSet();
        this.resultSetIndex = value.getResultSetIndex();
        this.nextFetchToken = value.getNextFetchToken();
    }

    public ResultSetReader getResultSetReader() {
        return ProtoValueReaders.forResultSet(resultSet);
    }

    public String getNextFetchToken() {
        return nextFetchToken;
    }

    public long getResultSetIndex() {
        return resultSetIndex;
    }
}
