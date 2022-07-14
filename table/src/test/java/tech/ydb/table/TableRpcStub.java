package tech.ydb.table;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.protobuf.Message;
import tech.ydb.OperationProtos.Operation;
import tech.ydb.core.Issue;
import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.core.utils.Async;
import tech.ydb.table.YdbTable.AlterTableRequest;
import tech.ydb.table.YdbTable.AlterTableResponse;
import tech.ydb.table.YdbTable.BeginTransactionRequest;
import tech.ydb.table.YdbTable.BeginTransactionResponse;
import tech.ydb.table.YdbTable.CommitTransactionRequest;
import tech.ydb.table.YdbTable.CommitTransactionResponse;
import tech.ydb.table.YdbTable.CopyTableRequest;
import tech.ydb.table.YdbTable.CopyTableResponse;
import tech.ydb.table.YdbTable.CreateSessionRequest;
import tech.ydb.table.YdbTable.CreateSessionResponse;
import tech.ydb.table.YdbTable.CreateTableRequest;
import tech.ydb.table.YdbTable.CreateTableResponse;
import tech.ydb.table.YdbTable.DeleteSessionRequest;
import tech.ydb.table.YdbTable.DeleteSessionResponse;
import tech.ydb.table.YdbTable.DescribeTableRequest;
import tech.ydb.table.YdbTable.DescribeTableResponse;
import tech.ydb.table.YdbTable.DropTableRequest;
import tech.ydb.table.YdbTable.DropTableResponse;
import tech.ydb.table.YdbTable.ExecuteDataQueryRequest;
import tech.ydb.table.YdbTable.ExecuteDataQueryResponse;
import tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest;
import tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse;
import tech.ydb.table.YdbTable.ExplainDataQueryRequest;
import tech.ydb.table.YdbTable.ExplainDataQueryResponse;
import tech.ydb.table.YdbTable.KeepAliveRequest;
import tech.ydb.table.YdbTable.KeepAliveResponse;
import tech.ydb.table.YdbTable.PrepareDataQueryRequest;
import tech.ydb.table.YdbTable.PrepareDataQueryResponse;
import tech.ydb.table.YdbTable.ReadTableRequest;
import tech.ydb.table.YdbTable.ReadTableResponse;
import tech.ydb.table.YdbTable.RollbackTransactionRequest;
import tech.ydb.table.YdbTable.RollbackTransactionResponse;
import tech.ydb.table.rpc.TableRpc;


/**
 * @author Sergey Polovko
 */
public class TableRpcStub implements TableRpc {

    @Override
    public CompletableFuture<Result<CreateSessionResponse>> createSession(CreateSessionRequest request, long deadlineAfter) {
        return notImplemented("createSession()");
    }

    @Override
    public CompletableFuture<Result<DeleteSessionResponse>> deleteSession(DeleteSessionRequest request, long deadlineAfter) {
        return notImplemented("deleteSession()");
    }

    @Override
    public CompletableFuture<Result<KeepAliveResponse>> keepAlive(KeepAliveRequest request, long deadlineAfter) {
        return notImplemented("keepAlive()");
    }

    @Override
    public CompletableFuture<Result<CreateTableResponse>> createTable(CreateTableRequest request, long deadlineAfter) {
        return notImplemented("createTable()");
    }

    @Override
    public CompletableFuture<Result<DropTableResponse>> dropTable(DropTableRequest request, long deadlineAfter) {
        return notImplemented("dropTable()");
    }

    @Override
    public CompletableFuture<Result<AlterTableResponse>> alterTable(AlterTableRequest request, long deadlineAfter) {
        return notImplemented("alterTable()");
    }

    @Override
    public CompletableFuture<Result<CopyTableResponse>> copyTable(CopyTableRequest request, long deadlineAfter) {
        return notImplemented("copyTable()");
    }

    @Override
    public CompletableFuture<Result<DescribeTableResponse>> describeTable(DescribeTableRequest request, long deadlineAfter) {
        return notImplemented("describeTable()");
    }

    @Override
    public CompletableFuture<Result<ExplainDataQueryResponse>> explainDataQuery(ExplainDataQueryRequest request, long deadlineAfter) {
        return notImplemented("explainDataQuery()");
    }

    @Override
    public CompletableFuture<Result<PrepareDataQueryResponse>> prepareDataQuery(PrepareDataQueryRequest request, long deadlineAfter) {
        return notImplemented("prepareDataQuery()");
    }

    @Override
    public CompletableFuture<Result<ExecuteDataQueryResponse>> executeDataQuery(ExecuteDataQueryRequest request, long deadlineAfter) {
        return notImplemented("executeDataQuery()");
    }

    @Override
    public CompletableFuture<Result<ExecuteSchemeQueryResponse>> executeSchemeQuery(ExecuteSchemeQueryRequest request, long deadlineAfter) {
        return notImplemented("executeSchemeQuery()");
    }

    @Override
    public CompletableFuture<Result<BeginTransactionResponse>> beginTransaction(BeginTransactionRequest request, long deadlineAfter) {
        return notImplemented("beginTransaction()");
    }

    @Override
    public CompletableFuture<Result<CommitTransactionResponse>> commitTransaction(CommitTransactionRequest request, long deadlineAfter) {
        return notImplemented("commitTransaction()");
    }

    @Override
    public CompletableFuture<Result<RollbackTransactionResponse>> rollbackTransaction(RollbackTransactionRequest request, long deadlineAfter) {
        return notImplemented("rollbackTransaction()");
    }

    @Override
    public StreamControl streamReadTable(ReadTableRequest request, StreamObserver<ReadTableResponse> observer, long deadlineAfter) {
        Issue issue = Issue.of("streamReadTable() is not implemented", Issue.Severity.ERROR);
        observer.onError(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, issue));
        return () -> {};
    }

    @Override
    public StreamControl streamExecuteScanQuery(YdbTable.ExecuteScanQueryRequest request, StreamObserver<YdbTable.ExecuteScanQueryPartialResponse> observer, long deadlineAfter) {
        Issue issue = Issue.of("streamExecuteScanQuery() is not implemented", Issue.Severity.ERROR);
        observer.onError(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, issue));
        return () -> {};
    }

    @Override
    public CompletableFuture<Result<YdbTable.BulkUpsertResponse>> bulkUpsert(YdbTable.BulkUpsertRequest request, long deadlineAfter) {
        return notImplemented("bulkUpsert()");
    }

    @Override
    public String getDatabase() {
        return "";
    }

    @Override
    public OperationTray getOperationTray() {
        return new ImmediateOperationTray();
    }

    @Override
    public void close() {
        // nop
    }

    private static <U> CompletableFuture<U> notImplemented(String method) {
        return Async.failedFuture(new UnsupportedOperationException(method + " not implemented"));
    }

    /**
     * IMMEDIATE OPERATION TRAY
     */
    private static final class ImmediateOperationTray implements OperationTray {
        @Override
        public CompletableFuture<Status> waitStatus(Operation operation, long deadlineAfter) {
            return CompletableFuture.completedFuture(Operations.status(operation));
        }

        @Override
        public <M extends Message, R> CompletableFuture<Result<R>> waitResult(
            Operation operation, Class<M> resultClass, Function<M, R> mapper, long deadlineAfter)
        {
            Status status = Operations.status(operation);
            if (status.isSuccess()) {
                M resultMessage = Operations.unpackResult(operation, resultClass);
                return CompletableFuture.completedFuture(Result.success(mapper.apply(resultMessage), status.getIssues()));
            }
            return CompletableFuture.completedFuture(Result.fail(status));
        }

        @Override
        public void close() {
            // nop
        }
    }
}
