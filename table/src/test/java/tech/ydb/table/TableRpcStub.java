package tech.ydb.table;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.EndpointInfo;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.core.utils.Async;
import tech.ydb.table.rpc.TableRpc;

import static tech.ydb.table.YdbTable.AlterTableRequest;
import static tech.ydb.table.YdbTable.BeginTransactionRequest;
import static tech.ydb.table.YdbTable.BeginTransactionResult;
import static tech.ydb.table.YdbTable.CommitTransactionRequest;
import static tech.ydb.table.YdbTable.CopyTableRequest;
import static tech.ydb.table.YdbTable.CreateSessionRequest;
import static tech.ydb.table.YdbTable.CreateSessionResult;
import static tech.ydb.table.YdbTable.CreateTableRequest;
import static tech.ydb.table.YdbTable.DeleteSessionRequest;
import static tech.ydb.table.YdbTable.DescribeTableRequest;
import static tech.ydb.table.YdbTable.DescribeTableResult;
import static tech.ydb.table.YdbTable.DropTableRequest;
import static tech.ydb.table.YdbTable.ExecuteDataQueryRequest;
import static tech.ydb.table.YdbTable.ExecuteQueryResult;
import static tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest;
import static tech.ydb.table.YdbTable.ExplainDataQueryRequest;
import static tech.ydb.table.YdbTable.ExplainQueryResult;
import static tech.ydb.table.YdbTable.KeepAliveRequest;
import static tech.ydb.table.YdbTable.KeepAliveResult;
import static tech.ydb.table.YdbTable.PrepareDataQueryRequest;
import static tech.ydb.table.YdbTable.PrepareQueryResult;
import static tech.ydb.table.YdbTable.ReadTableRequest;
import static tech.ydb.table.YdbTable.ReadTableResponse;
import static tech.ydb.table.YdbTable.RollbackTransactionRequest;


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
    public StreamControl streamReadTable(ReadTableRequest request, StreamObserver<ReadTableResponse> observer,
                                         GrpcRequestSettings settings) {
        Issue issue = Issue.of("streamReadTable() is not implemented", Issue.Severity.ERROR);
        observer.onError(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue));
        return () -> {};
    }

    @Override
    public StreamControl streamExecuteScanQuery(YdbTable.ExecuteScanQueryRequest request,
            StreamObserver<YdbTable.ExecuteScanQueryPartialResponse> observer, GrpcRequestSettings settings) {
        Issue issue = Issue.of("streamExecuteScanQuery() is not implemented", Issue.Severity.ERROR);
        observer.onError(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue));
        return () -> {};
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
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    @Override
    public void close() {
        // nop
    }

    @Override
    public EndpointInfo getEndpointBySessionId(String sessionId) {
        return null;
    }

    private static <U> CompletableFuture<U> notImplemented(String method) {
        return Async.failedFuture(new UnsupportedOperationException(method + " not implemented"));
    }
}
