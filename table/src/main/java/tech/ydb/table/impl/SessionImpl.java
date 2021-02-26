package tech.ydb.table.impl;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.protobuf.Timestamp;
import tech.ydb.StatusCodesProtos.StatusIds;
import tech.ydb.ValueProtos;
import tech.ydb.common.CommonProtos;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.table.Session;
import tech.ydb.table.SessionStatus;
import tech.ydb.table.YdbTable;
import tech.ydb.table.YdbTable.ReadTableRequest;
import tech.ydb.table.YdbTable.ReadTableResponse;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.description.TableIndex;
import tech.ydb.table.query.DataQuery;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.ExplainDataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.AutoPartitioningPolicy;
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
import tech.ydb.table.settings.PartitioningPolicy;
import tech.ydb.table.settings.PrepareDataQuerySettings;
import tech.ydb.table.settings.ReadTableSettings;
import tech.ydb.table.settings.ReplicationPolicy;
import tech.ydb.table.settings.RollbackTxSettings;
import tech.ydb.table.settings.StoragePolicy;
import tech.ydb.table.settings.TtlSettings;
import tech.ydb.table.transaction.Transaction;
import tech.ydb.table.transaction.TransactionMode;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.utils.OperationParamUtils;
import tech.ydb.table.values.ListValue;
import tech.ydb.table.values.Value;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
class SessionImpl implements Session {

    enum State {
        IDLE,
        BROKEN,
        ACTIVE,
        DISCONNECTED,
    }

    private static final AtomicReferenceFieldUpdater<SessionImpl, State> stateUpdater =
        AtomicReferenceFieldUpdater.newUpdater(SessionImpl.class, State.class, "state");

    private final String id;
    private final TableRpc tableRpc;
    private final OperationTray operationTray;
    @Nullable
    private final SessionPool sessionPool;
    @Nullable
    private final QueryCache queryCache;
    private final boolean keepQueryText;

    private volatile State state = State.ACTIVE;

    SessionImpl(String id, TableRpc tableRpc, SessionPool sessionPool, int queryCacheSize, boolean keepQueryText) {
        this.id = id;
        this.tableRpc = tableRpc;
        this.operationTray = tableRpc.getOperationTray();
        this.sessionPool = sessionPool;
        this.queryCache = (queryCacheSize > 0) ? new QueryCache(queryCacheSize) : null;
        this.keepQueryText = keepQueryText;
    }

    @Override
    public String getId() {
        return id;
    }

    State getState() {
        return stateUpdater.get(this);
    }

    void setState(State state) {
        stateUpdater.set(this, state);
    }

    boolean switchState(State from, State to) {
        return stateUpdater.compareAndSet(this, from, to);
    }

