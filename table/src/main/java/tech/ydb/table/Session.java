package tech.ydb.table;

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
import tech.ydb.table.settings.BulkUpsertSettings;
import tech.ydb.table.settings.CloseSessionSettings;
import tech.ydb.table.settings.CommitTxSettings;
import tech.ydb.table.settings.CopyTableSettings;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.settings.DescribeTableSettings;
import tech.ydb.table.settings.DropTableSettings;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.settings.ExecuteScanQuerySettings;
import tech.ydb.table.settings.ExecuteSchemeQuerySettings;
import tech.ydb.table.settings.ExplainDataQuerySettings;
import tech.ydb.table.settings.KeepAliveSessionSettings;
import tech.ydb.table.settings.PrepareDataQuerySettings;
import tech.ydb.table.settings.ReadTableSettings;
import tech.ydb.table.settings.RollbackTxSettings;
import tech.ydb.table.transaction.Transaction;
import tech.ydb.table.transaction.TransactionMode;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.values.ListValue;


/**
 * @author Sergey Polovko
 */
public interface Session {

    String getId();

    CompletableFuture<Status> createTable(String path, TableDescription tableDescriptions, CreateTableSettings settings);

    default CompletableFuture<Status> createTable(String path, TableDescription tableDescriptions) {
        return createTable(path, tableDescriptions, new CreateTableSettings());
    }

    CompletableFuture<Status> dropTable(String path, DropTableSettings settings);

    default CompletableFuture<Status> dropTable(String path) {
        return dropTable(path, new DropTableSettings());
    }

    CompletableFuture<Status> alterTable(String path, AlterTableSettings settings);

    default CompletableFuture<Status> alterTable(String path) {
        return alterTable(path, new AlterTableSettings());
    }

    CompletableFuture<Status> copyTable(String src, String dst, CopyTableSettings settings);

    default CompletableFuture<Status> copyTable(String src, String dst) {
        return copyTable(src, dst, new CopyTableSettings());
    }

    CompletableFuture<Result<TableDescription>> describeTable(String path, DescribeTableSettings settings);

    default CompletableFuture<Result<TableDescription>> describeTable(String path) {
        return describeTable(path, new DescribeTableSettings());
    }

    CompletableFuture<Result<DataQueryResult>> executeDataQuery(
        String query,
        TxControl txControl,
        Params params,
        ExecuteDataQuerySettings settings);

    default CompletableFuture<Result<DataQueryResult>> executeDataQuery(String query, TxControl txControl, Params params) {
        return executeDataQuery(query, txControl, params, new ExecuteDataQuerySettings());
    }

    default CompletableFuture<Result<DataQueryResult>> executeDataQuery(String query, TxControl txControl) {
        return executeDataQuery(query, txControl, Params.empty(), new ExecuteDataQuerySettings());
    }

    CompletableFuture<Result<DataQuery>> prepareDataQuery(String query, PrepareDataQuerySettings settings);

    default CompletableFuture<Result<DataQuery>> prepareDataQuery(String query) {
        return prepareDataQuery(query, new PrepareDataQuerySettings());
    }

    CompletableFuture<Status> executeSchemeQuery(String query, ExecuteSchemeQuerySettings settings);

    default CompletableFuture<Status> executeSchemeQuery(String query) {
        return executeSchemeQuery(query, new ExecuteSchemeQuerySettings());
    }

    CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(String query, ExplainDataQuerySettings settings);

    default CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(String query) {
        return explainDataQuery(query, new ExplainDataQuerySettings());
    }

    CompletableFuture<Result<Transaction>> beginTransaction(TransactionMode transactionMode, BeginTxSettings settings);

    default CompletableFuture<Result<Transaction>> beginTransaction(TransactionMode transactionMode) {
        return beginTransaction(transactionMode, new BeginTxSettings());
    }

    CompletableFuture<Status> commitTransaction(String txId, CommitTxSettings settings);

    CompletableFuture<Status> rollbackTransaction(String txId, RollbackTxSettings settings);

    CompletableFuture<Status> readTable(String tablePath, ReadTableSettings settings, Consumer<ResultSetReader> fn);

    CompletableFuture<Status> executeScanQuery(String query, Params params, ExecuteScanQuerySettings settings, Consumer<ResultSetReader> fn);

    CompletableFuture<Result<SessionStatus>> keepAlive(KeepAliveSessionSettings settings);

    CompletableFuture<Status> executeBulkUpsert(String tablePath, ListValue rows, BulkUpsertSettings settings);

    default CompletableFuture<Result<SessionStatus>> keepAlive() {
        return keepAlive(new KeepAliveSessionSettings());
    }

    void invalidateQueryCache();

    boolean release();

    CompletableFuture<Status> close(CloseSessionSettings settings);

    default CompletableFuture<Status> close() {
        return close(new CloseSessionSettings());
    }
}
