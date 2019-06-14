package tech.ydb.table;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.query.DataQuery;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.ExplainDataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.BeginTxSettings;
import tech.ydb.table.settings.CloseSessionSettings;
import tech.ydb.table.settings.CopyTableSettings;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.settings.DescribeTableSettings;
import tech.ydb.table.settings.DropTableSettings;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.settings.ExecuteSchemeQuerySettings;
import tech.ydb.table.settings.ExplainDataQuerySettings;
import tech.ydb.table.settings.KeepAliveSessionSettings;
import tech.ydb.table.settings.PrepareDataQuerySettings;
import tech.ydb.table.settings.ReadTableSettings;
import tech.ydb.table.transaction.Transaction;
import tech.ydb.table.transaction.TransactionMode;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.utils.Async;


/**
 * @author Sergey Polovko
 */
public class SessionStub implements Session {

    private final String id;

    public SessionStub(String id) {
        this.id = id;
    }

    public SessionStub() {
        this(UUID.randomUUID().toString());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public CompletableFuture<Status> createTable(
        String path, TableDescription tableDescriptions, CreateTableSettings settings)
    {
        return notImplemented("createTable()");
    }

    @Override
    public CompletableFuture<Status> dropTable(String path, DropTableSettings settings) {
        return notImplemented("dropTable()");
    }

    @Override
    public CompletableFuture<Status> alterTable(String path, AlterTableSettings settings) {
        return notImplemented("alterTable()");
    }

    @Override
    public CompletableFuture<Status> copyTable(String src, String dst, CopyTableSettings settings) {
        return notImplemented("copyTable()");
    }

    @Override
    public CompletableFuture<Result<TableDescription>> describeTable(String path, DescribeTableSettings settings) {
        return notImplemented("describeTable()");
    }

    @Override
    public CompletableFuture<Result<DataQueryResult>> executeDataQuery(
        String query, TxControl txControl, Params params, ExecuteDataQuerySettings settings)
    {
        return notImplemented("executeDataQuery()");
    }

    @Override
    public CompletableFuture<Result<DataQuery>> prepareDataQuery(String query, PrepareDataQuerySettings settings) {
        return notImplemented("prepareDataQuery()");
    }

    @Override
    public CompletableFuture<Status> executeSchemeQuery(String query, ExecuteSchemeQuerySettings settings) {
        return notImplemented("executeSchemeQuery()");
    }

    @Override
    public CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(
        String query, ExplainDataQuerySettings settings)
    {
        return notImplemented("explainDataQuery()");
    }

    @Override
    public CompletableFuture<Result<Transaction>> beginTransaction(
        TransactionMode transactionMode, BeginTxSettings settings)
    {
        return notImplemented("beginTransaction()");
    }

    @Override
    public CompletableFuture<Status> readTable(String tablePath, ReadTableSettings settings, Consumer<ResultSetReader> fn) {
        return notImplemented("readTable()");
    }

    @Override
    public CompletableFuture<Result<SessionStatus>> keepAlive(KeepAliveSessionSettings settings) {
        return notImplemented("keepAlive()");
    }

    @Override
    public void invalidateQueryCache() {
        throw new UnsupportedOperationException("invalidateQueryCache() not implemented");
    }

    @Override
    public boolean release() {
        return false;
    }

    @Override
    public CompletableFuture<Status> close(CloseSessionSettings settings) {
        return notImplemented("close()");
    }

    private static <U> CompletableFuture<U> notImplemented(String method) {
        return Async.failedFuture(new UnsupportedOperationException(method + " not implemented"));
    }
}
