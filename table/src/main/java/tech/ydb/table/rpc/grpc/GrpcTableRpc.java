package tech.ydb.table.rpc.grpc;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;


import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.OperationManager;
import tech.ydb.proto.StatusCodesProtos.StatusIds;
import tech.ydb.proto.YdbIssueMessage.IssueMessage;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.proto.table.YdbTable.AlterTableRequest;
import tech.ydb.proto.table.YdbTable.AlterTableResponse;
import tech.ydb.proto.table.YdbTable.BeginTransactionRequest;
import tech.ydb.proto.table.YdbTable.BeginTransactionResponse;
import tech.ydb.proto.table.YdbTable.BeginTransactionResult;
import tech.ydb.proto.table.YdbTable.BulkUpsertResponse;
import tech.ydb.proto.table.YdbTable.CommitTransactionRequest;
import tech.ydb.proto.table.YdbTable.CommitTransactionResponse;
import tech.ydb.proto.table.YdbTable.CopyTableRequest;
import tech.ydb.proto.table.YdbTable.CopyTableResponse;
import tech.ydb.proto.table.YdbTable.CopyTablesRequest;
import tech.ydb.proto.table.YdbTable.CopyTablesResponse;
import tech.ydb.proto.table.YdbTable.CreateTableRequest;
import tech.ydb.proto.table.YdbTable.CreateTableResponse;
import tech.ydb.proto.table.YdbTable.DeleteSessionRequest;
import tech.ydb.proto.table.YdbTable.DeleteSessionResponse;
import tech.ydb.proto.table.YdbTable.DescribeTableRequest;
import tech.ydb.proto.table.YdbTable.DescribeTableResponse;
import tech.ydb.proto.table.YdbTable.DescribeTableResult;
import tech.ydb.proto.table.YdbTable.DropTableRequest;
import tech.ydb.proto.table.YdbTable.DropTableResponse;
import tech.ydb.proto.table.YdbTable.ExecuteDataQueryRequest;
import tech.ydb.proto.table.YdbTable.ExecuteDataQueryResponse;
import tech.ydb.proto.table.YdbTable.ExecuteQueryResult;
import tech.ydb.proto.table.YdbTable.ExecuteSchemeQueryRequest;
import tech.ydb.proto.table.YdbTable.ExecuteSchemeQueryResponse;
import tech.ydb.proto.table.YdbTable.ExplainDataQueryRequest;
import tech.ydb.proto.table.YdbTable.ExplainDataQueryResponse;
import tech.ydb.proto.table.YdbTable.ExplainQueryResult;
import tech.ydb.proto.table.YdbTable.KeepAliveRequest;
import tech.ydb.proto.table.YdbTable.KeepAliveResponse;
import tech.ydb.proto.table.YdbTable.KeepAliveResult;
import tech.ydb.proto.table.YdbTable.PrepareDataQueryRequest;
import tech.ydb.proto.table.YdbTable.PrepareDataQueryResponse;
import tech.ydb.proto.table.YdbTable.PrepareQueryResult;
import tech.ydb.proto.table.YdbTable.ReadRowsRequest;
import tech.ydb.proto.table.YdbTable.ReadRowsResponse;
import tech.ydb.proto.table.YdbTable.RollbackTransactionRequest;
import tech.ydb.proto.table.YdbTable.RollbackTransactionResponse;
import tech.ydb.proto.table.v1.TableServiceGrpc;
import tech.ydb.table.rpc.TableRpc;

