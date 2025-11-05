package tech.ydb.query.script;

import tech.ydb.core.Status;
import tech.ydb.query.script.result.OperationScript;
import tech.ydb.query.script.result.FetchScriptResult;
import tech.ydb.query.script.settings.ExecuteScriptSettings;
import tech.ydb.query.script.settings.FetchScriptSettings;
import tech.ydb.query.script.settings.FindScriptSettings;
import tech.ydb.table.query.Params;


import java.util.concurrent.CompletableFuture;

public interface ScriptClient {

    /**
     * Find script
     *
     * @param operationId
     * @param settings
     * @return
     */
    CompletableFuture<OperationScript> findScript(String operationId, FindScriptSettings settings);

    /**
     * Start script and get entity for operation
     * @param query
     * @param params
     * @param settings
     * @return
     */
    CompletableFuture<OperationScript> startScript(String query,
                                                   Params params,
                                                   ExecuteScriptSettings settings);

    /**
     * Wait for script execution and just give result
     *
     * @param query
     * @param params
     * @param settings
     * @return
     */
    Status startJoinScript(String query,
                           Params params,
                           ExecuteScriptSettings settings);

    CompletableFuture<FetchScriptResult> fetchScriptResults(
            FetchScriptSettings settings);

}
