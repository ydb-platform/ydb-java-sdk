package tech.ydb.query.script;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationTray;
import tech.ydb.query.script.result.ScriptResultPart;
import tech.ydb.query.script.settings.ExecuteScriptSettings;
import tech.ydb.query.script.settings.FetchScriptSettings;
import tech.ydb.query.script.settings.FindScriptSettings;
import tech.ydb.table.query.Params;


import javax.annotation.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * High-level API for executing YQL scripts and retrieving their results.
 * <p>
 * Provides convenience methods for starting script execution, tracking operation status,
 * and fetching result sets with pagination support.
 * <p>
 * How to use
 * <ul>
 *     <li>startQueryScript - starting script execution or findQueryScript if script had already started</li>
 *     <li>fetchQueryScriptStatus - wait for script execution</li>
 *     <li>fetchQueryScriptResult - fetch script result if necessary</li>
 * </ul>
 * </p>
 * Example with fetch
 * <p>
 *  <ul>
 *    <li> Operation<Status> operation = scriptClient.startQueryScript("select...",Params.of(...), executeScriptSettings).join()</li>
 *    <li>Status status = scriptClient.fetchQueryScriptStatus(operation, 1).join()</li>
 *    <li>Result<ScriptResultPart> resultPartResult = scriptClient.fetchQueryScriptResult(operation, null, fetchScriptSettings).join()</li>
 *    <li>ResultSetReader reader = scriptResultPart.getResultSetReader()</li>
 *    <li>reader.next()</li>
 *  </ul>
 * </p>
 *
 *  * Example without fetch
 *  * <p>
 *  *  <ul>
 *  *    <li> Status status =
 *                      scriptClient.startQueryScript("select...",Params.of(...), executeScriptSettings)
 *                                  .thenCompose(p -> scriptClient.fetchQueryScriptStatus(p, 1))
 *                                  .join()</li>
 *  *  </ul>
 *  * </p>
 */
public interface ScriptClient {

    /**
     * Returns operation metadata for a previously started script execution.
     *
     * @param operationId operation identifier
     * @param settings    request settings
     * @return future resolving to operation status
     */
    CompletableFuture<Operation<Status>> findQueryScript(String operationId, FindScriptSettings settings);

    /**
     * Starts execution of the given YQL script with optional parameters.
     *
     * @param query    YQL script text
     * @param params   query parameters
     * @param settings execution settings (TTL, resource pool, exec mode)
     * @return future resolving to a long-running operation
     */
    CompletableFuture<Operation<Status>> startQueryScript(String query,
                                                          Params params,
                                                          ExecuteScriptSettings settings);

    /**
     * Wait for script execution and return status
     *
     * @param operation operation object returned when script started
     * @param fetchRateSeconds How often should we check if the operation has finished
     * @return future with result of script execution
     */
    default CompletableFuture<Status> fetchQueryScriptStatus(Operation<Status> operation, int fetchRateSeconds) {
        return OperationTray.fetchOperation(operation, fetchRateSeconds);
    }

    /**
     * Fetches script results incrementally.
     *
     * @param operation operation object returned when script started
     * @param previous  previous result part, or {@code null} if fetching from start
     * @param settings  fetch configuration
     * @return future resolving to result part containing a result set fragment
     */
    CompletableFuture<Result<ScriptResultPart>> fetchQueryScriptResult(Operation<Status> operation,
                                                                       @Nullable ScriptResultPart previous, FetchScriptSettings settings);

}
