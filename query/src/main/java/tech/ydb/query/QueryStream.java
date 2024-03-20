package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResultPart;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QueryStream {
    interface PartsHandler {
        void onNextPart(QueryResultPart part);
    }

    CompletableFuture<Result<QueryInfo>> execute(PartsHandler handler);

    void cancel();

    default CompletableFuture<Result<QueryInfo>> execute() {
        return execute(null);
    };
}
