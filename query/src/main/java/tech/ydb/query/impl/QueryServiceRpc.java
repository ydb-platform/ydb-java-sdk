package tech.ydb.query.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.query.v1.QueryServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryServiceRpc {
    private static final StatusExtract<YdbQuery.CreateSessionResponse> CREATE_SESSION = StatusExtract.of(
            YdbQuery.CreateSessionResponse::getStatus,
            YdbQuery.CreateSessionResponse::getIssuesList
    );

    private static final StatusExtract<YdbQuery.DeleteSessionResponse> DELETE_SESSION = StatusExtract.of(
            YdbQuery.DeleteSessionResponse::getStatus,
            YdbQuery.DeleteSessionResponse::getIssuesList
    );

    private static final StatusExtract<YdbQuery.BeginTransactionResponse> BEGIN_TRANSACTION = StatusExtract.of(
            YdbQuery.BeginTransactionResponse::getStatus,
            YdbQuery.BeginTransactionResponse::getIssuesList
    );

    private static final StatusExtract<YdbQuery.CommitTransactionResponse> COMMIT_TRANSACTION = StatusExtract.of(
            YdbQuery.CommitTransactionResponse::getStatus,
            YdbQuery.CommitTransactionResponse::getIssuesList
    );

    private static final StatusExtract<YdbQuery.RollbackTransactionResponse> ROLLBACK_TRANSACTION = StatusExtract.of(
            YdbQuery.RollbackTransactionResponse::getStatus,
            YdbQuery.RollbackTransactionResponse::getIssuesList
    );

    private static final StatusExtract<YdbQuery.FetchScriptResultsResponse> FETCH_SCRIPT = StatusExtract.of(
            YdbQuery.FetchScriptResultsResponse::getStatus,
            YdbQuery.FetchScriptResultsResponse::getIssuesList
    );

    private final GrpcTransport transport;

    public QueryServiceRpc(GrpcTransport transport) {
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
        return transport.readStreamCall(QueryServiceGrpc.getAttachSessionMethod(), settings, request);
    }

    public CompletableFuture<Result<YdbQuery.BeginTransactionResponse>> beginTransaction(
            YdbQuery.BeginTransactionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getBeginTransactionMethod(), settings, request)
                .thenApply(BEGIN_TRANSACTION);
    }

    public CompletableFuture<Result<YdbQuery.CommitTransactionResponse>> commitTransaction(
            YdbQuery.CommitTransactionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getCommitTransactionMethod(), settings, request)
                .thenApply(COMMIT_TRANSACTION);
    }

    public CompletableFuture<Result<YdbQuery.RollbackTransactionResponse>> rollbackTransaction(
            YdbQuery.RollbackTransactionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getRollbackTransactionMethod(), settings, request)
                .thenApply(ROLLBACK_TRANSACTION);
    }

    public GrpcReadStream<YdbQuery.ExecuteQueryResponsePart> executeQuery(
            YdbQuery.ExecuteQueryRequest request, GrpcRequestSettings settings) {
        return transport.readStreamCall(QueryServiceGrpc.getExecuteQueryMethod(), settings, request);
    }

    public CompletableFuture<Result<OperationProtos.Operation>> executeScript(
            YdbQuery.ExecuteScriptRequest request, GrpcRequestSettings settings) {
        return transport.unaryCall(QueryServiceGrpc.getExecuteScriptMethod(), settings, request);
    }

    public CompletableFuture<Result<YdbQuery.FetchScriptResultsResponse>> fetchScriptResults(
            YdbQuery.FetchScriptResultsRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getFetchScriptResultsMethod(), settings, request)
                .thenApply(FETCH_SCRIPT);
    }
}
