package tech.ydb.query.script;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.operation.Operation;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.query.YdbQuery;

import java.util.concurrent.CompletableFuture;

public interface ScriptRpc {

    CompletableFuture<Operation<Status>> getOperation(String operationId);

    CompletableFuture<Operation<Status>> executeScript(
            YdbQuery.ExecuteScriptRequest request, GrpcRequestSettings settings);

    CompletableFuture<Result<YdbQuery.FetchScriptResultsResponse>> fetchScriptResults(
            YdbQuery.FetchScriptResultsRequest request, GrpcRequestSettings settings);
}
