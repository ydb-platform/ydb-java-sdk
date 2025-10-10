package tech.ydb.query.impl;

import java.util.concurrent.CompletableFuture;

import io.grpc.Context;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.core.operation.StatusExtractor;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.query.v1.QueryServiceGrpc;
import tech.ydb.proto.scripting.ScriptingProtos;
import tech.ydb.proto.scripting.v1.ScriptingServiceGrpc;

/**
 * Low-level RPC client for YDB Query and Scripting services.
 * <p>
 * Provides direct gRPC bindings for session management, query execution,
 * transaction control, and script execution APIs.
 * <p>
 * Used internally by higher-level query session and client abstractions.
 *
 * @author Aleksandr Gorshenin
 */
class QueryServiceRpc {
    private static final StatusExtractor<YdbQuery.CreateSessionResponse> CREATE_SESSION = StatusExtractor.of(
            YdbQuery.CreateSessionResponse::getStatus,
            YdbQuery.CreateSessionResponse::getIssuesList
    );

    private static final StatusExtractor<YdbQuery.DeleteSessionResponse> DELETE_SESSION = StatusExtractor.of(
            YdbQuery.DeleteSessionResponse::getStatus,
            YdbQuery.DeleteSessionResponse::getIssuesList
    );

    private static final StatusExtractor<YdbQuery.BeginTransactionResponse> BEGIN_TX = StatusExtractor.of(
            YdbQuery.BeginTransactionResponse::getStatus,
            YdbQuery.BeginTransactionResponse::getIssuesList
    );

    private static final StatusExtractor<YdbQuery.CommitTransactionResponse> COMMIT_TX = StatusExtractor.of(
            YdbQuery.CommitTransactionResponse::getStatus,
            YdbQuery.CommitTransactionResponse::getIssuesList
    );

    private static final StatusExtractor<YdbQuery.RollbackTransactionResponse> ROLLBACK_TX = StatusExtractor.of(
            YdbQuery.RollbackTransactionResponse::getStatus,
            YdbQuery.RollbackTransactionResponse::getIssuesList
    );

    private static final StatusExtractor<YdbQuery.FetchScriptResultsResponse> FETCH_SCRIPT = StatusExtractor.of(
            YdbQuery.FetchScriptResultsResponse::getStatus,
            YdbQuery.FetchScriptResultsResponse::getIssuesList
    );

    private final GrpcTransport transport;

    QueryServiceRpc(GrpcTransport transport) {
        this.transport = transport;
    }

    public CompletableFuture<Result<YdbQuery.CreateSessionResponse>> createSession(
            YdbQuery.CreateSessionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getCreateSessionMethod(), settings, request)
                .thenApply(CREATE_SESSION);
    }

    public CompletableFuture<Result<YdbQuery.DeleteSessionResponse>> deleteSession(
            YdbQuery.DeleteSessionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getDeleteSessionMethod(), settings, request)
                .thenApply(DELETE_SESSION);
    }

    public GrpcReadStream<YdbQuery.SessionState> attachSession(
            YdbQuery.AttachSessionRequest request, GrpcRequestSettings settings) {
        // Execute attachSession call outside current context to avoid span propogation
        Context ctx = Context.ROOT.fork();
        Context previous = ctx.attach();
        try {
            return transport.readStreamCall(QueryServiceGrpc.getAttachSessionMethod(), settings, request);
        } finally {
            ctx.detach(previous);
        }
    }

    public CompletableFuture<Result<YdbQuery.BeginTransactionResponse>> beginTransaction(
            YdbQuery.BeginTransactionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getBeginTransactionMethod(), settings, request)
                .thenApply(BEGIN_TX);
    }

    public CompletableFuture<Result<YdbQuery.CommitTransactionResponse>> commitTransaction(
            YdbQuery.CommitTransactionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getCommitTransactionMethod(), settings, request)
                .thenApply(COMMIT_TX);
    }

    public CompletableFuture<Result<YdbQuery.RollbackTransactionResponse>> rollbackTransaction(
            YdbQuery.RollbackTransactionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getRollbackTransactionMethod(), settings, request)
                .thenApply(ROLLBACK_TX);
    }

    public GrpcReadStream<YdbQuery.ExecuteQueryResponsePart> executeQuery(
            YdbQuery.ExecuteQueryRequest request, GrpcRequestSettings settings) {
        return transport.readStreamCall(QueryServiceGrpc.getExecuteQueryMethod(), settings, request);
    }

    /**
     * Executes a YQL script via the scripting service.
     *
     * @param request  the {@link ScriptingProtos.ExecuteYqlRequest} containing the script definition
     * @param settings gRPC request settings
     * @return a future resolving to an {@link Operation} with {@link ScriptingProtos.ExecuteYqlResult}
     */
    public CompletableFuture<Operation<Result<ScriptingProtos.ExecuteYqlResult>>> executeScriptYql(
            ScriptingProtos.ExecuteYqlRequest request, GrpcRequestSettings settings) {

        return transport.unaryCall(ScriptingServiceGrpc.getExecuteYqlMethod(), settings, request)
                .thenApply(OperationBinder.bindAsync(
                        transport,
                        ScriptingProtos.ExecuteYqlResponse::getOperation,
                        ScriptingProtos.ExecuteYqlResult.class)
                );
    }

    /**
     * Executes a YQL script using the Query service API.
     *
     *
     * @param request  the {@link YdbQuery.ExecuteScriptRequest} containing the script
     * @param settings gRPC request settings
     * @return a future resolving to an {@link Operation} representing the script execution
     */
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
    public CompletableFuture<Result<YdbQuery.FetchScriptResultsResponse>> fetchScriptResults(
            YdbQuery.FetchScriptResultsRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getFetchScriptResultsMethod(), settings, request)
                .thenApply(FETCH_SCRIPT);
    }
}
