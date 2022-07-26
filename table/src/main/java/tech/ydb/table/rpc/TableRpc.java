package tech.ydb.table.rpc;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.rpc.Rpc;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse;
import tech.ydb.table.YdbTable.ExecuteScanQueryRequest;
import tech.ydb.table.YdbTable.ReadTableRequest;
import tech.ydb.table.YdbTable.ReadTableResponse;

import static tech.ydb.table.YdbTable.AlterTableRequest;
import static tech.ydb.table.YdbTable.AlterTableResponse;
import static tech.ydb.table.YdbTable.BeginTransactionRequest;
import static tech.ydb.table.YdbTable.BeginTransactionResponse;
import static tech.ydb.table.YdbTable.BulkUpsertRequest;
import static tech.ydb.table.YdbTable.BulkUpsertResponse;
import static tech.ydb.table.YdbTable.CommitTransactionRequest;
import static tech.ydb.table.YdbTable.CommitTransactionResponse;
import static tech.ydb.table.YdbTable.CopyTableRequest;
import static tech.ydb.table.YdbTable.CopyTableResponse;
import static tech.ydb.table.YdbTable.CreateSessionRequest;
import static tech.ydb.table.YdbTable.CreateSessionResponse;
import static tech.ydb.table.YdbTable.CreateTableRequest;
import static tech.ydb.table.YdbTable.CreateTableResponse;
import static tech.ydb.table.YdbTable.DeleteSessionRequest;
import static tech.ydb.table.YdbTable.DeleteSessionResponse;
import static tech.ydb.table.YdbTable.DescribeTableRequest;
import static tech.ydb.table.YdbTable.DescribeTableResponse;
import static tech.ydb.table.YdbTable.DropTableRequest;
import static tech.ydb.table.YdbTable.DropTableResponse;
import static tech.ydb.table.YdbTable.ExecuteDataQueryRequest;
import static tech.ydb.table.YdbTable.ExecuteDataQueryResponse;
import static tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest;
import static tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse;
import static tech.ydb.table.YdbTable.ExplainDataQueryRequest;
import static tech.ydb.table.YdbTable.ExplainDataQueryResponse;
import static tech.ydb.table.YdbTable.KeepAliveRequest;
import static tech.ydb.table.YdbTable.KeepAliveResponse;
import static tech.ydb.table.YdbTable.PrepareDataQueryRequest;
import static tech.ydb.table.YdbTable.PrepareDataQueryResponse;
import static tech.ydb.table.YdbTable.RollbackTransactionRequest;
import static tech.ydb.table.YdbTable.RollbackTransactionResponse;


/**
 * @author Sergey Polovko
 */
public interface TableRpc extends Rpc {

    /**
     * Create new session. Implicit session creation is forbidden, so user must create new session
     * before execute any query, otherwise BAD_SESSION status wil be returned. Simultaneous execution
     * of requests are forbidden. Sessions are volatile, can be invalidated by server, e.g. in case
     * of fatal errors. All requests with this session will fail with BAD_SESSION status.
     * So, client must be able to handle BAD_SESSION status.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<CreateSessionResponse>> createSession(CreateSessionRequest request,
                                                                   GrpcRequestSettings settings);

    /**
     * Ends a session, releasing server resources associated with it.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<DeleteSessionResponse>> deleteSession(DeleteSessionRequest request,
                                                                   GrpcRequestSettings settings);

    /**
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<KeepAliveResponse>> keepAlive(KeepAliveRequest request, GrpcRequestSettings settings);

    /**
     * Creates new table.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<CreateTableResponse>> createTable(CreateTableRequest request,
                                                               GrpcRequestSettings settings);

    /**
     * Drop table.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<DropTableResponse>> dropTable(DropTableRequest request, GrpcRequestSettings settings);

    /**
     * Modifies schema of given table.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<AlterTableResponse>> alterTable(AlterTableRequest request, GrpcRequestSettings settings);

    /**
     * Creates copy of given table.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<CopyTableResponse>> copyTable(CopyTableRequest request, GrpcRequestSettings settings);

    /**
     * Returns information about given table (metadata).
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<DescribeTableResponse>> describeTable(DescribeTableRequest request,
                                                                   GrpcRequestSettings settings);

    /**
     * Explains data query.
     * SessionId of previously created session must be provided.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<ExplainDataQueryResponse>> explainDataQuery(ExplainDataQueryRequest request,
                                                                         GrpcRequestSettings settings);

    /**
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<PrepareDataQueryResponse>> prepareDataQuery(PrepareDataQueryRequest request,
                                                                         GrpcRequestSettings settings);

    /**
     * Executes data query.
     * SessionId of previously created session must be provided.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<ExecuteDataQueryResponse>> executeDataQuery(ExecuteDataQueryRequest request,
                                                                         GrpcRequestSettings settings);

    /**
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<ExecuteSchemeQueryResponse>> executeSchemeQuery(ExecuteSchemeQueryRequest request,
                                                                             GrpcRequestSettings settings);

    /**
     * Begins new transaction.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<BeginTransactionResponse>> beginTransaction(BeginTransactionRequest request,
                                                                         GrpcRequestSettings settings);

    /**
     * Commits specified active transaction.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<CommitTransactionResponse>> commitTransaction(CommitTransactionRequest request,
                                                                           GrpcRequestSettings settings);

    /**
     * Performs a rollback of the specified active transaction.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<RollbackTransactionResponse>> rollbackTransaction(RollbackTransactionRequest request,
                                                                               GrpcRequestSettings settings);

    /**
     * Streaming read table.
     * @param request request proto
     * @param observer consumer of streaming data
     * @param settings rpc call settings
     * @return StreamControl object that allows to cancel the stream
     */
    StreamControl streamReadTable(ReadTableRequest request, StreamObserver<ReadTableResponse> observer,
                                  GrpcRequestSettings settings);

    /**
     * Streaming execute scan query.
     * @param request request proto
     * @param observer consumer of streaming data
     * @param settings rpc call settings
     * @return StreamControl object that allows to cancel the stream
     */
    StreamControl streamExecuteScanQuery(ExecuteScanQueryRequest request,
                                         StreamObserver<ExecuteScanQueryPartialResponse> observer,
                                         GrpcRequestSettings settings);

    /**
     * Execute bulk upsert
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<BulkUpsertResponse>> bulkUpsert(BulkUpsertRequest request, GrpcRequestSettings settings);
}

