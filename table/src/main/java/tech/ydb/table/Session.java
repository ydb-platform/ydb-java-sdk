package tech.ydb.table;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.grpc.ExperimentalApi;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.impl.call.ProxyReadStream;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.query.DataQuery;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.ExplainDataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.query.ReadRowsResult;
import tech.ydb.table.query.ReadTablePart;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.BeginTxSettings;
import tech.ydb.table.settings.BulkUpsertSettings;
import tech.ydb.table.settings.CommitTxSettings;
import tech.ydb.table.settings.CopyTableSettings;
import tech.ydb.table.settings.CopyTablesSettings;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.settings.DescribeTableSettings;
import tech.ydb.table.settings.DropTableSettings;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.settings.ExecuteScanQuerySettings;
import tech.ydb.table.settings.ExecuteSchemeQuerySettings;
import tech.ydb.table.settings.ExplainDataQuerySettings;
import tech.ydb.table.settings.KeepAliveSessionSettings;
import tech.ydb.table.settings.PrepareDataQuerySettings;
import tech.ydb.table.settings.ReadRowsSettings;
import tech.ydb.table.settings.ReadTableSettings;
import tech.ydb.table.settings.RenameTablesSettings;
import tech.ydb.table.settings.RollbackTxSettings;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.table.transaction.Transaction;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.values.ListValue;


/**
 * @author Sergey Polovko
 * @author Nikolay Perfilov
 */
public interface Session extends AutoCloseable {
    enum State {
        UNSPECIFIED,
        READY,
        BUSY;
    }

    String getId();

    @Override
    void close();

    CompletableFuture<Status> createTable(String path, TableDescription tableDescriptions,
                                          CreateTableSettings settings);

    CompletableFuture<Status> dropTable(String path, DropTableSettings settings);

    CompletableFuture<Status> alterTable(String path, AlterTableSettings settings);

    CompletableFuture<Status> copyTable(String src, String dst, CopyTableSettings settings);

    CompletableFuture<Status> copyTables(CopyTablesSettings settings);

    CompletableFuture<Status> renameTables(RenameTablesSettings settings);

    CompletableFuture<Result<TableDescription>> describeTable(String path, DescribeTableSettings settings);

    CompletableFuture<Result<DataQuery>> prepareDataQuery(String query, PrepareDataQuerySettings settings);

    CompletableFuture<Result<DataQueryResult>> executeDataQuery(
        String query,
        TxControl<?> txControl,
        Params params,
        ExecuteDataQuerySettings settings);

    CompletableFuture<Result<ReadRowsResult>> readRows(String pathToTable, ReadRowsSettings settings);

    CompletableFuture<Status> executeSchemeQuery(String query, ExecuteSchemeQuerySettings settings);

    CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(String query, ExplainDataQuerySettings settings);

    /**
     * Consider using {@link Session#beginTransaction(TxMode, BeginTxSettings)} instead
     */
    CompletableFuture<Result<Transaction>> beginTransaction(Transaction.Mode transactionMode, BeginTxSettings settings);

    /**
     * Create a new <i>not active</i> {@link TableTransaction}. This TableDescription will have no identifier and
     * starts a transaction on server by execution a query
     * @param txMode transaction mode
     * @return new implicit transaction
     */
    @ExperimentalApi("New table transaction interfaces are experimental and may change without notice")
    TableTransaction createNewTransaction(TxMode txMode);

    /**
     * Create and start a new <i>active</i> {@link TableTransaction}. This method creates a transaction on the server
     * and returns TableDescription which is ready to execute queries on this server transaction
     *
     * @param txMode transaction mode
     * @param settings additional settings for request
     * @return future with result of the transaction starting
     */
    @ExperimentalApi("New table transaction interfaces are experimental and may change without notice")
    CompletableFuture<Result<TableTransaction>> beginTransaction(TxMode txMode, BeginTxSettings settings);

    /**
     * Create and start a new <i>active</i> {@link TableTransaction}. This method creates a transaction on the server
     * and returns TableDescription which is ready to execute queries on this server transaction
     *
     * @param txMode transaction mode
     * @return future with result of the transaction starting
     */
    @ExperimentalApi("New table transaction interfaces are experimental and may change without notice")
    default CompletableFuture<Result<TableTransaction>> beginTransaction(TxMode txMode) {
        return beginTransaction(txMode, new BeginTxSettings());
    }

