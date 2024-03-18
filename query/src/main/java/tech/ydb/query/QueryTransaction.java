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
 * Short-living object allows transactional execution of several queries in one interactive transaction.
 * @author Aleksandr Gorshenin
 */
public interface QueryTransaction {

    /**
     * Returns identifier of the transaction or null if the transaction is not active (not started/committed/rollbacked)
     * @return identifier of the transaction or null if the transaction is not active
     */
    String getId();

    /**
     * Returns {@link QueryTx} with mode of the transaction
     * @return the transaction mode
     */
    QueryTx getQueryTx();

    default boolean isActive() {
        return getId() != null;
    }

    /**
     * Returns {@link QuerySession} that was used for creating the transaction
     * @return session that was used for creating the transaction
     */
    QuerySession getSession();

    CompletableFuture<Result<QueryInfo>> commit(CommitTransactionSettings settings);
    CompletableFuture<Status> rollback(RollbackTransactionSettings settings);

    /**
     * Creates {@link QueryStream} for executing query in this transaction. The query can contain DML, DDL and DCL
     * statements. Supported mix of different statement types depends on the chosen transaction type.
     *
     * @param query text of query
     * @param commitAtEnd true if transaction must be committed after query execution
     * @param prms query parameters
     * @param settings additional settings of query execution
     * @return ready to execute an instance of {@link QueryStream}
     */
    QueryStream createQuery(String query, boolean commitAtEnd, Params prms, ExecuteQuerySettings settings);

    /**
     * Creates {@link QueryStream} for executing query in this transaction. Transaction <i>will not be committed</i>
     * after the execution of query. The query can contain DML, DDL and DCL statements. Supported mix of different
     * statement types depends on the chosen transaction type.
     *
     * @param query text of query
     * @return ready to execute an instance of {@link QueryStream}
     */
    default QueryStream createQuery(String query) {
        return createQuery(query, false, Params.empty(), ExecuteQuerySettings.newBuilder().build());
    }

    /**
     * Creates {@link QueryStream} for executing query in this transaction. Transaction <i>will not be committed</i>
     * after the execution of query. The query can contain DML, DDL and DCL statements. Supported mix of different
     * statement types depends on the chosen transaction type.
     *
     * @param query text of query
     * @param prms query parameters
     * @return ready to execute an instance of {@link QueryStream}
     */
    default QueryStream createQuery(String query, Params prms) {
        return createQuery(query, false, prms, ExecuteQuerySettings.newBuilder().build());
    }

    /**
     * Creates {@link QueryStream} for executing query in this transaction. Transaction <i>will be committed</i> after
     * the execution of query. The query can contain DML, DDL and DCL statements. Supported mix of different statement
     * types depends on the chosen transaction type.
     *
     * @param query text of query
     * @return ready to execute an instance of {@link QueryStream}
     */
    default QueryStream createQueryWithCommit(String query) {
        return createQuery(query, true, Params.empty(), ExecuteQuerySettings.newBuilder().build());
    }

    /**
     * Creates {@link QueryStream} for executing query in this transaction. Transaction <i>will be committed</i> after
     * the execution of query. The query can contain DML, DDL and DCL statements. Supported mix of different statement
     * types depends on the chosen transaction type.
     *
     * @param query text of query
     * @param prms query parameters
     * @return ready to execute an instance of {@link QueryStream}
     */
    default QueryStream createQueryWithCommit(String query, Params prms) {
        return createQuery(query, true, prms, ExecuteQuerySettings.newBuilder().build());
    }

    default CompletableFuture<Result<QueryInfo>> commit() {
        return commit(CommitTransactionSettings.newBuilder().build());
    }

    default CompletableFuture<Status> rollback() {
        return rollback(RollbackTransactionSettings.newBuilder().build());
    }
}
