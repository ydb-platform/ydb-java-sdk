package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResponsePart;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QueryStream {
    interface PartsHandler {
        void onPart(QueryResponsePart part);
    }

    CompletableFuture<Result<QueryInfo>> execute(PartsHandler handler);

    void cancel();

}
