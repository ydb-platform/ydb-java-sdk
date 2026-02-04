package tech.ydb.query.script.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.core.operation.StatusExtractor;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.operation.v1.OperationServiceGrpc;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.query.v1.QueryServiceGrpc;

/**
 * Low-level RPC class for executing YQL scripts and fetching their results using gRPC.
 * <p>
 * Provides direct bindings to the YDB QueryService API
 * Used internally by {@link tech.ydb.query.script.ScriptClient} implementations.
 *
 * <p>Author: Evgeny Kuvardin
 */
public class ScriptRpc {
    private static final StatusExtractor<YdbQuery.FetchScriptResultsResponse> FETCH_SCRIPT = StatusExtractor.of(
            YdbQuery.FetchScriptResultsResponse::getStatus,
            YdbQuery.FetchScriptResultsResponse::getIssuesList
    );

    private final GrpcTransport transport;

    ScriptRpc(GrpcTransport transport) {
        this.transport = transport;
    }

    public CompletableFuture<Operation<Status>> getOperation(String operationId, GrpcRequestSettings settings) {
        OperationProtos.GetOperationRequest request = OperationProtos.GetOperationRequest.newBuilder()
                .setId(operationId)
                .build();

        return transport
                .unaryCall(OperationServiceGrpc.getGetOperationMethod(), settings, request)
                .thenApply(OperationBinder.bindAsync(transport, OperationProtos.GetOperationResponse::getOperation));
    }

    public CompletableFuture<Operation<Status>> executeScript(
            YdbQuery.ExecuteScriptRequest request, GrpcRequestSettings settings) {

        return transport.unaryCall(QueryServiceGrpc.getExecuteScriptMethod(), settings, request)
                .thenApply(OperationBinder.bindAsync(transport, Function.identity()));
    }

    public CompletableFuture<Result<YdbQuery.FetchScriptResultsResponse>> fetchScriptResults(
            YdbQuery.FetchScriptResultsRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getFetchScriptResultsMethod(), settings, request)
                .thenApply(FETCH_SCRIPT);
    }
}
