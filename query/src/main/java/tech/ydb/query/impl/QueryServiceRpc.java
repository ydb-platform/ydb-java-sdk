package tech.ydb.query.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.YdbIssueMessage;
import tech.ydb.proto.draft.query.YdbQuery;
import tech.ydb.proto.draft.query.v1.QueryServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryServiceRpc {
    private static final Mapper<YdbQuery.CreateSessionResponse> CREATE_SESSION_MAPPER = new Mapper<>(
            YdbQuery.CreateSessionResponse::getStatus,
            YdbQuery.CreateSessionResponse::getIssuesList
    );

    private static final Mapper<YdbQuery.DeleteSessionResponse> DELETE_SESSION_MAPPER = new Mapper<>(
            YdbQuery.DeleteSessionResponse::getStatus,
            YdbQuery.DeleteSessionResponse::getIssuesList
    );

    private static final Mapper<YdbQuery.BeginTransactionResponse> BEGIN_TRANSACTION_MAPPER = new Mapper<>(
            YdbQuery.BeginTransactionResponse::getStatus,
            YdbQuery.BeginTransactionResponse::getIssuesList
    );

    private static final Mapper<YdbQuery.CommitTransactionResponse> COMMIT_TRANSACTION_MAPPER = new Mapper<>(
            YdbQuery.CommitTransactionResponse::getStatus,
            YdbQuery.CommitTransactionResponse::getIssuesList
    );

    private static final Mapper<YdbQuery.RollbackTransactionResponse> ROLLBACK_TRANSACTION_MAPPER = new Mapper<>(
            YdbQuery.RollbackTransactionResponse::getStatus,
            YdbQuery.RollbackTransactionResponse::getIssuesList
    );

    private static final Mapper<YdbQuery.FetchScriptResultsResponse> FETCH_SCRIPT_MAPPER = new Mapper<>(
            YdbQuery.FetchScriptResultsResponse::getStatus,
            YdbQuery.FetchScriptResultsResponse::getIssuesList
    );

    private static final Mapper<YdbQuery.SaveScriptResponse> SAVE_SCRIPT_MAPPER = new Mapper<>(
            YdbQuery.SaveScriptResponse::getStatus,
            YdbQuery.SaveScriptResponse::getIssuesList
    );

    private static final Mapper<YdbQuery.ListScriptsResponse> LIST_SCRIPTS_MAPPER = new Mapper<>(
            YdbQuery.ListScriptsResponse::getStatus,
            YdbQuery.ListScriptsResponse::getIssuesList
    );

    private static final Mapper<YdbQuery.DeleteScriptResponse> DELETE_SCRIPT_MAPPER = new Mapper<>(
            YdbQuery.DeleteScriptResponse::getStatus,
            YdbQuery.DeleteScriptResponse::getIssuesList
    );

    private final GrpcTransport transport;

    public QueryServiceRpc(GrpcTransport transport) {
        this.transport = transport;
    }

    public CompletableFuture<Result<YdbQuery.CreateSessionResponse>> createSession(
            YdbQuery.CreateSessionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getCreateSessionMethod(), settings, request)
                .thenApply(CREATE_SESSION_MAPPER);
    }

    public CompletableFuture<Result<YdbQuery.DeleteSessionResponse>> deleteSession(
            YdbQuery.DeleteSessionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getDeleteSessionMethod(), settings, request)
                .thenApply(DELETE_SESSION_MAPPER);
    }

    public GrpcReadStream<YdbQuery.SessionState> attachSession(
            YdbQuery.AttachSessionRequest request, GrpcRequestSettings settings) {
        return transport.readStreamCall(QueryServiceGrpc.getAttachSessionMethod(), settings, request);
    }

    public CompletableFuture<Result<YdbQuery.BeginTransactionResponse>> beginTransaction(
            YdbQuery.BeginTransactionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getBeginTransactionMethod(), settings, request)
                .thenApply(BEGIN_TRANSACTION_MAPPER);
    }

    public CompletableFuture<Result<YdbQuery.CommitTransactionResponse>> commitTransaction(
            YdbQuery.CommitTransactionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getCommitTransactionMethod(), settings, request)
                .thenApply(COMMIT_TRANSACTION_MAPPER);
    }

    public CompletableFuture<Result<YdbQuery.RollbackTransactionResponse>> rollbackTransaction(
            YdbQuery.RollbackTransactionRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getRollbackTransactionMethod(), settings, request)
                .thenApply(ROLLBACK_TRANSACTION_MAPPER);
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
                .thenApply(FETCH_SCRIPT_MAPPER);
    }

    public CompletableFuture<Result<YdbQuery.SaveScriptResponse>> saveScript(
            YdbQuery.SaveScriptRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getSaveScriptMethod(), settings, request)
                .thenApply(SAVE_SCRIPT_MAPPER);
    }

    public CompletableFuture<Result<YdbQuery.DeleteScriptResponse>> deleteScript(
            YdbQuery.DeleteScriptRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getDeleteScriptMethod(), settings, request)
                .thenApply(DELETE_SCRIPT_MAPPER);
    }

    public CompletableFuture<Result<YdbQuery.ListScriptsResponse>> listScripts(
            YdbQuery.ListScriptsRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(QueryServiceGrpc.getListScriptsMethod(), settings, request)
                .thenApply(LIST_SCRIPTS_MAPPER);
    }


    private static class Mapper<T> implements Function<Result<T>, Result<T>> {
        private final Function<T, StatusCodesProtos.StatusIds.StatusCode> statusFunctor;
        private final Function<T, List<YdbIssueMessage.IssueMessage>> issuesFunctor;

        public Mapper(
                Function<T, StatusCodesProtos.StatusIds.StatusCode> statusFunctor,
                Function<T, List<YdbIssueMessage.IssueMessage>> messagesFunctor) {
            this.statusFunctor = statusFunctor;
            this.issuesFunctor = messagesFunctor;
        }

        @Override
        public Result<T> apply(Result<T> result) {
            if (!result.isSuccess()) {
                return result;
            }

            T resp = result.getValue();
            Status status = Status.of(
                    StatusCode.fromProto(statusFunctor.apply(resp)),
                    result.getStatus().getConsumedRu(),
                    Issue.fromPb(issuesFunctor.apply(resp))
            );
            return Result.success(resp, status);
        }
    }
}
