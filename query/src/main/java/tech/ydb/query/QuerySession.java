package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

import io.grpc.ExperimentalApi;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationTray;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.scripting.ScriptingProtos;
import tech.ydb.query.result.OperationResult;
import tech.ydb.query.settings.BeginTransactionSettings;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.settings.ExecuteScriptSettings;
import tech.ydb.query.settings.FetchScriptSettings;
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

    /**
     * Executes a YQL script via the scripting service and returns its result as a completed future.
     *
     * <p>This method sends a YQL script for execution and collects the full result set in a single response.
     * It uses {@link ScriptingProtos.ExecuteYqlRequest} under the hood and returns
     * an {@link OperationResult} wrapped in {@link Result} to provide status and issues details.</p>
     *
     * @param query    the YQL script text to execute
     * @param params   input parameters for the script
     * @param settings execution settings such as statistics collection or tracing
     * @return a future that resolves to a {@link Result} containing {@link ScriptingProtos.ExecuteYqlResult}
     */
    CompletableFuture<Result<ScriptingProtos.ExecuteYqlResult>> executeScriptYql(String query,
                                                                                 Params params,
                                                                                 ExecuteScriptSettings settings);


    /**
     * Submits a YQL script for asynchronous execution and returns a handle to the operation.
     * Take a not that join return future will not guarantee that script is finished. It's guarantee that script is passed to ydb
     *
     * <p>This method executes the given script asynchronously and immediately returns
     * a {@link CompletableFuture} for an {@link Operation}, which can be later monitored or fetched
     * via {@link #waitForScript(CompletableFuture)} or {@link #fetchScriptResults(String, Params, FetchScriptSettings)}.</p>
     *
     * @param query    the YQL script text to execute
     * @param params   input parameters to pass to the script
     * @param settings script execution options such as TTL, statistics mode, or resource pool
     * @return a future resolving to an {@link Operation} representing the submitted script execution
     */
    CompletableFuture<Operation<Status>> executeScript(String query, Params params, ExecuteScriptSettings settings);

    /**
     * Fetches partial or complete results from a previously executed YQL script.
     *
     * <p>This method retrieves result sets produced by an asynchronous script execution.
     * It supports incremental fetching using tokens, row limits, and result set index selection.</p>
     *
     * @param query     optional query text for context (not used by the server but may help debugging)
     * @param params    parameters used during script execution (typically empty)
     * @param settings  settings that define which operation to fetch results from, including fetch token, row limit, and index
     * @return a future resolving to a {@link Result} containing {@link YdbQuery.FetchScriptResultsResponse}
     */
    CompletableFuture<Result<YdbQuery.FetchScriptResultsResponse>> fetchScriptResults(String query,
                                                                                      Params params,
                                                                                      FetchScriptSettings settings);

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

    /**
     * Executes a YQL script via the scripting service and returns its result as a completed future.
     *
     * <p>This method sends a YQL script for execution and collects the full result set in a single response.
     * It uses {@link ScriptingProtos.ExecuteYqlRequest} under the hood and returns
     * an {@link OperationResult} wrapped in {@link Result} to provide status and issues details.</p>
     *
     * @param query    the YQL script text to execute
     * @return a future that resolves to a {@link Result} containing {@link ScriptingProtos.ExecuteYqlResult}
     */
    default CompletableFuture<Result<ScriptingProtos.ExecuteYqlResult>> executeScriptYql(String query) {
        return executeScriptYql(query, Params.empty(), ExecuteScriptSettings.newBuilder().build());
    }

    /**
     * Submits a YQL script for asynchronous execution and returns a handle to the operation.
     * Take a not that join return future will not guarantee that script is finished. It's guarantee that script is passed to ydb
     *
     * <p>This method executes the given script asynchronously and immediately returns
     * a {@link CompletableFuture} for an {@link Operation}, which can be later monitored or fetched
     * via {@link #waitForScript(CompletableFuture)} or {@link #fetchScriptResults(String, Params, FetchScriptSettings)}.</p>
     *
     * @param query    the YQL script text to execute
     * @return a future resolving to an {@link Operation} representing the submitted script execution
     */
    default CompletableFuture<Operation<Status>> executeScript(String query) {
        return executeScript(query, Params.empty(), ExecuteScriptSettings.newBuilder().build());
    }

    /**
     * Waits for a previously submitted script operation to complete.
     *
     * <p>This method polls or fetches the state of the running operation via {@link OperationTray#fetchOperation}
     * until the operation completes successfully or fails. It is typically used after calling
     * {@link #executeScript(String, Params, ExecuteScriptSettings)}.</p>
     *
     * @param scriptFuture a {@link CompletableFuture} returned by {@link #executeScript(String, Params, ExecuteScriptSettings)}
     * @return a future resolving to the final {@link Status} of the script execution
     */
    CompletableFuture<Status> waitForScript(CompletableFuture<Operation<Status>> scriptFuture);

}
