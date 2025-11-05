package tech.ydb.query.script.impl;

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
import tech.ydb.query.script.ScriptRpc;

import javax.annotation.WillNotClose;

import java.util.concurrent.CompletableFuture;

public class ScriptRpcImpl implements ScriptRpc {

    private static final StatusExtractor<YdbQuery.FetchScriptResultsResponse> FETCH_SCRIPT = StatusExtractor.of(
            YdbQuery.FetchScriptResultsResponse::getStatus,
            YdbQuery.FetchScriptResultsResponse::getIssuesList
    );

    private final GrpcTransport transport;

    private ScriptRpcImpl(GrpcTransport grpcTransport) {
        this.transport = grpcTransport;
    }

    public static ScriptRpcImpl useTransport(@WillNotClose GrpcTransport grpcTransport) {
        return new ScriptRpcImpl(grpcTransport);
    }

    @Override
    public CompletableFuture<Operation<Status>> getOperation(String operationId) {
        OperationProtos.GetOperationRequest request = OperationProtos.GetOperationRequest.newBuilder()
                .setId(operationId)
                .build();

        GrpcRequestSettings settings = GrpcRequestSettings.newBuilder().build();

        return transport
                .unaryCall(OperationServiceGrpc.getGetOperationMethod(), settings, request)
                .thenApply(
                        OperationBinder.bindAsync(transport,
                                OperationProtos.GetOperationResponse::getOperation
                        ));
    }

    /**
     * Executes a YQL script using the Query service API.
     *
     *
     * @param request  the {@link YdbQuery.ExecuteScriptRequest} containing the script
     * @param settings gRPC request settings
     * @return a future resolving to an {@link Operation} representing the script execution
     */
    @Override
    public CompletableFuture<Operation<Status>> executeScript(
            YdbQuery.ExecuteScriptRequest request, GrpcRequestSettings settings) {

        return transport.unaryCall(QueryServiceGrpc.getExecuteScriptMethod(), settings, request)
                .thenApply(
                        OperationBinder.bindAsync(transport,
                                op -> op
                        ));
    }

    /**
     * Fetches the results of a previously executed script.
     *
     * <p>This method retrieves the next portion of script execution results,
     * supporting pagination and partial fetch using tokens.</p>
     *
     * @param request  the {@link YdbQuery.FetchScriptResultsRequest} specifying the fetch parameters
     * @param settings gRPC request settings
     * @return a future resolving to {@link Result} containing {@link YdbQuery.FetchScriptResultsResponse}
     */
    @Override
    public CompletableFuture<Result<YdbQuery.FetchScriptResultsResponse>> fetchScriptResults(
            YdbQuery.FetchScriptResultsRequest request, GrpcRequestSettings settings) {

        return transport
                .unaryCall(QueryServiceGrpc.getFetchScriptResultsMethod(), settings, request);
    }
}