    /**
     * Consider using {@link TableTransaction#commit()} ()} instead
     */
    CompletableFuture<Status> commitTransaction(String txId, CommitTxSettings settings);

    /**
     * Consider using {@link TableTransaction#rollback()} instead
     */
    CompletableFuture<Status> rollbackTransaction(String txId, RollbackTxSettings settings);

    GrpcReadStream<ReadTablePart> executeReadTable(String tablePath, ReadTableSettings settings);

    GrpcReadStream<ResultSetReader> executeScanQuery(String query, Params params, ExecuteScanQuerySettings settings);

    @Deprecated
    default GrpcReadStream<ResultSetReader> readTable(String tablePath, ReadTableSettings settings) {
        return new ProxyReadStream<>(executeReadTable(tablePath, settings), (part, promise, observer) -> {
            observer.onNext(part.getResultSetReader());
        });
    }

    @Deprecated
    default CompletableFuture<Status> readTable(String tablePath, ReadTableSettings settings,
            Consumer<ResultSetReader> fn) {
        return executeReadTable(tablePath, settings).start(part -> fn.accept(part.getResultSetReader()));
    }

    @Deprecated
    default CompletableFuture<Status> executeScanQuery(String query, Params params, ExecuteScanQuerySettings settings,
            Consumer<ResultSetReader> fn) {
        return executeScanQuery(query, params, settings).start(fn::accept);
    }

    CompletableFuture<Result<State>> keepAlive(KeepAliveSessionSettings settings);

    CompletableFuture<Status> executeBulkUpsert(String tablePath, ListValue rows, BulkUpsertSettings settings);

    default CompletableFuture<Status> createTable(String path, TableDescription tableDescriptions) {
        return createTable(path, tableDescriptions, new CreateTableSettings());
    }

    default CompletableFuture<Status> dropTable(String path) {
        return dropTable(path, new DropTableSettings());
    }

    default CompletableFuture<Status> alterTable(String path) {
        return alterTable(path, new AlterTableSettings());
    }

    default CompletableFuture<Status> copyTable(String src, String dst) {
        return copyTable(src, dst, new CopyTableSettings());
    }

    default CompletableFuture<Status> renameTable(String src, String dst) {
        return renameTables(new RenameTablesSettings().addTable(src, dst));
    }

    default CompletableFuture<Status> renameTable(String src, String dst, boolean overwrite) {
        return renameTables(new RenameTablesSettings().addTable(src, dst, overwrite));
    }

    default CompletableFuture<Result<TableDescription>> describeTable(String path) {
        return describeTable(path, new DescribeTableSettings());
    }

    default CompletableFuture<Result<DataQueryResult>> executeDataQuery(String query, TxControl<?> txControl,
            Params params) {
        return executeDataQuery(query, txControl, params, new ExecuteDataQuerySettings());
    }

    default CompletableFuture<Result<DataQueryResult>> executeDataQuery(String query, TxControl<?> txControl) {
        return executeDataQuery(query, txControl, Params.empty(), new ExecuteDataQuerySettings());
    }

    default CompletableFuture<Result<DataQuery>> prepareDataQuery(String query) {
        return prepareDataQuery(query, new PrepareDataQuerySettings());
    }

    default CompletableFuture<Status> executeSchemeQuery(String query) {
        return executeSchemeQuery(query, new ExecuteSchemeQuerySettings());
    }

    default CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(String query) {
        return explainDataQuery(query, new ExplainDataQuerySettings());
    }

    /**
     * Consider using {@link Session#beginTransaction(TxMode)} instead
     */
    default CompletableFuture<Result<Transaction>> beginTransaction(Transaction.Mode transactionMode) {
        return beginTransaction(transactionMode, new BeginTxSettings());
    }

    default CompletableFuture<Status> executeBulkUpsert(String tablePath, ListValue rows) {
        return executeBulkUpsert(tablePath, rows, new BulkUpsertSettings());
    }

    default CompletableFuture<Result<State>> keepAlive() {
        return keepAlive(new KeepAliveSessionSettings());
    }
}
