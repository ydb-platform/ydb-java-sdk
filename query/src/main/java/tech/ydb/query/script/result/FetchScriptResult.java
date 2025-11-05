package tech.ydb.query.script.result;

import tech.ydb.core.Result;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.YdbIssueMessage;
import tech.ydb.proto.query.YdbQuery;

import java.util.List;

public class FetchScriptResult {
    final YdbQuery.FetchScriptResultsResponse resultsResponse;

    public FetchScriptResult(YdbQuery.FetchScriptResultsResponse value) {
        resultsResponse = value;
    }

    public ValueProtos.ResultSet getResultSet(){
        return resultsResponse.getResultSet();
    }

    public StatusCodesProtos.StatusIds.StatusCode getStatusCode() {
        return resultsResponse.getStatus();
    }

    public List<YdbIssueMessage.IssueMessage> getIssuesList() {
        return resultsResponse.getIssuesList();
    }

    public String getNextFetchToken() {
        return resultsResponse.getNextFetchToken();
    }

    public long getResultSetIndex() {
        return resultsResponse.getResultSetIndex();
    }
}
