package tech.ydb.table.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.proto.table.YdbTable.AlterTableRequest;
import tech.ydb.proto.table.YdbTable.BeginTransactionRequest;
import tech.ydb.proto.table.YdbTable.BeginTransactionResult;
import tech.ydb.proto.table.YdbTable.BulkUpsertRequest;
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
import tech.ydb.proto.table.YdbTable.ExecuteScanQueryPartialResponse;
import tech.ydb.proto.table.YdbTable.ExecuteScanQueryRequest;
import tech.ydb.proto.table.YdbTable.ExecuteSchemeQueryRequest;
import tech.ydb.proto.table.YdbTable.ExplainDataQueryRequest;
import tech.ydb.proto.table.YdbTable.ExplainQueryResult;
import tech.ydb.proto.table.YdbTable.KeepAliveRequest;
import tech.ydb.proto.table.YdbTable.KeepAliveResult;
import tech.ydb.proto.table.YdbTable.PrepareDataQueryRequest;
import tech.ydb.proto.table.YdbTable.PrepareQueryResult;
import tech.ydb.proto.table.YdbTable.ReadRowsRequest;
import tech.ydb.proto.table.YdbTable.ReadRowsResponse;
import tech.ydb.proto.table.YdbTable.ReadTableRequest;
import tech.ydb.proto.table.YdbTable.ReadTableResponse;
import tech.ydb.proto.table.YdbTable.RenameTablesRequest;
import tech.ydb.proto.table.YdbTable.RollbackTransactionRequest;


/**
 * @author Sergey Polovko
 */
public interface TableRpc extends AutoCloseable {

    String getDatabase();

    ScheduledExecutorService getScheduler();

    @Override
    void close();


    /**
     * Create new session. Implicit session creation is forbidden, so user must create new session
     * before execute any query, otherwise BAD_SESSION status wil be returned. Simultaneous execution
     * of requests are forbidden. Sessions are volatile, can be invalidated by server, e.g. in case
     * of fatal errors. All requests with this session will fail with BAD_SESSION status.
     * So, client must be able to handle BAD_SESSION status.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with id of new session
     */
    CompletableFuture<Result<CreateSessionResult>> createSession(CreateSessionRequest request,
                                                                   GrpcRequestSettings settings);

    /**
     * Ends a session, releasing server resources associated with it.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> deleteSession(DeleteSessionRequest request, GrpcRequestSettings settings);

    /**
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<KeepAliveResult>> keepAlive(KeepAliveRequest request, GrpcRequestSettings settings);

    /**
     * Creates new table.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> createTable(CreateTableRequest request, GrpcRequestSettings settings);

    /**
     * Drop table.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> dropTable(DropTableRequest request, GrpcRequestSettings settings);

    /**
     * Modifies schema of given table.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> alterTable(AlterTableRequest request, GrpcRequestSettings settings);

    /**
     * Creates copy of given table.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> copyTable(CopyTableRequest request, GrpcRequestSettings settings);

    /**
     * Creates consistent copies of the given tables.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> copyTables(CopyTablesRequest request, GrpcRequestSettings settings);

    /**
     * Renames the given tables, possibly replacing the existing destination tables.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> renameTables(RenameTablesRequest request, GrpcRequestSettings settings);

    /**
     * Returns information about given table (metadata).
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<DescribeTableResult>> describeTable(DescribeTableRequest request,
            GrpcRequestSettings settings);

    /**
     * Explains data query.
     * SessionId of previously created session must be provided.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<ExplainQueryResult>> explainDataQuery(ExplainDataQueryRequest request,
            GrpcRequestSettings settings);

    /**
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<PrepareQueryResult>> prepareDataQuery(PrepareDataQueryRequest request,
            GrpcRequestSettings settings);

    /**
     * Executes data query.
     * SessionId of previously created session must be provided.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<ExecuteQueryResult>> executeDataQuery(ExecuteDataQueryRequest request,
            GrpcRequestSettings settings);

    /**
     * Read rows in the key-value form.
     * SessionId of previously created session must be provided.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result set
     */
    CompletableFuture<Result<ReadRowsResponse>> readRows(ReadRowsRequest request,
            GrpcRequestSettings settings);

    /**
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> executeSchemeQuery(ExecuteSchemeQueryRequest request,
            GrpcRequestSettings settings);

    /**
     * Begins new transaction.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<BeginTransactionResult>> beginTransaction(BeginTransactionRequest request,
            GrpcRequestSettings settings);

    /**
     * Commits specified active transaction.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> commitTransaction(CommitTransactionRequest request,
            GrpcRequestSettings settings);

    /**
     * Performs a rollback of the specified active transaction.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> rollbackTransaction(RollbackTransactionRequest request,
            GrpcRequestSettings settings);

    /**
     * Streaming read table.
     * @param request request proto
     * @param settings rpc call settings
     * @return GrpcReadStream object that allows to start and cancel the stream
     */
    GrpcReadStream<ReadTableResponse> streamReadTable(ReadTableRequest request, GrpcRequestSettings settings);

    /**
     * Streaming execute scan query.
     * @param request request proto
     * @param settings rpc call settings
     * @return GrpcReadStream object that allows to start and  cancel the stream
     */
    GrpcReadStream<ExecuteScanQueryPartialResponse> streamExecuteScanQuery(ExecuteScanQueryRequest request,
            GrpcRequestSettings settings);

    /**
     * Execute bulk upsert
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> bulkUpsert(BulkUpsertRequest request, GrpcRequestSettings settings);

    /**
     * Describe table request.
     * @param request request proto
     * @param settings rpc call settings
     * @return GrpcReadStream object that allows to start and cancel the stream
     */
    CompletableFuture<Result<YdbTable.DescribeTableOptionsResult>> describeTableOptions(YdbTable.DescribeTableOptionsRequest request,
                                                                         GrpcRequestSettings settings);
}

