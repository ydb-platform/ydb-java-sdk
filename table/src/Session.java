package ru.yandex.ydb.table;

import java.util.concurrent.CompletableFuture;

import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.Status;
import ru.yandex.ydb.table.description.TableDescription;
import ru.yandex.ydb.table.query.DataQuery;
import ru.yandex.ydb.table.query.DataQueryResult;
import ru.yandex.ydb.table.query.ExplainDataQueryResult;
import ru.yandex.ydb.table.query.Params;
import ru.yandex.ydb.table.settings.AlterTableSettings;
import ru.yandex.ydb.table.settings.BeginTxSettings;
import ru.yandex.ydb.table.settings.CloseSessionSettings;
import ru.yandex.ydb.table.settings.CopyTableSettings;
import ru.yandex.ydb.table.settings.CreateTableSettings;
import ru.yandex.ydb.table.settings.DescribeTableSettings;
import ru.yandex.ydb.table.settings.DropTableSettings;
import ru.yandex.ydb.table.settings.ExecuteDataQuerySettings;
import ru.yandex.ydb.table.settings.ExecuteSchemeQuerySettings;
import ru.yandex.ydb.table.settings.ExplainDataQuerySettings;
import ru.yandex.ydb.table.settings.PrepareDataQuerySettings;
import ru.yandex.ydb.table.transaction.Transaction;
import ru.yandex.ydb.table.transaction.TransactionMode;
import ru.yandex.ydb.table.transaction.TxControl;


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

    CompletableFuture<Status> close(CloseSessionSettings settings);

    default CompletableFuture<Status> close() {
        return close(new CloseSessionSettings());
    }
}
