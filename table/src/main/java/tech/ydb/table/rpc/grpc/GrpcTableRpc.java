package tech.ydb.table.rpc.grpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;

import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.table.YdbTable;
import tech.ydb.table.YdbTable.AlterTableRequest;
import tech.ydb.table.YdbTable.AlterTableResponse;
import tech.ydb.table.YdbTable.BeginTransactionRequest;
import tech.ydb.table.YdbTable.BeginTransactionResponse;
import tech.ydb.table.YdbTable.BeginTransactionResult;
import tech.ydb.table.YdbTable.BulkUpsertResponse;
import tech.ydb.table.YdbTable.CommitTransactionRequest;
import tech.ydb.table.YdbTable.CommitTransactionResponse;
import tech.ydb.table.YdbTable.CopyTableRequest;
import tech.ydb.table.YdbTable.CopyTableResponse;
import tech.ydb.table.YdbTable.CopyTablesRequest;
import tech.ydb.table.YdbTable.CopyTablesResponse;
import tech.ydb.table.YdbTable.CreateTableRequest;
import tech.ydb.table.YdbTable.CreateTableResponse;
import tech.ydb.table.YdbTable.DeleteSessionRequest;
import tech.ydb.table.YdbTable.DeleteSessionResponse;
import tech.ydb.table.YdbTable.DescribeTableRequest;
import tech.ydb.table.YdbTable.DescribeTableResponse;
import tech.ydb.table.YdbTable.DescribeTableResult;
import tech.ydb.table.YdbTable.DropTableRequest;
import tech.ydb.table.YdbTable.DropTableResponse;
import tech.ydb.table.YdbTable.ExecuteDataQueryRequest;
import tech.ydb.table.YdbTable.ExecuteDataQueryResponse;
import tech.ydb.table.YdbTable.ExecuteQueryResult;
import tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest;
import tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse;
import tech.ydb.table.YdbTable.ExplainDataQueryRequest;
import tech.ydb.table.YdbTable.ExplainDataQueryResponse;
import tech.ydb.table.YdbTable.ExplainQueryResult;
import tech.ydb.table.YdbTable.KeepAliveRequest;
import tech.ydb.table.YdbTable.KeepAliveResponse;
import tech.ydb.table.YdbTable.KeepAliveResult;
import tech.ydb.table.YdbTable.PrepareDataQueryRequest;
import tech.ydb.table.YdbTable.PrepareDataQueryResponse;
import tech.ydb.table.YdbTable.PrepareQueryResult;
import tech.ydb.table.YdbTable.RollbackTransactionRequest;
import tech.ydb.table.YdbTable.RollbackTransactionResponse;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.v1.TableServiceGrpc;

/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public final class GrpcTableRpc implements TableRpc {
    private final GrpcTransport transport;
    private final boolean transportOwned;

    private GrpcTableRpc(GrpcTransport transport, boolean transportOwned) {
        this.transport = transport;
        this.transportOwned = transportOwned;
    }

    @Nullable
    public static GrpcTableRpc useTransport(@WillNotClose GrpcTransport transport) {
        return new GrpcTableRpc(transport, false);
    }

    @Nullable
    public static GrpcTableRpc ownTransport(@WillClose GrpcTransport transport) {
        return new GrpcTableRpc(transport, true);
    }

    @Override
    public CompletableFuture<Result<YdbTable.CreateSessionResult>> createSession(YdbTable.CreateSessionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCreateSessionMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(
                        YdbTable.CreateSessionResponse::getOperation,
                        YdbTable.CreateSessionResult.class));
    }

    @Override
    public CompletableFuture<Status> deleteSession(DeleteSessionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getDeleteSessionMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(DeleteSessionResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<KeepAliveResult>> keepAlive(KeepAliveRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getKeepAliveMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(KeepAliveResponse::getOperation, KeepAliveResult.class));
    }

    @Override
    public CompletableFuture<Status> createTable(CreateTableRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCreateTableMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(CreateTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> dropTable(DropTableRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getDropTableMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(DropTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> alterTable(AlterTableRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getAlterTableMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(AlterTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> copyTable(CopyTableRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCopyTableMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(CopyTableResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> copyTables(CopyTablesRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCopyTablesMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(CopyTablesResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<DescribeTableResult>> describeTable(DescribeTableRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getDescribeTableMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(DescribeTableResponse::getOperation, DescribeTableResult.class));
    }

    @Override
    public CompletableFuture<Result<ExplainQueryResult>> explainDataQuery(ExplainDataQueryRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getExplainDataQueryMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(
                        ExplainDataQueryResponse::getOperation, ExplainQueryResult.class)
                );
    }

    @Override
    public CompletableFuture<Result<PrepareQueryResult>> prepareDataQuery(
            PrepareDataQueryRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getPrepareDataQueryMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(
                        PrepareDataQueryResponse::getOperation, PrepareQueryResult.class
                ));
    }

    @Override
    public CompletableFuture<Result<ExecuteQueryResult>> executeDataQuery(ExecuteDataQueryRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getExecuteDataQueryMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(
                        ExecuteDataQueryResponse::getOperation, ExecuteQueryResult.class
                ));
    }

    @Override
    public CompletableFuture<Status> executeSchemeQuery(ExecuteSchemeQueryRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getExecuteSchemeQueryMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(ExecuteSchemeQueryResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<BeginTransactionResult>> beginTransaction(BeginTransactionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getBeginTransactionMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(
                        BeginTransactionResponse::getOperation, BeginTransactionResult.class
                ));
    }

    @Override
    public CompletableFuture<Status> commitTransaction(CommitTransactionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getCommitTransactionMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(CommitTransactionResponse::getOperation));
    }

    @Override
    public CompletableFuture<Status> rollbackTransaction(RollbackTransactionRequest request,
            GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getRollbackTransactionMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(RollbackTransactionResponse::getOperation));
    }

    @Override
    public GrpcReadStream<YdbTable.ReadTableResponse> streamReadTable(
            YdbTable.ReadTableRequest request, GrpcRequestSettings settings) {
        return transport.serverStreamCall(TableServiceGrpc.getStreamReadTableMethod(), settings, request);
    }

    @Override
    public GrpcReadStream<YdbTable.ExecuteScanQueryPartialResponse> streamExecuteScanQuery(
            YdbTable.ExecuteScanQueryRequest request, GrpcRequestSettings settings) {
        return transport.serverStreamCall(TableServiceGrpc.getStreamExecuteScanQueryMethod(), settings, request);
    }

    @Override
    public CompletableFuture<Status> bulkUpsert(YdbTable.BulkUpsertRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(TableServiceGrpc.getBulkUpsertMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(BulkUpsertResponse::getOperation));
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
