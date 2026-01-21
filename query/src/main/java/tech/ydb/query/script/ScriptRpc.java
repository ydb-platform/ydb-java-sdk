package tech.ydb.query.script;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.operation.Operation;
import tech.ydb.proto.query.YdbQuery;

/**
 * Low-level RPC interface for executing YQL scripts and fetching their results using gRPC.
 * <p>
 * Provides direct bindings to the YDB QueryService API
 * Used internally by {@link tech.ydb.query.script.ScriptClient} implementations.
 *
 * <p>Author: Evgeny Kuvardin
 */
public interface ScriptRpc {

    /**
     * Retrieves a previously created operation by its ID.
     *
     * @param operationId ID of the operation to fetch
     * @return future resolving to the operation metadata and status
     */
    CompletableFuture<Operation<Status>> getOperation(String operationId);

    /**
     * Executes a script as a long-running operation.
     *
     * @param request  execution request describing the script and execution mode {@link YdbQuery.ExecuteScriptRequest}
     * @param settings RPC request settings including timeout, trace ID, etc.
     * @return future resolving to an {@link Operation} representing the script execution
     */
    CompletableFuture<Operation<Status>> executeScript(
            YdbQuery.ExecuteScriptRequest request, GrpcRequestSettings settings);

    /**
     * Fetches partial results for a previously executed script.
     *
     * @param request  fetch request including token, result set index, etc. {@link YdbQuery.FetchScriptResultsRequest}
     * @param settings RPC settings for this request
     * @return future resolving to the result fetch response {@link Result} of {@link YdbQuery.FetchScriptResultsResponse}
     */
    CompletableFuture<Result<YdbQuery.FetchScriptResultsResponse>> fetchScriptResults(
            YdbQuery.FetchScriptResultsRequest request, GrpcRequestSettings settings);
}
