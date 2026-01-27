package tech.ydb.query.script.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.operation.v1.OperationServiceGrpc;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.query.v1.QueryServiceGrpc;

/**
 * Default gRPC-based implementation of {@link ScriptRpc}.
 * <p>
 * Uses {@link GrpcTransport} to communicate with YDB QueryService and OperationService.
 * Provides async unary calls for executing scripts and retrieving results or operation metadata.
 *
 * <p>Author: Evgeny Kuvardin
 */
public class ScriptRpcImpl implements ScriptRpc {

    private final GrpcTransport transport;

    private ScriptRpcImpl(GrpcTransport grpcTransport) {
        this.transport = grpcTransport;
    }

    /**
     * Creates a new RPC instance bound to the given gRPC transport.
     *
     * @param grpcTransport transport instance (not closed by this class)
     * @return new {@link ScriptRpcImpl} instance
     */
    public static ScriptRpcImpl useTransport(@WillNotClose GrpcTransport grpcTransport) {
        return new ScriptRpcImpl(grpcTransport);
    }

    @Override
    public CompletableFuture<Operation<Status>> getOperation(String operationId, GrpcRequestSettings settings) {
        OperationProtos.GetOperationRequest request = OperationProtos.GetOperationRequest.newBuilder()
                .setId(operationId)
                .build();

        return transport
                .unaryCall(OperationServiceGrpc.getGetOperationMethod(), settings, request)
                .thenApply(OperationBinder.bindAsync(transport, OperationProtos.GetOperationResponse::getOperation));
    }

    @Override
    public CompletableFuture<Operation<Status>> executeScript(
            YdbQuery.ExecuteScriptRequest request, GrpcRequestSettings settings) {

        return transport.unaryCall(QueryServiceGrpc.getExecuteScriptMethod(), settings, request)
                .thenApply(OperationBinder.bindAsync(transport, Function.identity()));
    }

    @Override
    public CompletableFuture<Result<YdbQuery.FetchScriptResultsResponse>> fetchScriptResults(
            YdbQuery.FetchScriptResultsRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getFetchScriptResultsMethod(), settings, request);
    }
}
