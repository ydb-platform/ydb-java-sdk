package tech.ydb.table;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.impl.call.EmptyStream;
import tech.ydb.core.utils.Async;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.proto.table.YdbTable.AlterTableRequest;
import tech.ydb.proto.table.YdbTable.BeginTransactionRequest;
import tech.ydb.proto.table.YdbTable.BeginTransactionResult;
import tech.ydb.proto.table.YdbTable.CommitTransactionRequest;
import tech.ydb.proto.table.YdbTable.CopyTableRequest;
import tech.ydb.proto.table.YdbTable.CopyTablesRequest;
import tech.ydb.proto.table.YdbTable.CreateSessionRequest;
import tech.ydb.proto.table.YdbTable.CreateSessionResult;
import tech.ydb.proto.table.YdbTable.CreateTableRequest;
import tech.ydb.proto.table.YdbTable.DeleteSessionRequest;
import tech.ydb.proto.table.YdbTable.DescribeTableRequest;
import tech.ydb.proto.table.YdbTable.DescribeTableResult;
import tech.ydb.proto.table.YdbTable.DropTableRequest;
import tech.ydb.proto.table.YdbTable.ExecuteDataQueryRequest;
import tech.ydb.proto.table.YdbTable.ExecuteQueryResult;
import tech.ydb.proto.table.YdbTable.ExecuteSchemeQueryRequest;
import tech.ydb.proto.table.YdbTable.ExplainDataQueryRequest;
import tech.ydb.proto.table.YdbTable.ExplainQueryResult;
import tech.ydb.proto.table.YdbTable.KeepAliveRequest;
import tech.ydb.proto.table.YdbTable.KeepAliveResult;
import tech.ydb.proto.table.YdbTable.PrepareDataQueryRequest;
import tech.ydb.proto.table.YdbTable.PrepareQueryResult;
import tech.ydb.proto.table.YdbTable.RollbackTransactionRequest;
import tech.ydb.table.rpc.TableRpc;


/**
 * @author Sergey Polovko
 */
public class TableRpcStub implements TableRpc {
    private final ScheduledExecutorService scheduler;

    public TableRpcStub(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<Result<CreateSessionResult>> createSession(CreateSessionRequest request,
                                                                          GrpcRequestSettings settings) {
        return notImplemented("createSession()");
    }

    @Override
    public CompletableFuture<Status> deleteSession(DeleteSessionRequest request,
                                                                          GrpcRequestSettings settings) {
        return notImplemented("deleteSession()");
    }

    @Override
    public CompletableFuture<Result<KeepAliveResult>> keepAlive(KeepAliveRequest request,
                                                                  GrpcRequestSettings settings) {
        return notImplemented("keepAlive()");
    }

    @Override
    public CompletableFuture<Status> createTable(CreateTableRequest request,
                                                                      GrpcRequestSettings settings) {
        return notImplemented("createTable()");
    }

    @Override
    public CompletableFuture<Status> dropTable(DropTableRequest request,
                                                                  GrpcRequestSettings settings) {
        return notImplemented("dropTable()");
    }

    @Override
    public CompletableFuture<Status> alterTable(AlterTableRequest request,
                                                                    GrpcRequestSettings settings) {
        return notImplemented("alterTable()");
    }

    @Override
    public CompletableFuture<Status> copyTable(CopyTableRequest request,
                                                                  GrpcRequestSettings settings) {
        return notImplemented("copyTable()");
    }

    @Override
    public CompletableFuture<Status> copyTables(CopyTablesRequest request,
                                                                  GrpcRequestSettings settings) {
        return notImplemented("copyTables()");
    }

    @Override
    public CompletableFuture<Result<DescribeTableResult>> describeTable(DescribeTableRequest request,
                                                                          GrpcRequestSettings settings) {
        return notImplemented("describeTable()");
    }

    @Override
    public CompletableFuture<Result<ExplainQueryResult>> explainDataQuery(ExplainDataQueryRequest request,
                                                                                GrpcRequestSettings settings) {
        return notImplemented("explainDataQuery()");
    }

    @Override
    public CompletableFuture<Result<PrepareQueryResult>> prepareDataQuery(PrepareDataQueryRequest request,
                                                                                GrpcRequestSettings settings) {
        return notImplemented("prepareDataQuery()");
    }

    @Override
    public CompletableFuture<Result<ExecuteQueryResult>> executeDataQuery(ExecuteDataQueryRequest request,
                                                                                GrpcRequestSettings settings) {
        return notImplemented("executeDataQuery()");
    }

    @Override
    public CompletableFuture<Status> executeSchemeQuery(ExecuteSchemeQueryRequest request,
                                                                                    GrpcRequestSettings settings) {
        return notImplemented("executeSchemeQuery()");
    }

    @Override
    public CompletableFuture<Result<BeginTransactionResult>> beginTransaction(BeginTransactionRequest request,
                                                                                GrpcRequestSettings settings) {
        return notImplemented("beginTransaction()");
    }

    @Override
    public CompletableFuture<Status> commitTransaction(CommitTransactionRequest request,
                                                                                  GrpcRequestSettings settings) {
        return notImplemented("commitTransaction()");
    }

    @Override
    public CompletableFuture<Status> rollbackTransaction(
            RollbackTransactionRequest request, GrpcRequestSettings settings) {
        return notImplemented("rollbackTransaction()");
    }

    @Override
    public GrpcReadStream<YdbTable.ReadTableResponse> streamReadTable(
            YdbTable.ReadTableRequest request, GrpcRequestSettings settings) {
        Issue issue = Issue.of("streamReadTable() is not implemented", Issue.Severity.ERROR);
        Status status = Status.of(StatusCode.CLIENT_INTERNAL_ERROR).withIssues(issue);
        return new EmptyStream<>(status);
    }

    @Override
    public GrpcReadStream<YdbTable.ExecuteScanQueryPartialResponse> streamExecuteScanQuery(
            YdbTable.ExecuteScanQueryRequest request, GrpcRequestSettings settings) {
        Issue issue = Issue.of("streamExecuteScanQuery() is not implemented", Issue.Severity.ERROR);
        Status status = Status.of(StatusCode.CLIENT_INTERNAL_ERROR).withIssues(issue);
        return new EmptyStream<>(status);
    }

    @Override
    public CompletableFuture<Status> bulkUpsert(YdbTable.BulkUpsertRequest request,
            GrpcRequestSettings settings) {
        return notImplemented("bulkUpsert()");
    }

    @Override
    public String getDatabase() {
        return "";
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public void close() {
        // nop
    }

    private static <U> CompletableFuture<U> notImplemented(String method) {
        return Async.failedFuture(new UnsupportedOperationException(method + " not implemented"));
    }
}
