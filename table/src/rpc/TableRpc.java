package ru.yandex.ydb.table.rpc;

import java.util.concurrent.CompletableFuture;

import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.rpc.Rpc;

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

}