    @Override
    public CompletableFuture<Status> createTable(
        String path,
        TableDescription tableDescriptions,
        CreateTableSettings settings)
    {
        YdbTable.CreateTableRequest.Builder request = YdbTable.CreateTableRequest.newBuilder()
            .setSessionId(id)
            .setPath(path)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .addAllPrimaryKey(tableDescriptions.getPrimaryKeys());

        for (TableColumn column : tableDescriptions.getColumns()) {
            request.addColumns(YdbTable.ColumnMeta.newBuilder()
                .setName(column.getName())
                .setType(column.getType().toPb())
                .build());
        }

        for (TableIndex index : tableDescriptions.getIndexes()) {
            YdbTable.TableIndex.Builder b = request.addIndexesBuilder();
            b.setName(index.getName());
            b.addAllIndexColumns(index.getColumns());
            if (index.getType() == TableIndex.Type.GLOBAL) {
                b.setGlobalIndex(YdbTable.GlobalIndex.getDefaultInstance());
            }
        }

        if (settings.getPresetName() != null) {
            request.getProfileBuilder()
                .setPresetName(settings.getPresetName());
        }

        if (settings.getExecutionPolicy() != null) {
            request.getProfileBuilder()
                .getExecutionPolicyBuilder()
                .setPresetName(settings.getExecutionPolicy());
        }

        if (settings.getCompactionPolicy() != null) {
            request.getProfileBuilder()
                .getCompactionPolicyBuilder()
                .setPresetName(settings.getExecutionPolicy());
        }

        {
            PartitioningPolicy policy = settings.getPartitioningPolicy();
            if (policy != null) {
                YdbTable.PartitioningPolicy.Builder policyProto = request.getProfileBuilder()
                    .getPartitioningPolicyBuilder();
                if (policy.getPresetName() != null) {
                    policyProto.setPresetName(policy.getPresetName());
                }
                if (policy.getAutoPartitioning() != null) {
                    policyProto.setAutoPartitioning(toPb(policy.getAutoPartitioning()));
                }

                if (policy.getUniformPartitions() > 0) {
                    policyProto.setUniformPartitions(policy.getUniformPartitions());
                } else if (policy.getExplicitPartitioningPoints() != null) {
                    YdbTable.ExplicitPartitions.Builder b = policyProto.getExplicitPartitionsBuilder();
                    for (Value p : policy.getExplicitPartitioningPoints()) {
                        b.addSplitPoints(ProtoValue.toTypedValue(p));
                    }
                }
            }
        }

        {
            StoragePolicy policy = settings.getStoragePolicy();
            if (policy != null) {
                YdbTable.StoragePolicy.Builder policyProto = request.getProfileBuilder()
                    .getStoragePolicyBuilder();
                if (policy.getPresetName() != null) {
                    policyProto.setPresetName(policy.getPresetName());
                }
                if (policy.getSysLog() != null) {
                    policyProto.getSyslogBuilder().setMedia(policy.getSysLog());
                }
                if (policy.getLog() != null) {
                    policyProto.getLogBuilder().setMedia(policy.getLog());
                }
                if (policy.getData() != null) {
                    policyProto.getDataBuilder().setMedia(policy.getData());
                }
                if (policy.getExternal() != null) {
                    policyProto.getExternalBuilder().setMedia(policy.getExternal());
                }
            }
        }

        {
            ReplicationPolicy policy = settings.getReplicationPolicy();
            if (policy != null) {
                YdbTable.ReplicationPolicy.Builder replicationPolicyProto =
                    request.getProfileBuilder().getReplicationPolicyBuilder();
                if (policy.getPresetName() != null) {
                    replicationPolicyProto.setPresetName(policy.getPresetName());
                }
                replicationPolicyProto.setReplicasCount(policy.getReplicasCount());
                replicationPolicyProto.setCreatePerAvailabilityZone(policy.isCreatePerAvailabilityZone() ?
                    CommonProtos.FeatureFlag.Status.ENABLED : CommonProtos.FeatureFlag.Status.DISABLED);
                replicationPolicyProto.setAllowPromotion(policy.isAllowPromotion() ?
                    CommonProtos.FeatureFlag.Status.ENABLED : CommonProtos.FeatureFlag.Status.DISABLED);
            }
        }

        {
            TtlSettings ttlSettings = settings.getTtlSettings();
            if (ttlSettings != null) {
                YdbTable.DateTypeColumnModeSettings.Builder dateTypeColumnBuilder = request.getTtlSettingsBuilder().getDateTypeColumnBuilder();
                dateTypeColumnBuilder.setColumnName(ttlSettings.getDateTimeColumn());
                dateTypeColumnBuilder.setExpireAfterSeconds(ttlSettings.getExpireAfterSeconds());
            }
        }

        final long deadlineAfter = settings.getDeadlineAfter();
        return tableRpc.createTable(request.build(), deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationTray.waitStatus(response.expect("createTable()").getOperation(), deadlineAfter);
            });
    }

    private static YdbTable.PartitioningPolicy.AutoPartitioningPolicy toPb(AutoPartitioningPolicy policy) {
        switch (policy) {
            case AUTO_SPLIT: return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.AUTO_SPLIT;
            case AUTO_SPLIT_MERGE: return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.AUTO_SPLIT_MERGE;
            case DISABLED: return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.DISABLED;
        }
        throw new IllegalArgumentException("unknown AutoPartitioningPolicy: " + policy);
    }

    @Override
    public CompletableFuture<Status> dropTable(String path, DropTableSettings settings) {
        YdbTable.DropTableRequest request = YdbTable.DropTableRequest.newBuilder()
            .setSessionId(id)
            .setPath(path)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .build();

        final long deadlineAfter = settings.getDeadlineAfter();
        return tableRpc.dropTable(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationTray.waitStatus(response.expect("dropTable()").getOperation(), deadlineAfter);
            });
    }

    @Override
    public CompletableFuture<Status> alterTable(String path, AlterTableSettings settings) {
        YdbTable.AlterTableRequest.Builder builder = YdbTable.AlterTableRequest.newBuilder()
            .setSessionId(id)
            .setPath(path)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings));

        settings.forEachAddColumn((name, type) -> {
            builder.addAddColumns(YdbTable.ColumnMeta.newBuilder()
                .setName(name)
                .setType(type.toPb())
                .build());
        });

        settings.forEachDropColumn(builder::addDropColumns);

        TtlSettings ttlSettings = settings.getTtlSettings();
        if (ttlSettings != null) {
            YdbTable.DateTypeColumnModeSettings.Builder dateTypeColumnBuilder = builder.getSetTtlSettingsBuilder().getDateTypeColumnBuilder();
            dateTypeColumnBuilder.setColumnName(ttlSettings.getDateTimeColumn());
            dateTypeColumnBuilder.setExpireAfterSeconds(ttlSettings.getExpireAfterSeconds());
        }

        final long deadlineAfter = settings.getDeadlineAfter();
        return tableRpc.alterTable(builder.build(), deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationTray.waitStatus(response.expect("alterTable()").getOperation(), deadlineAfter);
            });
    }

    @Override
    public CompletableFuture<Status> copyTable(String src, String dst, CopyTableSettings settings) {
        YdbTable.CopyTableRequest request = YdbTable.CopyTableRequest.newBuilder()
            .setSessionId(id)
            .setSourcePath(src)
            .setDestinationPath(dst)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .build();

        final long deadlineAfter = settings.getDeadlineAfter();
        return tableRpc.copyTable(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationTray.waitStatus(response.expect("copyTable()").getOperation(), deadlineAfter);
            });
    }

    @Override
    public CompletableFuture<Result<TableDescription>> describeTable(String path, DescribeTableSettings settings) {
        YdbTable.DescribeTableRequest request = YdbTable.DescribeTableRequest.newBuilder()
            .setSessionId(id)
            .setPath(path)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .setIncludeTableStats(settings.isIncludeTableStats())
            .build();

        final long deadlineAfter = settings.getDeadlineAfter();
        return tableRpc.describeTable(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationTray.waitResult(
                    response.expect("describeTable()").getOperation(),
                    YdbTable.DescribeTableResult.class,
                    SessionImpl::mapDescribeTable,
                    deadlineAfter);
            });
    }

    private static TableDescription mapDescribeTable(YdbTable.DescribeTableResult result) {
        TableDescription.Builder description = TableDescription.newBuilder();
        for (int i = 0; i < result.getColumnsCount(); i++) {
            YdbTable.ColumnMeta column = result.getColumns(i);
            description.addNonnullColumn(column.getName(), ProtoType.fromPb(column.getType()));
        }
        description.setPrimaryKeys(result.getPrimaryKeyList());
        for (int i = 0; i < result.getIndexesCount(); i++) {
            YdbTable.TableIndexDescription index = result.getIndexes(i);
            description.addGlobalIndex(index.getName(), index.getIndexColumnsList());
        }
        YdbTable.TableStats resultTableStats = result.getTableStats();
        if (resultTableStats != null) {
            Timestamp creationTime = resultTableStats.getCreationTime();
            Instant createdAt = creationTime == null ? null : Instant.ofEpochSecond(creationTime.getSeconds(), creationTime.getNanos());
            Timestamp modificationTime = resultTableStats.getCreationTime();
            Instant modifiedAt = modificationTime == null ? null : Instant.ofEpochSecond(modificationTime.getSeconds(), modificationTime.getNanos());
            TableDescription.TableStats tableStats = new TableDescription.TableStats(
                    createdAt, modifiedAt, resultTableStats.getRowsEstimate(), resultTableStats.getStoreSize());
            description.tableStats(tableStats);
        }

        return description.build();
    }

    private static YdbTable.TransactionSettings txSettings(TransactionMode transactionMode) {
        YdbTable.TransactionSettings.Builder settings = YdbTable.TransactionSettings.newBuilder();
        if (transactionMode == TransactionMode.SERIALIZABLE_READ_WRITE) {
            settings.setSerializableReadWrite(YdbTable.SerializableModeSettings.getDefaultInstance());
        } else if (transactionMode == TransactionMode.ONLINE_READ_ONLY) {
            settings.setOnlineReadOnly(YdbTable.OnlineModeSettings.getDefaultInstance());
        } else if (transactionMode == TransactionMode.STALE_READ_ONLY) {
            settings.setStaleReadOnly(YdbTable.StaleModeSettings.getDefaultInstance());
        }
        return settings.build();
    }

    @Override
    public CompletableFuture<Result<DataQueryResult>> executeDataQuery(
        String query, TxControl txControl, Params params, ExecuteDataQuerySettings settings)
    {
        if (queryCache != null) {
            DataQueryImpl dataQuery = queryCache.find(query);
            if (dataQuery != null) {
                return dataQuery.execute(txControl, params, settings)
                    .whenComplete((r, t) -> {
                        if (r.getCode() == StatusCode.NOT_FOUND) {
                            queryCache.remove(dataQuery);
                        }
                    });
            }
        }

        YdbTable.ExecuteDataQueryRequest.Builder request = YdbTable.ExecuteDataQueryRequest.newBuilder()
            .setSessionId(id)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .setTxControl(txControl.toPb())
            .setQuery(YdbTable.Query.newBuilder().setYqlText(query))
            .putAllParameters(params.toPb());

        boolean keepInQueryCache = settings.isKeepInQueryCache();
        if (queryCache != null && keepInQueryCache) {
            request.getQueryCachePolicyBuilder()
                .setKeepInCache(true);
        }

        final long deadlineAfter = settings.getDeadlineAfter();
        return interceptResult(tableRpc.executeDataQuery(request.build(), deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationTray.waitResult(
                    response.expect("executeDataQuery()").getOperation(),
                    YdbTable.ExecuteQueryResult.class,
                    result -> mapExecuteDataQuery(result, query, keepInQueryCache),
                    deadlineAfter);
            }));
    }

    private DataQueryResult mapExecuteDataQuery(
        YdbTable.ExecuteQueryResult result,
        @Nullable String queryText,
        boolean keepInQueryCache)
    {
        if (keepInQueryCache && result.hasQueryMeta() && queryText != null) {
            assert queryCache != null;
            String queryId = result.getQueryMeta().getId();
            Map<String, ValueProtos.Type> types = result.getQueryMeta().getParametersTypesMap();
            queryCache.put(new DataQueryImpl(this, queryId, queryText, keepQueryText, types));
        }

        YdbTable.TransactionMeta txMeta = result.getTxMeta();
        return new DataQueryResult(txMeta.getId(), result.getResultSetsList());
    }

    CompletableFuture<Result<DataQueryResult>> executePreparedDataQuery(
        String queryId, @Nullable String queryText, TxControl txControl, Params params, ExecuteDataQuerySettings settings)
    {
        YdbTable.ExecuteDataQueryRequest.Builder request = YdbTable.ExecuteDataQueryRequest.newBuilder()
            .setSessionId(id)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .setTxControl(txControl.toPb());

        request.getQueryBuilder().setId(queryId);
        request.putAllParameters(params.toPb());

        boolean keepInQueryCache = (queryCache != null) && settings.isKeepInQueryCache();
        if (keepInQueryCache) {
            request.getQueryCachePolicyBuilder()
                .setKeepInCache(true);
        }

        final long deadlineAfter = settings.getDeadlineAfter();
        return interceptResult(tableRpc.executeDataQuery(request.build(), deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return tableRpc.getOperationTray().waitResult(
                    response.expect("executeDataQuery()").getOperation(),
                    YdbTable.ExecuteQueryResult.class,
                    result -> mapExecuteDataQuery(result, queryText, keepInQueryCache),
                    deadlineAfter);
            }));
    }

    @Override
    public CompletableFuture<Result<DataQuery>> prepareDataQuery(String query, PrepareDataQuerySettings settings) {
        YdbTable.PrepareDataQueryRequest.Builder request = YdbTable.PrepareDataQueryRequest.newBuilder()
            .setSessionId(id)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .setYqlText(query);

        final boolean keepInQueryCache = (queryCache != null) && settings.isKeepInQueryCache();
        final long deadlineAfter = settings.getDeadlineAfter();
        return interceptResult(tableRpc.prepareDataQuery(request.build(), deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationTray.waitResult(
                    response.expect("prepareDataQuery()").getOperation(),
                    YdbTable.PrepareQueryResult.class,
                    result -> {
                        String queryId = result.getQueryId();
                        Map<String, ValueProtos.Type> types = result.getParametersTypesMap();
                        DataQueryImpl dataQuery = new DataQueryImpl(this, queryId, query, keepQueryText, types);
                        if (keepInQueryCache) {
                            queryCache.put(dataQuery);
                        }
                        return dataQuery;
                    },
                    deadlineAfter);
            }));
    }

    @Override
    public CompletableFuture<Status> executeSchemeQuery(String query, ExecuteSchemeQuerySettings settings) {
        YdbTable.ExecuteSchemeQueryRequest request = YdbTable.ExecuteSchemeQueryRequest.newBuilder()
            .setSessionId(id)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .setYqlText(query)
            .build();

        final long deadlineAfter = settings.getDeadlineAfter();
        return interceptStatus(tableRpc.executeSchemeQuery(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationTray.waitStatus(response.expect("executeSchemaQuery()").getOperation(), deadlineAfter);
            }));
    }

    @Override
    public CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(String query, ExplainDataQuerySettings settings) {
        YdbTable.ExplainDataQueryRequest request = YdbTable.ExplainDataQueryRequest.newBuilder()
            .setSessionId(id)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .setYqlText(query)
            .build();

        final long deadlineAfter = settings.getDeadlineAfter();
        return interceptResult(tableRpc.explainDataQuery(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationTray.waitResult(
                    response.expect("explainDataQuery()").getOperation(),
                    YdbTable.ExplainQueryResult.class,
                    result -> new ExplainDataQueryResult(result.getQueryAst(), result.getQueryPlan()),
                    deadlineAfter);
            }));
    }

    @Override
    public CompletableFuture<Result<Transaction>> beginTransaction(TransactionMode transactionMode, BeginTxSettings settings) {
        YdbTable.BeginTransactionRequest request = YdbTable.BeginTransactionRequest.newBuilder()
            .setSessionId(id)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .setTxSettings(txSettings(transactionMode))
            .build();

        final long deadlineAfter = settings.getDeadlineAfter();
        return interceptResult(tableRpc.beginTransaction(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationTray.waitResult(
                    response.expect("beginTransaction()").getOperation(),
                    YdbTable.BeginTransactionResult.class,
                    result -> new TransactionImpl(this, result.getTxMeta().getId()),
                    deadlineAfter);
            }));
    }

    @Override
    public CompletableFuture<Status> readTable(String tablePath, ReadTableSettings settings, Consumer<ResultSetReader> fn) {
        ReadTableRequest.Builder request = ReadTableRequest.newBuilder()
            .setSessionId(id)
            .setPath(tablePath)
            .setOrdered(settings.isOrdered())
            .setRowLimit(settings.getRowLimit());

        Value fromKey = settings.getFromKey();
        if (fromKey != null) {
            YdbTable.KeyRange.Builder range = request.getKeyRangeBuilder();
            if (settings.isFromInclusive()) {
                range.setGreaterOrEqual(ProtoValue.toTypedValue(fromKey));
            } else {
                range.setGreater(ProtoValue.toTypedValue(fromKey));
            }
        }

        Value toKey = settings.getToKey();
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

        final long deadlineAfter = settings.getDeadlineAfter();
        CompletableFuture<Status> promise = new CompletableFuture<>();
        StreamControl control = tableRpc.streamReadTable(request.build(), new StreamObserver<ReadTableResponse>() {
            @Override
            public void onNext(ReadTableResponse response) {
                StatusIds.StatusCode statusCode = response.getStatus();
                if (statusCode == StatusIds.StatusCode.SUCCESS) {
                    try {
                        fn.accept(ProtoValueReaders.forResultSet(response.getResult().getResultSet()));
                    } catch (Throwable t) {
                        promise.completeExceptionally(t);
                        throw new IllegalStateException(t);
                    }
                } else {
                    Issue[] issues = Issue.fromPb(response.getIssuesList());
                    StatusCode code = StatusCode.fromProto(statusCode);
                    promise.complete(Status.of(code, issues));
                }
            }

            @Override
            public void onError(Status status) {
                assert !status.isSuccess();
                promise.complete(status);
            }

            @Override
            public void onCompleted() {
                promise.complete(Status.SUCCESS);
            }
        }, deadlineAfter);
        return promise.whenComplete((status, ex) -> {
            if (ex instanceof CancellationException) {
                control.cancel();
            }
        });
    }

    public CompletableFuture<Status> executeScanQuery(String query, Params params, ExecuteScanQuerySettings settings, Consumer<ResultSetReader> fn)
    {
        YdbTable.ExecuteScanQueryRequest request = YdbTable.ExecuteScanQueryRequest.newBuilder()
                .setQuery(YdbTable.Query.newBuilder().setYqlText(query))
                .setMode(settings.getMode())
                .putAllParameters(params.toPb())
                .setCollectStats(settings.getCollectStats())
                .build();

        CompletableFuture<Status> promise = new CompletableFuture<>();
        final long deadlineAfter = settings.getDeadlineAfter();
        StreamControl control = tableRpc.streamExecuteScanQuery(request, new StreamObserver<YdbTable.ExecuteScanQueryPartialResponse>() {
            @Override
            public void onNext(YdbTable.ExecuteScanQueryPartialResponse response) {
                StatusIds.StatusCode statusCode = response.getStatus();
                if (statusCode == StatusIds.StatusCode.SUCCESS) {
                    try {
                        fn.accept(ProtoValueReaders.forResultSet(response.getResult().getResultSet()));
                    } catch (Throwable t) {
                        promise.completeExceptionally(t);
                        throw new IllegalStateException(t);
                    }
                } else {
                    Issue[] issues = Issue.fromPb(response.getIssuesList());
                    StatusCode code = StatusCode.fromProto(statusCode);
                    promise.complete(Status.of(code, issues));
                }
            }

            @Override
            public void onError(Status status) {
                assert !status.isSuccess();
                promise.complete(status);
            }

            @Override
            public void onCompleted() {
                promise.complete(Status.SUCCESS);
            }
        }, deadlineAfter);
        return promise.whenComplete((status, ex) -> {
            if (ex instanceof CancellationException) {
                control.cancel();
            }
        });
    }

    public CompletableFuture<Status> commitTransaction(String txId, CommitTxSettings settings) {
        YdbTable.CommitTransactionRequest request = YdbTable.CommitTransactionRequest.newBuilder()
            .setSessionId(id)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .setTxId(txId)
            .build();

        final long deadlineAfter = settings.getDeadlineAfter();
        return interceptStatus(tableRpc.commitTransaction(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return tableRpc.getOperationTray()
                    .waitStatus(response.expect("commitTransaction()").getOperation(), deadlineAfter);
            }));
    }

    public CompletableFuture<Status> rollbackTransaction(String txId, RollbackTxSettings settings) {
        YdbTable.RollbackTransactionRequest request = YdbTable.RollbackTransactionRequest.newBuilder()
            .setSessionId(id)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .setTxId(txId)
            .build();

        final long deadlineAfter = settings.getDeadlineAfter();
        return interceptStatus(tableRpc.rollbackTransaction(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return tableRpc.getOperationTray()
                    .waitStatus(response.expect("rollbackTransaction()").getOperation(), deadlineAfter);
            }));
    }

    @Override
    public CompletableFuture<Result<SessionStatus>> keepAlive(KeepAliveSessionSettings settings) {
        YdbTable.KeepAliveRequest request = YdbTable.KeepAliveRequest.newBuilder()
            .setSessionId(id)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .build();

        final long deadlineAfter = settings.getDeadlineAfter();
        return interceptResult(tableRpc.keepAlive(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationTray.waitResult(
                    response.expect("keepAlive()").getOperation(),
                    YdbTable.KeepAliveResult.class,
                    SessionImpl::mapSessionStatus,
                    deadlineAfter
                );
            }));
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
                .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
                .build();

        final long deadlineAfter = settings.getDeadlineAfter();

        return interceptStatus(tableRpc.bulkUpsert(request, deadlineAfter)
           .thenCompose(response -> {
               if (!response.isSuccess()) {
                   return CompletableFuture.completedFuture(response.toStatus());
               }
               return operationTray.waitStatus(response.expect("bulkUpsert()").getOperation(), deadlineAfter);
           }));
    }

    private static SessionStatus mapSessionStatus(YdbTable.KeepAliveResult result) {
        switch (result.getSessionStatus()) {
            case UNRECOGNIZED:
            case SESSION_STATUS_UNSPECIFIED: return SessionStatus.UNSPECIFIED;
            case SESSION_STATUS_BUSY: return SessionStatus.BUSY;
            case SESSION_STATUS_READY: return SessionStatus.READY;
        }
        throw new IllegalStateException("unknown session status: " + result.getSessionStatus());
    }

    @Override
    public void invalidateQueryCache() {
        if (queryCache != null) {
            queryCache.clear();
        }
    }

    @Override
    public boolean release() {
        if (sessionPool != null) {
            sessionPool.release(this);
            return true;
        }
        return false;
    }

    @Override
    public CompletableFuture<Status> close(CloseSessionSettings settings) {
        YdbTable.DeleteSessionRequest request = YdbTable.DeleteSessionRequest.newBuilder()
            .setSessionId(id)
            .setOperationParams(OperationParamUtils.fromRequestSettings(settings))
            .build();

        final long deadlineAfter = settings.getDeadlineAfter();
        return interceptStatus(tableRpc.deleteSession(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationTray.waitStatus(response.expect("deleteSession()").getOperation(), deadlineAfter);
            }));
    }

    private <T> CompletableFuture<Result<T>> interceptResult(CompletableFuture<Result<T>> future) {
        return future.whenComplete((r, t) -> {
            changeSessionState(t, r.getCode());
        });
    }

    private CompletableFuture<Status> interceptStatus(CompletableFuture<Status> future) {
        return future.whenComplete((r, t) -> {
            changeSessionState(t, r.getCode());
        });
    }

    private void changeSessionState(Throwable t, StatusCode code) {
        State oldState = getState();
        if (t != null) {
            switchState(oldState, State.BROKEN);
            return;
        }

        if (code.isTransportError() && code != StatusCode.CLIENT_RESOURCE_EXHAUSTED) {
            switchState(oldState, State.DISCONNECTED);
        } else if (code == StatusCode.BAD_SESSION) {
            switchState(oldState, State.BROKEN);
        } else if (code == StatusCode.SESSION_BUSY) {
            switchState(oldState, State.BROKEN);
        } else if (code == StatusCode.INTERNAL_ERROR) {
            switchState(oldState, State.BROKEN);
        }
    }

    @Override
    public String toString() {
        return "Session{" +
            "id='" + id + '\'' +
            ", state=" + state +
            '}';
    }
}
