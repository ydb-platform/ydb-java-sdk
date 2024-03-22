package tech.ydb.table.transaction;

import java.util.concurrent.CompletableFuture;

import tech.ydb.common.transaction.BaseTransaction;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.table.Session;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.settings.CommitTxSettings;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.settings.RollbackTxSettings;

/**
 * @author Nikolay Perfilov
 */
public interface TableTransaction extends BaseTransaction {

    /**
     * Returns {@link Session} that was used to create this transaction
     *
     * @return session that was used to create this transaction
     */
    Session getSession();

    /**
     * Execute DataQuery
     *
     * @param query text of query. Can only contain DML statements
     * @param commitAtEnd true if transaction must be committed after query execution
     * @param params query parameters
     * @param settings additional settings of query execution
     * @return a future to query result
     */
    CompletableFuture<Result<DataQueryResult>> executeDataQuery(
            String query, boolean commitAtEnd, Params params, ExecuteDataQuerySettings settings);

    /**
     * Execute DataQuery.
     * Transaction <i>will not be committed</i> after the execution of query.
     *
     * @param query text of query. Can only contain DML statements
     * @return a future to query result
     */
    default CompletableFuture<Result<DataQueryResult>> executeDataQuery(String query) {
        return executeDataQuery(query, false, Params.empty(), new ExecuteDataQuerySettings());
    }

    /**
     * Execute DataQuery.
     * Transaction <i>will not be committed</i> after the execution of query.
     *
     * @param query text of query. Can only contain DML statements
     * @param params query parameters
     * @return a future to query result
     */
    default CompletableFuture<Result<DataQueryResult>> executeDataQuery(String query, Params params) {
        return executeDataQuery(query, false, params, new ExecuteDataQuerySettings());
    }

    /**
     * Execute DataQuery.
     * Transaction <i>will be committed</i> after the execution of query.
     *
     * @param query text of query. Can only contain DML statements
     * @return a future to query result
     */
    default CompletableFuture<Result<DataQueryResult>> executeDataQueryAndCommit(String query) {
        return executeDataQuery(query, true, Params.empty(), new ExecuteDataQuerySettings());
    }

    /**
     * Execute DataQuery.
     * Transaction <i>will be committed</i> after the execution of query.
     *
     * @param query text of query. Can only contain DML statements
     * @param params query parameters
     * @return a future to query result
     */
    default CompletableFuture<Result<DataQueryResult>> executeDataQueryAndCommit(String query, Params params) {
        return executeDataQuery(query, false, params, new ExecuteDataQuerySettings());
    }

    CompletableFuture<Status> commit(CommitTxSettings settings);
    CompletableFuture<Status> rollback(RollbackTxSettings settings);

    default CompletableFuture<Status> commit() {
        return commit(new CommitTxSettings());
    }

    default CompletableFuture<Status> rollback() {
        return rollback(new RollbackTxSettings());
    }
}
