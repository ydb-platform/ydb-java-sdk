package tech.ydb.query.script.result;

import tech.ydb.core.Issue;
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
 *     <li>issues returned by the server</li>
 * </ul>
 */
public class ScriptResultPart {
    private final ResultSetReader resultSetReader;
    private final long resultSetIndex;
    private final String nextFetchToken;
    private final Issue[] issues;

    public ScriptResultPart(YdbQuery.FetchScriptResultsResponse value) {
        this.resultSetReader = ProtoValueReaders.forResultSet(value.getResultSet());
        this.resultSetIndex = value.getResultSetIndex();
        this.nextFetchToken = value.getNextFetchToken();
        this.issues = Issue.fromPb(value.getIssuesList());
    }

    public ResultSetReader getResultSetReader() {
        return resultSetReader;
    }

    public String getNextFetchToken() {
        return nextFetchToken;
    }

    public long getResultSetIndex() {
        return resultSetIndex;
    }

    public boolean hasErrors() {
        return issues.length > 0;
    }

    public Issue[] getIssues() {
        return issues;
    }
}
