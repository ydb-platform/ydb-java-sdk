package tech.ydb.table;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.protobuf.Message;
import tech.ydb.OperationProtos.Operation;
import tech.ydb.core.Issue;
import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;
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
import tech.ydb.table.utils.Async;


/**
 * @author Sergey Polovko
 */
public class TableRpcStub implements TableRpc {

    @Override
    public CompletableFuture<Result<CreateSessionResponse>> createSession(CreateSessionRequest request,
                                                                          GrpcRequestSettings settings) {
        return notImplemented("createSession()");
    }

    @Override
    public CompletableFuture<Result<DeleteSessionResponse>> deleteSession(DeleteSessionRequest request,
                                                                          GrpcRequestSettings settings) {
        return notImplemented("deleteSession()");
    }

    @Override
    public CompletableFuture<Result<KeepAliveResponse>> keepAlive(KeepAliveRequest request,
                                                                  GrpcRequestSettings settings) {
        return notImplemented("keepAlive()");
    }

    @Override
    public CompletableFuture<Result<CreateTableResponse>> createTable(CreateTableRequest request,
                                                                      GrpcRequestSettings settings) {
        return notImplemented("createTable()");
    }

    @Override
    public CompletableFuture<Result<DropTableResponse>> dropTable(DropTableRequest request,
                                                                  GrpcRequestSettings settings) {
        return notImplemented("dropTable()");
    }

    @Override
    public CompletableFuture<Result<AlterTableResponse>> alterTable(AlterTableRequest request,
                                                                    GrpcRequestSettings settings) {
        return notImplemented("alterTable()");
    }

    @Override
    public CompletableFuture<Result<CopyTableResponse>> copyTable(CopyTableRequest request,
                                                                  GrpcRequestSettings settings) {
        return notImplemented("copyTable()");
    }

    @Override
    public CompletableFuture<Result<DescribeTableResponse>> describeTable(DescribeTableRequest request,
                                                                          GrpcRequestSettings settings) {
        return notImplemented("describeTable()");
    }

    @Override
    public CompletableFuture<Result<ExplainDataQueryResponse>> explainDataQuery(ExplainDataQueryRequest request,
                                                                                GrpcRequestSettings settings) {
        return notImplemented("explainDataQuery()");
    }

    @Override
    public CompletableFuture<Result<PrepareDataQueryResponse>> prepareDataQuery(PrepareDataQueryRequest request,
                                                                                GrpcRequestSettings settings) {
        return notImplemented("prepareDataQuery()");
    }

    @Override
    public CompletableFuture<Result<ExecuteDataQueryResponse>> executeDataQuery(ExecuteDataQueryRequest request,
                                                                                GrpcRequestSettings settings) {
        return notImplemented("executeDataQuery()");
    }

    @Override
    public CompletableFuture<Result<ExecuteSchemeQueryResponse>> executeSchemeQuery(ExecuteSchemeQueryRequest request,
                                                                                    GrpcRequestSettings settings) {
        return notImplemented("executeSchemeQuery()");
    }

    @Override
    public CompletableFuture<Result<BeginTransactionResponse>> beginTransaction(BeginTransactionRequest request,
                                                                                GrpcRequestSettings settings) {
        return notImplemented("beginTransaction()");
    }

    @Override
    public CompletableFuture<Result<CommitTransactionResponse>> commitTransaction(CommitTransactionRequest request,
                                                                                  GrpcRequestSettings settings) {
        return notImplemented("commitTransaction()");
    }

    @Override
    public CompletableFuture<Result<RollbackTransactionResponse>> rollbackTransaction(
            RollbackTransactionRequest request, GrpcRequestSettings settings) {
        return notImplemented("rollbackTransaction()");
    }

    @Override
    public StreamControl streamReadTable(ReadTableRequest request, StreamObserver<ReadTableResponse> observer,
                                         GrpcRequestSettings settings) {
        Issue issue = Issue.of("streamReadTable() is not implemented", Issue.Severity.ERROR);
        observer.onError(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, issue));
        return () -> {};
    }

    @Override
    public StreamControl streamExecuteScanQuery(YdbTable.ExecuteScanQueryRequest request,
            StreamObserver<YdbTable.ExecuteScanQueryPartialResponse> observer, GrpcRequestSettings settings) {
        Issue issue = Issue.of("streamExecuteScanQuery() is not implemented", Issue.Severity.ERROR);
        observer.onError(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, issue));
        return () -> {};
    }

    @Override
    public CompletableFuture<Result<YdbTable.BulkUpsertResponse>> bulkUpsert(YdbTable.BulkUpsertRequest request,
                                                                             GrpcRequestSettings settings) {
        return notImplemented("bulkUpsert()");
    }

    @Override
    public String getDatabase() {
        return "";
    }

    @Override
    @Nullable
    public String getEndpointByNodeId(int nodeId) {
        return null;
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
        public CompletableFuture<Status> waitStatus(Operation operation, GrpcRequestSettings settings) {
            return CompletableFuture.completedFuture(Operations.status(operation));
        }

        @Override
        public <M extends Message, R> CompletableFuture<Result<R>> waitResult(
            Operation operation, Class<M> resultClass, Function<M, R> mapper, GrpcRequestSettings settings)
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
