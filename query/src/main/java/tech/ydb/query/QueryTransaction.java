package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.settings.CommitTransactionSettings;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.settings.RollbackTransactionSettings;
import tech.ydb.table.query.Params;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QueryTransaction {
    String getId();

    QueryTx getQueryTx();

    default boolean isActive() {
        return getId() != null;
    }

    QuerySession getSession();

    CompletableFuture<Result<QueryInfo>> commit(CommitTransactionSettings settings);
    CompletableFuture<Status> rollback(RollbackTransactionSettings settings);

    QueryStream createQuery(String query, Params prms, ExecuteQuerySettings settings);
    QueryStream createQueryWithCommit(String query, Params prms, ExecuteQuerySettings settings);

    default QueryStream createQuery(String query) {
        return createQuery(query, Params.empty(), ExecuteQuerySettings.newBuilder().build());
    }

    default QueryStream createQuery(String query, Params prms) {
        return createQuery(query, prms, ExecuteQuerySettings.newBuilder().build());
    }

    default CompletableFuture<Result<QueryInfo>> commit() {
        return commit(CommitTransactionSettings.newBuilder().build());
    }

    default CompletableFuture<Status> rollback() {
        return rollback(RollbackTransactionSettings.newBuilder().build());
    }
}
