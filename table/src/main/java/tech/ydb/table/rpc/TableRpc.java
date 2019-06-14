package tech.ydb.table.rpc;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.rpc.Rpc;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.table.YdbTable.ReadTableRequest;
import tech.ydb.table.YdbTable.ReadTableResponse;

import static tech.ydb.table.YdbTable.AlterTableRequest;
import static tech.ydb.table.YdbTable.AlterTableResponse;
import static tech.ydb.table.YdbTable.BeginTransactionRequest;
import static tech.ydb.table.YdbTable.BeginTransactionResponse;
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
     */
    CompletableFuture<Result<CreateSessionResponse>> createSession(CreateSessionRequest request);

    /**
     * Ends a session, releasing server resources associated with it.
     */
    CompletableFuture<Result<DeleteSessionResponse>> deleteSession(DeleteSessionRequest request);

    /**
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     */
    CompletableFuture<Result<KeepAliveResponse>> keepAlive(KeepAliveRequest request);

    /**
     * Creates new table.
     */
    CompletableFuture<Result<CreateTableResponse>> createTable(CreateTableRequest request);

    /**
     * Drop table.
     */
    CompletableFuture<Result<DropTableResponse>> dropTable(DropTableRequest request);

    /**
     * Modifies schema of given table.
     */
    CompletableFuture<Result<AlterTableResponse>> alterTable(AlterTableRequest request);

    /**
     * Creates copy of given table.
     */
    CompletableFuture<Result<CopyTableResponse>> copyTable(CopyTableRequest request);

    /**
     * Returns information about given table (metadata).
     */
    CompletableFuture<Result<DescribeTableResponse>> describeTable(DescribeTableRequest request);

    /**
     * Explains data query.
     * SessionId of previously created session must be provided.
     */
    CompletableFuture<Result<ExplainDataQueryResponse>> explainDataQuery(ExplainDataQueryRequest request);

    /**
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     */
    CompletableFuture<Result<PrepareDataQueryResponse>> prepareDataQuery(PrepareDataQueryRequest request);

    /**
     * Executes data query.
     * SessionId of previously created session must be provided.
     */
    CompletableFuture<Result<ExecuteDataQueryResponse>> executeDataQuery(ExecuteDataQueryRequest request);

    /**
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     */
    CompletableFuture<Result<ExecuteSchemeQueryResponse>> executeSchemeQuery(ExecuteSchemeQueryRequest request);

    /**
     * Begins new transaction.
     */
    CompletableFuture<Result<BeginTransactionResponse>> beginTransaction(BeginTransactionRequest request);

    /**
     * Commits specified active transaction.
     */
    CompletableFuture<Result<CommitTransactionResponse>> commitTransaction(CommitTransactionRequest request);

    /**
     * Performs a rollback of the specified active transaction.
     */
    CompletableFuture<Result<RollbackTransactionResponse>> rollbackTransaction(RollbackTransactionRequest request);

    /**
     * Streaming read table.
     */
    void streamReadTable(ReadTableRequest request, StreamObserver<ReadTableResponse> observer);
}
