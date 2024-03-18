package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.query.settings.BeginTransactionSettings;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.table.query.Params;

/**
 * Sessions are basic primitives for communicating with YDB Query Service. The are similar to connections for classic
 * relational DBs. Sessions serve three main purposes:
 *
 * <ol>
 * <li>Provide a flow control for DB requests with limited number of active channels.</li>
 * <li>Distribute load evenly across multiple DB nodes.</li>
 * <li>Store state for volatile stateful operations, such as short-living transactions.</li>
 * </ol>
 *
 * @author Aleksandr Gorshenin
 */
public interface QuerySession extends AutoCloseable {

    /**
     *
     * @return identifier of session
     */
    String getId();

    QueryTransaction currentTransaction();

    QueryTransaction createNewTransaction(QueryTx txMode);

    CompletableFuture<Result<QueryTransaction>> beginTransaction(QueryTx txMode, BeginTransactionSettings settings);

    /**
     * Create {@link QueryStream} for executing query with specified {@link QueryTx}. The query can contain DML, DDL and
     * DCL statements. Supported mix of different statement types depends on the chosen transaction type.
     *
     * @param query text of query
     * @param tx transaction mode
     * @param prms query parameters
     * @param settings additional settings of query execution
     * @return ready to execute an instance of {@link QueryStream}
     */
    QueryStream createQuery(String query, QueryTx tx, Params prms, ExecuteQuerySettings settings);

    @Override
    void close();

    /**
     * Create {@link QueryStream} for executing query with specified {@link QueryTx}. The query can contain DML, DDL and
     * DCL statements. Supported mix of different statement types depends on the chosen transaction type.
     *
     * @param query text of query
     * @param tx transaction mode
     * @param prms query parameters
     * @return ready to execute an instance of {@link QueryStream}
     */
    default QueryStream createQuery(String query, QueryTx tx, Params prms) {
        return createQuery(query, tx, prms, ExecuteQuerySettings.newBuilder().build());
    }

    /**
     * Create {@link QueryStream} for executing query with specified {@link QueryTx}. The query can contain DML, DDL and
     * DCL statements. Supported mix of different statement types depends on the chosen transaction type.
     *
     * @param query text of query
     * @param tx transaction mode
     * @return ready to execute an instance of {@link QueryStream}
     */
    default QueryStream createQuery(String query, QueryTx tx) {
        return createQuery(query, tx, Params.empty(), ExecuteQuerySettings.newBuilder().build());
    }

    default CompletableFuture<Result<QueryTransaction>> beginTransaction(QueryTx tx) {
        return beginTransaction(tx, BeginTransactionSettings.newBuilder().build());
    }
}