/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public final class GrpcTableRpc implements TableRpc {
    private final GrpcTransport transport;
    private final boolean transportOwned;

    private static final StatusExtractor<ReadRowsResponse> READ_ROWS = new StatusExtractor<>(
        response -> response.getStatus() == StatusIds.StatusCode.STATUS_CODE_UNSPECIFIED ?
                StatusIds.StatusCode.SUCCESS : response.getStatus(),
        ReadRowsResponse::getIssuesList
    );

    private GrpcTableRpc(GrpcTransport transport, boolean transportOwned) {
        this.transport = transport;
        this.transportOwned = transportOwned;
    }

    public static GrpcTableRpc useTransport(@WillNotClose GrpcTransport transport) {
        return new GrpcTableRpc(transport, false);
    }

    public static GrpcTableRpc ownTransport(@WillClose GrpcTransport transport) {
        return new GrpcTableRpc(transport, true);
    }

    @Override
    public CompletableFuture<Result<YdbTable.CreateSessionResult>> createSession(YdbTable.CreateSessionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCreateSessionMethod(), settings, request)
                .thenApply(OperationManager.syncResultUnwrapper(
                        YdbTable.CreateSessionResponse::getOperation,
                        YdbTable.CreateSessionResult.class));
    }

    @Override
    public CompletableFuture<Status> deleteSession(DeleteSessionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getDeleteSessionMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(DeleteSessionResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<KeepAliveResult>> keepAlive(KeepAliveRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getKeepAliveMethod(), settings, request)
                .thenApply(OperationManager.syncResultUnwrapper(
                        KeepAliveResponse::getOperation, KeepAliveResult.class));
    }

    @Override
    public CompletableFuture<Status> createTable(CreateTableRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCreateTableMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(CreateTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> dropTable(DropTableRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getDropTableMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(DropTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> alterTable(AlterTableRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getAlterTableMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(AlterTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> copyTable(CopyTableRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCopyTableMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(CopyTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> copyTables(CopyTablesRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCopyTablesMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(CopyTablesResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<DescribeTableResult>> describeTable(DescribeTableRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getDescribeTableMethod(), settings, request)
                .thenApply(OperationManager.syncResultUnwrapper(
                        DescribeTableResponse::getOperation, DescribeTableResult.class));
    }

    @Override
    public CompletableFuture<Result<ExplainQueryResult>> explainDataQuery(ExplainDataQueryRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getExplainDataQueryMethod(), settings, request)
                .thenApply(OperationManager.syncResultUnwrapper(
                        ExplainDataQueryResponse::getOperation, ExplainQueryResult.class)
                );
    }

    @Override
    public CompletableFuture<Result<PrepareQueryResult>> prepareDataQuery(
            PrepareDataQueryRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getPrepareDataQueryMethod(), settings, request)
                .thenApply(OperationManager.syncResultUnwrapper(
                        PrepareDataQueryResponse::getOperation, PrepareQueryResult.class
                ));
    }

    @Override
    public CompletableFuture<Result<ExecuteQueryResult>> executeDataQuery(ExecuteDataQueryRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getExecuteDataQueryMethod(), settings, request)
                .thenApply(OperationManager.syncResultUnwrapper(
                        ExecuteDataQueryResponse::getOperation, ExecuteQueryResult.class
                ));
    }

    @Override
    public CompletableFuture<Result<ReadRowsResponse>> readRows(ReadRowsRequest request,
                                                                     GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getReadRowsMethod(), settings, request)
                .thenApply(READ_ROWS);
    }

    @Override
    public CompletableFuture<Status> executeSchemeQuery(ExecuteSchemeQueryRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getExecuteSchemeQueryMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(ExecuteSchemeQueryResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<BeginTransactionResult>> beginTransaction(BeginTransactionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getBeginTransactionMethod(), settings, request)
                .thenApply(OperationManager.syncResultUnwrapper(
                        BeginTransactionResponse::getOperation, BeginTransactionResult.class
                ));
    }

    @Override
    public CompletableFuture<Status> commitTransaction(CommitTransactionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCommitTransactionMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(CommitTransactionResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> rollbackTransaction(RollbackTransactionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getRollbackTransactionMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(RollbackTransactionResponse::getOperation));
    }

    @Override
    public GrpcReadStream<YdbTable.ReadTableResponse> streamReadTable(
            YdbTable.ReadTableRequest request, GrpcRequestSettings settings) {
        return transport.readStreamCall(TableServiceGrpc.getStreamReadTableMethod(), settings, request);
    }

    @Override
    public GrpcReadStream<YdbTable.ExecuteScanQueryPartialResponse> streamExecuteScanQuery(
            YdbTable.ExecuteScanQueryRequest request, GrpcRequestSettings settings) {
        return transport.readStreamCall(TableServiceGrpc.getStreamExecuteScanQueryMethod(), settings, request);
    }

    @Override
    public CompletableFuture<Status> bulkUpsert(YdbTable.BulkUpsertRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getBulkUpsertMethod(), settings, request)
                .thenApply(OperationManager.syncStatusUnwrapper(BulkUpsertResponse::getOperation));
    }

    @Override
    public String getDatabase() {
        return transport.getDatabase();
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return transport.getScheduler();
    }

    @Override
    public void close() {
        if (transportOwned) {
            transport.close();
        }
    }
}

class StatusExtractor<R> implements Function<Result<R>, Result<R>> {
    private final Function<R, StatusIds.StatusCode> statusCodeExtractor;
    private final Function<R, List<IssueMessage>> issueListExtractor;

    StatusExtractor(Function<R, StatusIds.StatusCode> statusCodeExtractor,
                    Function<R, List<IssueMessage>> issueListExtractor) {
        this.statusCodeExtractor = statusCodeExtractor;
        this.issueListExtractor = issueListExtractor;
    }

    public Function<R, StatusIds.StatusCode> getStatusCodeExtractor() {
        return statusCodeExtractor;
    }

    public Function<R, List<IssueMessage>> getIssueListExtractor() {
        return issueListExtractor;
    }

    @Override
    public Result<R> apply(Result<R> result) {
        if (!result.isSuccess()) {
            return result.map(null);
        }
        final Status status = Status.of(StatusCode.fromProto(statusCodeExtractor.apply(result.getValue())));
        final List<IssueMessage> issueMessageList = issueListExtractor.apply(result.getValue());
        if (!issueMessageList.isEmpty()) {
            status.withIssues((Issue[]) issueMessageList.stream().map(Issue::fromPb).toArray());
        }

        return Result.success(result.getValue(), status);
    }
}
