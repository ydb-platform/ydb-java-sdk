package tech.ydb.table.rpc.grpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.core.operation.StatusExtractor;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.proto.table.v1.TableServiceGrpc;
import tech.ydb.table.rpc.TableRpc;

/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public final class GrpcTableRpc implements TableRpc {
    private final GrpcTransport transport;
    private final boolean transportOwned;

    private static final StatusExtractor<YdbTable.ReadRowsResponse> READ_ROWS = StatusExtractor.of(
            YdbTable.ReadRowsResponse::getStatus, YdbTable.ReadRowsResponse::getIssuesList);

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
    public CompletableFuture<Result<YdbTable.CreateSessionResult>> createSession(
            YdbTable.CreateSessionRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getCreateSessionMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(
                        YdbTable.CreateSessionResponse::getOperation, YdbTable.CreateSessionResult.class
                ));
    }

    @Override
    public CompletableFuture<Status> deleteSession(
            YdbTable.DeleteSessionRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getDeleteSessionMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.DeleteSessionResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<YdbTable.KeepAliveResult>> keepAlive(
            YdbTable.KeepAliveRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getKeepAliveMethod(), settings, request)
                .thenApply(OperationBinder
                        .bindSync(YdbTable.KeepAliveResponse::getOperation, YdbTable.KeepAliveResult.class)
                );
    }

    @Override
    public CompletableFuture<Status> createTable(YdbTable.CreateTableRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCreateTableMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.CreateTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> dropTable(YdbTable.DropTableRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getDropTableMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.DropTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> alterTable(YdbTable.AlterTableRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getAlterTableMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.AlterTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> copyTable(YdbTable.CopyTableRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCopyTableMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.CopyTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> copyTables(YdbTable.CopyTablesRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCopyTablesMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.CopyTablesResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> renameTables(YdbTable.RenameTablesRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getRenameTablesMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.RenameTablesResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<YdbTable.DescribeTableResult>> describeTable(
            YdbTable.DescribeTableRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getDescribeTableMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(
                        YdbTable.DescribeTableResponse::getOperation, YdbTable.DescribeTableResult.class
                ));
    }

    @Override
    public CompletableFuture<Result<YdbTable.ExplainQueryResult>> explainDataQuery(
            YdbTable.ExplainDataQueryRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getExplainDataQueryMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(
                        YdbTable.ExplainDataQueryResponse::getOperation, YdbTable.ExplainQueryResult.class
                ));
    }

    @Override
    public CompletableFuture<Result<YdbTable.PrepareQueryResult>> prepareDataQuery(
            YdbTable.PrepareDataQueryRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getPrepareDataQueryMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(
                        YdbTable.PrepareDataQueryResponse::getOperation, YdbTable.PrepareQueryResult.class
                ));
    }

    @Override
    public CompletableFuture<Result<YdbTable.ExecuteQueryResult>> executeDataQuery(
            YdbTable.ExecuteDataQueryRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getExecuteDataQueryMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(
                        YdbTable.ExecuteDataQueryResponse::getOperation, YdbTable.ExecuteQueryResult.class
                ));
    }

    @Override
    public CompletableFuture<Result<YdbTable.ReadRowsResponse>> readRows(
            YdbTable.ReadRowsRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getReadRowsMethod(), settings, request)
                .thenApply(READ_ROWS);
    }

    @Override
    public CompletableFuture<Status> executeSchemeQuery(YdbTable.ExecuteSchemeQueryRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getExecuteSchemeQueryMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.ExecuteSchemeQueryResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<YdbTable.BeginTransactionResult>> beginTransaction(
            YdbTable.BeginTransactionRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getBeginTransactionMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(
                        YdbTable.BeginTransactionResponse::getOperation, YdbTable.BeginTransactionResult.class
                ));
    }

    @Override
    public CompletableFuture<Status> commitTransaction(
            YdbTable.CommitTransactionRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getCommitTransactionMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.CommitTransactionResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> rollbackTransaction(
            YdbTable.RollbackTransactionRequest request, GrpcRequestSettings settings
    ) {
        return transport
                .unaryCall(TableServiceGrpc.getRollbackTransactionMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.RollbackTransactionResponse::getOperation));
    }

    @Override
    public GrpcReadStream<YdbTable.ReadTableResponse> streamReadTable(
            YdbTable.ReadTableRequest request, GrpcRequestSettings settings
    ) {
        return transport.readStreamCall(TableServiceGrpc.getStreamReadTableMethod(), settings, request);
    }

    @Override
    public GrpcReadStream<YdbTable.ExecuteScanQueryPartialResponse> streamExecuteScanQuery(
            YdbTable.ExecuteScanQueryRequest request, GrpcRequestSettings settings
    ) {
        return transport.readStreamCall(TableServiceGrpc.getStreamExecuteScanQueryMethod(), settings, request);
    }

    @Override
    public CompletableFuture<Status> bulkUpsert(YdbTable.BulkUpsertRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getBulkUpsertMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(YdbTable.BulkUpsertResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<YdbTable.DescribeTableOptionsResult>> describeTableOptions(
            YdbTable.DescribeTableOptionsRequest request, GrpcRequestSettings settings
    ) {
        return transport.unaryCall(TableServiceGrpc.getDescribeTableOptionsMethod(), settings, request)
                .thenApply(OperationBinder.bindSync(
                        YdbTable.DescribeTableOptionsResponse::getOperation, YdbTable.DescribeTableOptionsResult.class
                ));
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
