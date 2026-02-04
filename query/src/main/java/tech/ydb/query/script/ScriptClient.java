package tech.ydb.query.script;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationTray;
import tech.ydb.query.script.impl.ScriptClientImpl;
import tech.ydb.query.script.result.ScriptResultPart;
import tech.ydb.query.script.settings.ExecuteScriptSettings;
import tech.ydb.query.script.settings.FetchScriptSettings;
import tech.ydb.query.script.settings.FindScriptSettings;
import tech.ydb.table.query.Params;

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
 * <p>Example with fetch
 * <pre>{@code
 *      Operation<Status> operation = scriptClient.startQueryScript("select...",Params.of(...), executeScriptSettings).join())
 *      Status status = scriptClient.fetchQueryScriptStatus(operation, 1).join()
 *      Result< ScriptResultPart> resultPartResult = scriptClient.fetchQueryScriptResult(operation, null, fetchScriptSettings).join()
 *      ResultSetReader reader = scriptResultPart.getResultSetReader()
 *      reader.next()
 * }</pre>
 * <p>Example without fetch
 * <pre>{@code
 * Status status = scriptClient.startQueryScript("select...",Params.of(...), executeScriptSettings)
 *                             .thenCompose(p -> scriptClient.fetchQueryScriptStatus(p, 1))
 *                             .join()
 * }</pre>
 * <p>Author: Evgeny Kuvardin
 */
public interface ScriptClient {
    static ScriptClient newClient(@WillNotClose GrpcTransport transport) {
        return new ScriptClientImpl(transport);
    }

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
    CompletableFuture<Result<ScriptResultPart>> fetchQueryScriptResult(@Nonnull Operation<Status> operation,
                                                                       @Nullable ScriptResultPart previous,
                                                                       FetchScriptSettings settings);

}
