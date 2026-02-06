package tech.ydb.table.impl;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.call.ProxyReadStream;
import tech.ydb.core.operation.Operation;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.Session;
import tech.ydb.table.SessionSupplier;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.query.DataQuery;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.ExplainDataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.query.ReadRowsResult;
import tech.ydb.table.query.ReadTablePart;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
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
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.ListValue;
import tech.ydb.table.values.StructValue;
import tech.ydb.table.values.Value;
import tech.ydb.table.values.proto.ProtoValue;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ImplicitSession implements Session, SessionSupplier {
    private final TableRpc rpc;

    private ImplicitSession(GrpcTransport transport) {
        this.rpc = GrpcTableRpc.useTransport(transport);
    }

    @Override
    public CompletableFuture<Result<Session>> createSession(Duration duration) {
        return CompletableFuture.completedFuture(Result.success(this));
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return rpc.getScheduler();
    }

    private GrpcRequestSettings makeGrpcRequestSettings(Duration timeout, String traceId) {
        return GrpcRequestSettings.newBuilder()
                .withDeadline(timeout)
                .withTraceId(traceId == null ? UUID.randomUUID().toString() : traceId)
                .build();
    }

    @Override
    public CompletableFuture<Result<ReadRowsResult>> readRows(String pathToTable, ReadRowsSettings settings) {
        ValueProtos.TypedValue keys = ValueProtos.TypedValue.newBuilder().build();
        if (!settings.getKeys().isEmpty()) {
            ValueProtos.Type type = ListType.of(settings.getKeys().get(0).getType()).toPb();
            List<ValueProtos.Value> values = settings.getKeys().stream()
                    .map(StructValue::toPb)
                    .collect(Collectors.toList());
            keys = ValueProtos.TypedValue.newBuilder()
                    .setType(type)
                    .setValue(ValueProtos.Value.newBuilder().addAllItems(values))
                    .build();
        }

        YdbTable.ReadRowsRequest request = YdbTable.ReadRowsRequest.newBuilder()
                .setPath(pathToTable)
                .addAllColumns(settings.getColumns())
                .setKeys(keys)
                .build();

        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings.getRequestTimeout(), settings.getTraceId());
        return rpc.readRows(request, grpcSettings).thenApply(result -> result.map(ReadRowsResult::new));
    }

    @Override
    public GrpcReadStream<ReadTablePart> executeReadTable(String tablePath, ReadTableSettings settings) {
        YdbTable.ReadTableRequest.Builder request = YdbTable.ReadTableRequest.newBuilder()
                .setPath(tablePath)
                .setOrdered(settings.isOrdered())
                .setRowLimit(settings.getRowLimit())
                .setBatchLimitBytes(settings.batchLimitBytes())
                .setBatchLimitRows(settings.batchLimitRows());

        Value<?> fromKey = settings.getFromKey();
        if (fromKey != null) {
            YdbTable.KeyRange.Builder range = request.getKeyRangeBuilder();
            if (settings.isFromInclusive()) {
                range.setGreaterOrEqual(ProtoValue.toTypedValue(fromKey));
            } else {
                range.setGreater(ProtoValue.toTypedValue(fromKey));
            }
        }

        Value<?> toKey = settings.getToKey();
        if (toKey != null) {
            YdbTable.KeyRange.Builder range = request.getKeyRangeBuilder();
            if (settings.isToInclusive()) {
                range.setLessOrEqual(ProtoValue.toTypedValue(toKey));
            } else {
                range.setLess(ProtoValue.toTypedValue(toKey));
            }
        }

        if (!settings.getColumns().isEmpty()) {
            request.addAllColumns(settings.getColumns());
        }

        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings.getRequestTimeout(), settings.getTraceId());
        GrpcReadStream<YdbTable.ReadTableResponse> origin = rpc.streamReadTable(request.build(), grpcSettings);

        return new ProxyReadStream<>(origin, (response, future, observer) -> {
            StatusCodesProtos.StatusIds.StatusCode statusCode = response.getStatus();
            if (statusCode == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
                try {
                    observer.onNext(new ReadTablePart(response.getResult(), response.getSnapshot()));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                    origin.cancel();
                }
            } else {
                Issue[] issues = Issue.fromPb(response.getIssuesList());
                StatusCode code = StatusCode.fromProto(statusCode);
                future.complete(Status.of(code, issues));
                origin.cancel();
            }
        });
    }

    @Override
    public GrpcReadStream<ResultSetReader> executeScanQuery(
            String query, Params params, ExecuteScanQuerySettings settings
    ) {
        YdbTable.ExecuteScanQueryRequest request = YdbTable.ExecuteScanQueryRequest.newBuilder()
                .setQuery(YdbTable.Query.newBuilder().setYqlText(query))
                .setMode(settings.getMode().toPb())
                .putAllParameters(params.toPb())
                .setCollectStats(settings.getCollectStats().toPb())
                .build();

        GrpcReadStream<YdbTable.ExecuteScanQueryPartialResponse> origin = rpc.streamExecuteScanQuery(
                request, makeGrpcRequestSettings(settings.getRequestTimeout(), settings.getTraceId())
        );

        return new ProxyReadStream<>(origin, (response, future, observer) -> {
            StatusCodesProtos.StatusIds.StatusCode statusCode = response.getStatus();
            if (statusCode == StatusCodesProtos.StatusIds.StatusCode.SUCCESS) {
                try {
                    observer.onNext(ProtoValueReaders.forResultSet(response.getResult().getResultSet()));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                    origin.cancel();
                }
            } else {
                Issue[] issues = Issue.fromPb(response.getIssuesList());
                StatusCode code = StatusCode.fromProto(statusCode);
                future.complete(Status.of(code, issues));
                origin.cancel();
            }
        });
    }

    @Override
    public CompletableFuture<Status> executeBulkUpsert(String tablePath, ListValue rows, BulkUpsertSettings settings) {
        ValueProtos.TypedValue typedRows = ValueProtos.TypedValue.newBuilder()
                .setType(rows.getType().toPb())
                .setValue(rows.toPb())
                .build();

        YdbTable.BulkUpsertRequest request = YdbTable.BulkUpsertRequest.newBuilder()
                .setTable(tablePath)
                .setRows(typedRows)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        return rpc.bulkUpsert(request, makeGrpcRequestSettings(settings.getTimeoutDuration(), settings.getTraceId()));
    }

    public static ImplicitSession of(GrpcTransport transport) {
        return new ImplicitSession(transport);
    }

    @Override
    public String getId() {
        return "Implicit YDB session";
    }

    @Override
    public void close() {
        // NOTHING
    }

    @Override
    public CompletableFuture<Status> createTable(
            String path, TableDescription tableDescriptions, CreateTableSettings settings
    ) {
        throw new UnsupportedOperationException("Implicit session doesn't support createTable");
    }

    @Override
    public CompletableFuture<Status> dropTable(String path, DropTableSettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support dropTable");
    }

    @Override
    public CompletableFuture<Status> alterTable(String path, AlterTableSettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support alterTable");
    }

    @Override
    public CompletableFuture<Status> copyTable(String src, String dst, CopyTableSettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support copyTable");
    }

    @Override
    public CompletableFuture<Status> copyTables(CopyTablesSettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support copyTables");
    }

    @Override
    public CompletableFuture<Status> renameTables(RenameTablesSettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support renameTables");
    }

    @Override
    public CompletableFuture<Result<TableDescription>> describeTable(String path, DescribeTableSettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support describeTable");
    }

    @Override
    public CompletableFuture<Result<DataQuery>> prepareDataQuery(String query, PrepareDataQuerySettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support prepareDataQuery");
    }

    @Override
    public CompletableFuture<Result<DataQueryResult>> executeDataQuery(
            String query, TxControl<?> txControl, Params params, ExecuteDataQuerySettings settings
    ) {
        throw new UnsupportedOperationException("Implicit session doesn't support executeDataQuery");
    }

    @Override
    public CompletableFuture<Status> executeSchemeQuery(String query, ExecuteSchemeQuerySettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support executeSchemeQuery");
    }

    @Override
    public CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(
            String query, ExplainDataQuerySettings settings
    ) {
        throw new UnsupportedOperationException("Implicit session doesn't support explainDataQuery");
    }

    @Override
    public CompletableFuture<Result<Transaction>> beginTransaction(
            Transaction.Mode transactionMode, BeginTxSettings settings
    ) {
        throw new UnsupportedOperationException("Implicit session doesn't support beginTransaction");
    }

    @Override
    public CompletableFuture<Result<TableTransaction>> beginTransaction(TxMode txMode, BeginTxSettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support beginTransaction");
    }

    @Override
    public TableTransaction createNewTransaction(TxMode txMode) {
        throw new UnsupportedOperationException("Implicit session doesn't support createNewTransaction");
    }

    @Override
    public CompletableFuture<Status> commitTransaction(String txId, CommitTxSettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support commitTransaction");
    }

    @Override
    public CompletableFuture<Status> rollbackTransaction(String txId, RollbackTxSettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support rollbackTransaction");
    }

    @Override
    public CompletableFuture<Result<State>> keepAlive(KeepAliveSessionSettings settings) {
        throw new UnsupportedOperationException("Implicit session doesn't support keepAlive");
    }
}
