package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

import io.grpc.ExperimentalApi;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResultPart;

/**
 *
 * @author Aleksandr Gorshenin
 */
@ExperimentalApi("QueryService is experimental and API may change without notice")
public interface QueryStream {
    interface PartsHandler {
        default void onIssues(Issue[] issues) { }
        void onNextPart(QueryResultPart part);
    }

    CompletableFuture<Result<QueryInfo>> execute(PartsHandler handler);

    void cancel();

    default CompletableFuture<Result<QueryInfo>> execute() {
        return execute(null);
    }
}
