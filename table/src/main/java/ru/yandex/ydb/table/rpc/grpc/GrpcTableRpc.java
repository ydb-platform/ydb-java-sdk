package ru.yandex.ydb.table.rpc.grpc;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.grpc.GrpcTransport;
import ru.yandex.ydb.core.rpc.RpcTransport;
import ru.yandex.ydb.table.rpc.TableRpc;
import ru.yandex.ydb.table.v1.TableServiceGrpc;

import static ru.yandex.ydb.table.YdbTable.AlterTableRequest;
import static ru.yandex.ydb.table.YdbTable.AlterTableResponse;
import static ru.yandex.ydb.table.YdbTable.BeginTransactionRequest;
import static ru.yandex.ydb.table.YdbTable.BeginTransactionResponse;
import static ru.yandex.ydb.table.YdbTable.CommitTransactionRequest;
import static ru.yandex.ydb.table.YdbTable.CommitTransactionResponse;
import static ru.yandex.ydb.table.YdbTable.CopyTableRequest;
import static ru.yandex.ydb.table.YdbTable.CopyTableResponse;
import static ru.yandex.ydb.table.YdbTable.CreateSessionRequest;
import static ru.yandex.ydb.table.YdbTable.CreateSessionResponse;
import static ru.yandex.ydb.table.YdbTable.CreateTableRequest;
import static ru.yandex.ydb.table.YdbTable.CreateTableResponse;
import static ru.yandex.ydb.table.YdbTable.DeleteSessionRequest;
import static ru.yandex.ydb.table.YdbTable.DeleteSessionResponse;
import static ru.yandex.ydb.table.YdbTable.DescribeTableRequest;
import static ru.yandex.ydb.table.YdbTable.DescribeTableResponse;
import static ru.yandex.ydb.table.YdbTable.DropTableRequest;
import static ru.yandex.ydb.table.YdbTable.DropTableResponse;
import static ru.yandex.ydb.table.YdbTable.ExecuteDataQueryRequest;
import static ru.yandex.ydb.table.YdbTable.ExecuteDataQueryResponse;
import static ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryRequest;
import static ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryResponse;
import static ru.yandex.ydb.table.YdbTable.ExplainDataQueryRequest;
import static ru.yandex.ydb.table.YdbTable.ExplainDataQueryResponse;
import static ru.yandex.ydb.table.YdbTable.KeepAliveRequest;
import static ru.yandex.ydb.table.YdbTable.KeepAliveResponse;
import static ru.yandex.ydb.table.YdbTable.PrepareDataQueryRequest;
import static ru.yandex.ydb.table.YdbTable.PrepareDataQueryResponse;
import static ru.yandex.ydb.table.YdbTable.RollbackTransactionRequest;
import static ru.yandex.ydb.table.YdbTable.RollbackTransactionResponse;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public final class GrpcTableRpc implements TableRpc {

    private final GrpcTransport transport;

    private GrpcTableRpc(GrpcTransport transport) {
        this.transport = transport;
    }

    @Nullable
    public static GrpcTableRpc create(RpcTransport transport) {
        if (transport instanceof GrpcTransport) {
            return new GrpcTableRpc((GrpcTransport) transport);
        }
        return null;
    }

    @Override
    public CompletableFuture<Result<CreateSessionResponse>> createSession(CreateSessionRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_CREATE_SESSION, request);
    }

    @Override
    public CompletableFuture<Result<DeleteSessionResponse>> deleteSession(DeleteSessionRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_DELETE_SESSION, request);
    }

    @Override
    public CompletableFuture<Result<KeepAliveResponse>> keepAlive(KeepAliveRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_KEEP_ALIVE, request);
    }

    @Override
    public CompletableFuture<Result<CreateTableResponse>> createTable(CreateTableRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_CREATE_TABLE, request);
    }

    @Override
    public CompletableFuture<Result<DropTableResponse>> dropTable(DropTableRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_DROP_TABLE, request);
    }

    @Override
    public CompletableFuture<Result<AlterTableResponse>> alterTable(AlterTableRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_ALTER_TABLE, request);
    }

    @Override
    public CompletableFuture<Result<CopyTableResponse>> copyTable(CopyTableRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_COPY_TABLE, request);
    }

    @Override
    public CompletableFuture<Result<DescribeTableResponse>> describeTable(DescribeTableRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_DESCRIBE_TABLE, request);
    }

    @Override
    public CompletableFuture<Result<ExplainDataQueryResponse>> explainDataQuery(ExplainDataQueryRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_EXPLAIN_DATA_QUERY, request);
    }

    @Override
    public CompletableFuture<Result<PrepareDataQueryResponse>> prepareDataQuery(PrepareDataQueryRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_PREPARE_DATA_QUERY, request);
    }

    @Override
    public CompletableFuture<Result<ExecuteDataQueryResponse>> executeDataQuery(ExecuteDataQueryRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_EXECUTE_DATA_QUERY, request);
    }

    @Override
    public CompletableFuture<Result<ExecuteSchemeQueryResponse>> executeSchemeQuery(ExecuteSchemeQueryRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_EXECUTE_SCHEME_QUERY, request);
    }

    @Override
    public CompletableFuture<Result<BeginTransactionResponse>> beginTransaction(BeginTransactionRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_BEGIN_TRANSACTION, request);
    }

    @Override
    public CompletableFuture<Result<CommitTransactionResponse>> commitTransaction(CommitTransactionRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_COMMIT_TRANSACTION, request);
    }

    @Override
    public CompletableFuture<Result<RollbackTransactionResponse>> rollbackTransaction(RollbackTransactionRequest request) {
        return transport.unaryCall(TableServiceGrpc.METHOD_ROLLBACK_TRANSACTION, request);
    }

    @Override
    public void close() {
        // nop
    }
}
