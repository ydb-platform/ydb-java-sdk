package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

import io.grpc.ExperimentalApi;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.query.settings.BeginTransactionSettings;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.table.query.Params;

/**
 * Sessions are basic primitives for communicating with YDB Query Service. They are similar to connections for classic
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
@ExperimentalApi("QueryService is experimental and API may change without notice")
public interface QuerySession extends AutoCloseable {

    /**
     * Return the identifier of the session
     *
     * @return identifier of the session
     */
    String getId();

    /**
     *
     * @return current {@link QueryTransaction} of the session
     */
    QueryTransaction currentTransaction();

    /**
     * Create a new <i>not active</i> {@link QueryTransaction}. This QueryTransaction will have no identifier and
     * starts a transaction on server by execution any query
     * @param txMode transaction mode
     * @return new implicit transaction
     */
    QueryTransaction createNewTransaction(TxMode txMode);

    /**
     * Create and start a new <i>active</i> {@link QueryTransaction}. This method creates a transaction on the server
     * and returns QueryTransaction which is ready to execute queries on this server transaction
     *
     * @param txMode transaction mode
     * @param settings additional settings for request
     * @return future with result of the transaction starting
     */
    CompletableFuture<Result<QueryTransaction>> beginTransaction(TxMode txMode, BeginTransactionSettings settings);

    /**
     * Create {@link QueryStream} for executing query with specified {@link TxMode}. The query can contain DML, DDL and
     * DCL statements. Supported mix of different statement types depends on the chosen transaction type.
     *
     * @param query text of query
     * @param tx transaction mode
     * @param params query parameters
     * @param settings additional settings of query execution
     * @return a ready to execute instance of {@link QueryStream}
     */
    QueryStream createQuery(String query, TxMode tx, Params params, ExecuteQuerySettings settings);

    @Override
    void close();

    /**
     * Create {@link QueryStream} for executing query with specified {@link TxMode}. The query can contain DML, DDL and
     * DCL statements. Supported mix of different statement types depends on the chosen transaction type.
     *
     * @param query text of query
     * @param tx transaction mode
     * @param params query parameters
     * @return a ready to execute instance of {@link QueryStream}
     */
    default QueryStream createQuery(String query, TxMode tx, Params params) {
        return createQuery(query, tx, params, ExecuteQuerySettings.newBuilder().build());
    }

    /**
     * Create {@link QueryStream} for executing query with specified {@link TxMode}. The query can contain DML, DDL and
     * DCL statements. Supported mix of different statement types depends on the chosen transaction type.
     *
     * @param query text of query
     * @param tx transaction mode
     * @return a ready to execute instance of {@link QueryStream}
     */
    default QueryStream createQuery(String query, TxMode tx) {
        return createQuery(query, tx, Params.empty(), ExecuteQuerySettings.newBuilder().build());
    }

    /**
     * Create and start a new <i>active</i> {@link QueryTransaction}. This method creates a transaction on the server
     * and returns QueryTransaction which is ready to execute queries on this server transaction
     *
     * @param txMode transaction mode
     * @return future with result of the transaction starting
     */
    default CompletableFuture<Result<QueryTransaction>> beginTransaction(TxMode txMode) {
        return beginTransaction(txMode, BeginTransactionSettings.newBuilder().build());
    }
}
