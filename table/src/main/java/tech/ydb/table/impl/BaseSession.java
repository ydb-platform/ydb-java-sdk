package tech.ydb.table.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.google.protobuf.Timestamp;
import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.common.transaction.impl.YdbTransactionImpl;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.core.impl.call.ProxyReadStream;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.core.utils.URITools;
import tech.ydb.proto.StatusCodesProtos.StatusIds;
import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.ValueProtos.TypedValue;
import tech.ydb.proto.common.CommonProtos;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.Session;
import tech.ydb.table.description.ColumnFamily;
import tech.ydb.table.description.KeyBound;
import tech.ydb.table.description.KeyRange;
import tech.ydb.table.description.StoragePool;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.description.TableIndex;
import tech.ydb.table.query.DataQuery;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.ExplainDataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.query.ReadRowsResult;
import tech.ydb.table.query.ReadTablePart;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.AutoPartitioningPolicy;
import tech.ydb.table.settings.BeginTxSettings;
import tech.ydb.table.settings.BulkUpsertSettings;
import tech.ydb.table.settings.Changefeed;
import tech.ydb.table.settings.CommitTxSettings;
import tech.ydb.table.settings.CopyTableSettings;
import tech.ydb.table.settings.CopyTablesSettings;
import tech.ydb.table.settings.CreateSessionSettings;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.settings.DeleteSessionSettings;
import tech.ydb.table.settings.DescribeTableSettings;
import tech.ydb.table.settings.DropTableSettings;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.settings.ExecuteScanQuerySettings;
import tech.ydb.table.settings.ExecuteSchemeQuerySettings;
import tech.ydb.table.settings.ExplainDataQuerySettings;
import tech.ydb.table.settings.KeepAliveSessionSettings;
import tech.ydb.table.settings.PartitioningPolicy;
import tech.ydb.table.settings.PartitioningSettings;
import tech.ydb.table.settings.PrepareDataQuerySettings;
import tech.ydb.table.settings.ReadRowsSettings;
import tech.ydb.table.settings.ReadTableSettings;
import tech.ydb.table.settings.RenameTablesSettings;
import tech.ydb.table.settings.ReplicationPolicy;
import tech.ydb.table.settings.RequestSettings;
import tech.ydb.table.settings.RollbackTxSettings;
import tech.ydb.table.settings.StoragePolicy;
import tech.ydb.table.settings.TtlSettings;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.table.transaction.Transaction;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.ListValue;
import tech.ydb.table.values.StructValue;
import tech.ydb.table.values.TupleValue;
import tech.ydb.table.values.Value;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;



/**
 * @author Sergey Polovko
 * @author Alexandr Gorshenin
 * @author Nikolay Perfilov
 */
@ThreadSafe
public abstract class BaseSession implements Session {
    private static final String SERVER_BALANCER_HINT = "session-balancer";
    private static final Logger logger = LoggerFactory.getLogger(Session.class);

    private final String id;
    private final Integer prefferedNodeID;
    private final TableRpc tableRpc;
    private final ShutdownHandler shutdownHandler;
    private final boolean keepQueryText;

    protected BaseSession(String id, TableRpc tableRpc, boolean keepQueryText) {
        this.id = id;
        this.tableRpc = tableRpc;
        this.keepQueryText = keepQueryText;
        this.prefferedNodeID = getNodeBySessionId(id);
        this.shutdownHandler = new ShutdownHandler();
    }

    private static Integer getNodeBySessionId(String sessionId) {
        try {
            Map<String, List<String>> params = URITools.splitQuery(new URI(sessionId));
            List<String> nodeParam = params.get("node_id");
            if (nodeParam != null && !nodeParam.isEmpty()) {
                return Integer.parseUnsignedInt(nodeParam.get(0));
            }
        } catch (URISyntaxException | RuntimeException e) {
            logger.debug("Failed to parse session_id for node_id: {}", e.toString());
        }
        return null;
    }

    private GrpcRequestSettings makeGrpcRequestSettings(RequestSettings<?> settings) {
        Metadata headers = new Metadata();
        headers.put(YdbHeaders.TRACE_ID, settings.getTraceIdOrGenerateNew());
        return GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getTimeoutDuration())
                .withExtraHeaders(headers)
                .withPreferredNodeID(prefferedNodeID)
                .withTrailersHandler(shutdownHandler)
                .build();
    }

    private GrpcRequestSettings makeGrpcRequestSettings(BaseRequestSettings settings) {
        Metadata headers = new Metadata();
        headers.put(YdbHeaders.TRACE_ID, settings.getTraceIdOrGenerateNew());
        return GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withExtraHeaders(headers)
                .withPreferredNodeID(prefferedNodeID)
                .withTrailersHandler(shutdownHandler)
                .build();
    }

    @Override
    public String getId() {
        return id;
    }

    public static CompletableFuture<Result<String>> createSessionId(TableRpc tableRpc,
                                                                    CreateSessionSettings settings,
                                                                    boolean useServerBalancer) {
        YdbTable.CreateSessionRequest request = YdbTable.CreateSessionRequest.newBuilder()
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        Metadata headers = null;
        if (useServerBalancer) {
            headers = new Metadata();
            headers.put(YdbHeaders.YDB_CLIENT_CAPABILITIES, SERVER_BALANCER_HINT);
            headers.put(YdbHeaders.TRACE_ID, settings.getTraceIdOrGenerateNew());
        }

        GrpcRequestSettings grpcSettings = GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getTimeoutDuration())
                .withExtraHeaders(headers)
                .build();

        return tableRpc.createSession(request, grpcSettings)
                .thenApply(result -> result.map(YdbTable.CreateSessionResult::getSessionId));
    }

    private static YdbTable.PartitioningSettings buildPartitioningSettings(PartitioningSettings partitioningSettings) {
        YdbTable.PartitioningSettings.Builder builder = YdbTable.PartitioningSettings.newBuilder();
        if (partitioningSettings.getPartitioningByLoad() != null) {
            builder.setPartitioningByLoad(
                    partitioningSettings.getPartitioningByLoad()
                            ? CommonProtos.FeatureFlag.Status.ENABLED
                            : CommonProtos.FeatureFlag.Status.DISABLED
            );
        }
        if (partitioningSettings.getPartitioningBySize() != null) {
            builder.setPartitioningBySize(
                    partitioningSettings.getPartitioningBySize()
                            ? CommonProtos.FeatureFlag.Status.ENABLED
                            : CommonProtos.FeatureFlag.Status.DISABLED
            );
        }
        if (partitioningSettings.getPartitionSizeMb() != null) {
            builder.setPartitionSizeMb(partitioningSettings.getPartitionSizeMb());
        }
        if (partitioningSettings.getMinPartitionsCount() != null) {
            builder.setMinPartitionsCount(partitioningSettings.getMinPartitionsCount());
        }
        if (partitioningSettings.getMaxPartitionsCount() != null) {
            builder.setMaxPartitionsCount(partitioningSettings.getMaxPartitionsCount());
        }

        return builder.build();
    }

    private static YdbTable.ColumnMeta buildColumnMeta(TableColumn column) {
        YdbTable.ColumnMeta.Builder builder = YdbTable.ColumnMeta.newBuilder()
                .setName(column.getName())
                .setType(column.getType().toPb());
        if (column.getFamily() != null) {
            builder.setFamily(column.getFamily());
        }
        return builder.build();
    }

    private static YdbTable.Changefeed buildChangefeed(Changefeed changefeed) {
        YdbTable.Changefeed.Builder builder = YdbTable.Changefeed.newBuilder()
                .setName(changefeed.getName())
                .setFormat(changefeed.getFormat().toProto())
                .setVirtualTimestamps(changefeed.hasVirtualTimestamps())
                .setInitialScan(changefeed.hasInitialScan())
                .setMode(changefeed.getMode().toPb());

        Duration retentionPeriod = changefeed.getRetentionPeriod();
        if (retentionPeriod != null) {
            builder.setRetentionPeriod(com.google.protobuf.Duration.newBuilder()
                    .setSeconds(retentionPeriod.getSeconds())
                    .setNanos(retentionPeriod.getNano())
                    .build());
        }

        return builder.build();
    }

    private static YdbTable.TableIndex buildIndex(TableIndex index) {
        YdbTable.TableIndex.Builder builder = YdbTable.TableIndex.newBuilder();
        builder.setName(index.getName());
        builder.addAllIndexColumns(index.getColumns());
        builder.addAllDataColumns(index.getDataColumns());
        switch (index.getType()) {
            case GLOBAL_ASYNC:
                builder.setGlobalAsyncIndex(YdbTable.GlobalAsyncIndex.getDefaultInstance());
                break;
            case GLOBAL:
            default:
                builder.setGlobalIndex(YdbTable.GlobalIndex.getDefaultInstance());
                break;
        }
        return builder.build();
    }

    private static YdbTable.ColumnFamily buildColumnFamity(ColumnFamily family) {
        YdbTable.ColumnFamily.Compression compression;
        switch (family.getCompression()) {
            case COMPRESSION_NONE:
                compression = YdbTable.ColumnFamily.Compression.COMPRESSION_NONE;
                break;
            case COMPRESSION_LZ4:
                compression = YdbTable.ColumnFamily.Compression.COMPRESSION_LZ4;
                break;
            default:
                compression = YdbTable.ColumnFamily.Compression.COMPRESSION_UNSPECIFIED;
        }

        return YdbTable.ColumnFamily.newBuilder()
                .setCompression(compression)
                .setData(YdbTable.StoragePool.newBuilder().setMedia(family.getData().getMedia()))
                .setName(family.getName())
                .build();
    }

    private static YdbTable.TtlSettings buildTtlSettings(TtlSettings settings) {
        return YdbTable.TtlSettings.newBuilder()
                .setDateTypeColumn(YdbTable.DateTypeColumnModeSettings.newBuilder()
                        .setColumnName(settings.getDateTimeColumn())
                        .setExpireAfterSeconds(settings.getExpireAfterSeconds())
                        .build())
                .build();
    }

    @Override
    public CompletableFuture<Status> createTable(
            String path,
            TableDescription description,
            CreateTableSettings settings
    ) {
        YdbTable.CreateTableRequest.Builder request = YdbTable.CreateTableRequest.newBuilder()
                .setSessionId(id)
                .setPath(path)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .addAllPrimaryKey(description.getPrimaryKeys());

        for (ColumnFamily family: description.getColumnFamilies()) {
            request.addColumnFamilies(buildColumnFamity(family));
        }

        for (TableColumn column : description.getColumns()) {
            request.addColumns(buildColumnMeta(column));
        }

        for (TableIndex index : description.getIndexes()) {
            request.addIndexes(buildIndex(index));
        }

        if (settings.getTtlSettings() != null) {
            request.setTtlSettings(buildTtlSettings(settings.getTtlSettings()));
        }

        if (description.getPartitioningSettings() != null) {
            request.setPartitioningSettings(buildPartitioningSettings(description.getPartitioningSettings()));
        }

        if (settings.getPresetName() != null) {
            request.getProfileBuilder().setPresetName(settings.getPresetName());
        }

        if (settings.getExecutionPolicy() != null) {
            request.getProfileBuilder().getExecutionPolicyBuilder().setPresetName(settings.getExecutionPolicy());
        }

        if (settings.getCompactionPolicy() != null) {
            request.getProfileBuilder().getCompactionPolicyBuilder().setPresetName(settings.getCompactionPolicy());
        }

        PartitioningPolicy partitioningPolicy = settings.getPartitioningPolicy();
        if (partitioningPolicy != null) {
            YdbTable.PartitioningPolicy.Builder policyProto = request.getProfileBuilder()
                    .getPartitioningPolicyBuilder();
            if (partitioningPolicy.getPresetName() != null) {
                policyProto.setPresetName(partitioningPolicy.getPresetName());
            }
            if (partitioningPolicy.getAutoPartitioning() != null) {
                policyProto.setAutoPartitioning(toPb(partitioningPolicy.getAutoPartitioning()));
            }

            if (partitioningPolicy.getUniformPartitions() > 0) {
                policyProto.setUniformPartitions(partitioningPolicy.getUniformPartitions());
            } else {
                List<TupleValue> points = partitioningPolicy.getExplicitPartitioningPoints();
                if (points != null) {
                    YdbTable.ExplicitPartitions.Builder b = policyProto.getExplicitPartitionsBuilder();
                    for (Value<?> p : points) {
                        b.addSplitPoints(ProtoValue.toTypedValue(p));
                    }
                }
            }
        }

        StoragePolicy storagePolicy = settings.getStoragePolicy();
        if (storagePolicy != null) {
            YdbTable.StoragePolicy.Builder policyProto = request.getProfileBuilder()
                    .getStoragePolicyBuilder();
            if (storagePolicy.getPresetName() != null) {
                policyProto.setPresetName(storagePolicy.getPresetName());
            }
            if (storagePolicy.getSysLog() != null) {
                policyProto.getSyslogBuilder().setMedia(storagePolicy.getSysLog());
            }
            if (storagePolicy.getLog() != null) {
                policyProto.getLogBuilder().setMedia(storagePolicy.getLog());
            }
            if (storagePolicy.getData() != null) {
                policyProto.getDataBuilder().setMedia(storagePolicy.getData());
            }
            if (storagePolicy.getExternal() != null) {
                policyProto.getExternalBuilder().setMedia(storagePolicy.getExternal());
            }
        }

        ReplicationPolicy replicationPolicy = settings.getReplicationPolicy();
        if (replicationPolicy != null) {
            YdbTable.ReplicationPolicy.Builder replicationPolicyProto =
                    request.getProfileBuilder().getReplicationPolicyBuilder();
            if (replicationPolicy.getPresetName() != null) {
                replicationPolicyProto.setPresetName(replicationPolicy.getPresetName());
            }
            replicationPolicyProto.setReplicasCount(replicationPolicy.getReplicasCount());
            replicationPolicyProto.setCreatePerAvailabilityZone(replicationPolicy.isCreatePerAvailabilityZone() ?
                    CommonProtos.FeatureFlag.Status.ENABLED : CommonProtos.FeatureFlag.Status.DISABLED);
            replicationPolicyProto.setAllowPromotion(replicationPolicy.isAllowPromotion() ?
                    CommonProtos.FeatureFlag.Status.ENABLED : CommonProtos.FeatureFlag.Status.DISABLED);
        }

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return tableRpc.createTable(request.build(), grpcRequestSettings);
    }

    private static YdbTable.PartitioningPolicy.AutoPartitioningPolicy toPb(AutoPartitioningPolicy policy) {
        switch (policy) {
            case AUTO_SPLIT:
                return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.AUTO_SPLIT;
            case AUTO_SPLIT_MERGE:
                return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.AUTO_SPLIT_MERGE;
            case DISABLED:
                return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.DISABLED;
            default:
                throw new IllegalArgumentException("unknown AutoPartitioningPolicy: " + policy);
        }
    }

    @Override
    public CompletableFuture<Status> dropTable(String path, DropTableSettings settings) {
        YdbTable.DropTableRequest request = YdbTable.DropTableRequest.newBuilder()
                .setSessionId(id)
                .setPath(path)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return tableRpc.dropTable(request, grpcRequestSettings);
    }

    @Override
    public CompletableFuture<Status> alterTable(String path, AlterTableSettings settings) {
        YdbTable.AlterTableRequest.Builder builder = YdbTable.AlterTableRequest.newBuilder()
                .setSessionId(id)
                .setPath(path)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()));

        for (TableColumn addColumn: settings.getAddColumns()) {
            builder.addAddColumns(buildColumnMeta(addColumn));
        }

        for (Changefeed addChangefeed: settings.getAddChangefeeds()) {
            builder.addAddChangefeeds(buildChangefeed(addChangefeed));
        }

        for (TableIndex index : settings.getAddIndexes()) {
            builder.addAddIndexes(buildIndex(index));
        }

        if (settings.getTtlSettings() != null) {
            builder.setSetTtlSettings(buildTtlSettings(settings.getTtlSettings()));
        }

        if (settings.getPartitioningSettings() != null) {
            builder.setAlterPartitioningSettings(buildPartitioningSettings(settings.getPartitioningSettings()));
        }

        for (String dropColumn: settings.getDropColumns()) {
            builder.addDropColumns(dropColumn);
        }

        for (String dropChangefeed: settings.getDropChangefeeds()) {
            builder.addDropChangefeeds(dropChangefeed);
        }

        for (String dropIndex: settings.getDropIndexes()) {
            builder.addDropIndexes(dropIndex);
        }

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return tableRpc.alterTable(builder.build(), grpcRequestSettings);
    }

    @Override
    public CompletableFuture<Status> copyTable(String src, String dst, CopyTableSettings settings) {
        YdbTable.CopyTableRequest request = YdbTable.CopyTableRequest.newBuilder()
                .setSessionId(id)
                .setSourcePath(src)
                .setDestinationPath(dst)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return tableRpc.copyTable(request, grpcRequestSettings);
    }

    @Override
    public CompletableFuture<Status> copyTables(CopyTablesSettings settings) {
        YdbTable.CopyTablesRequest request = YdbTable.CopyTablesRequest.newBuilder()
                .setSessionId(id)
                .addAllTables(convertCopyTableItems(settings))
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return tableRpc.copyTables(request, grpcRequestSettings);
    }

    private List<YdbTable.CopyTableItem> convertCopyTableItems(CopyTablesSettings cts) {
        final String dbpath = tableRpc.getDatabase();
        return cts.getItems().stream().map(t -> {
            String sp = t.getSourcePath();
            if (!sp.startsWith("/")) {
                sp = dbpath + "/" + sp;
            }
            String dp = t.getDestinationPath();
            if (!dp.startsWith("/")) {
                dp = dbpath + "/" + dp;
            }
            return YdbTable.CopyTableItem.newBuilder()
                    .setSourcePath(sp)
                    .setDestinationPath(dp)
                    .setOmitIndexes(t.isOmitIndexes())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<Status> renameTables(RenameTablesSettings settings) {
        YdbTable.RenameTablesRequest request = YdbTable.RenameTablesRequest.newBuilder()
                .setSessionId(id)
                .addAllTables(convertRenameTableItems(settings))
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return tableRpc.renameTables(request, grpcRequestSettings);
    }

    private List<YdbTable.RenameTableItem> convertRenameTableItems(RenameTablesSettings cts) {
        final String dbpath = tableRpc.getDatabase();
        return cts.getItems().stream().map(t -> {
            String sp = t.getSourcePath();
            if (!sp.startsWith("/")) {
                sp = dbpath + "/" + sp;
            }
            String dp = t.getDestinationPath();
            if (!dp.startsWith("/")) {
                dp = dbpath + "/" + dp;
            }
            return YdbTable.RenameTableItem.newBuilder()
                    .setSourcePath(sp)
                    .setDestinationPath(dp)
                    .setReplaceDestination(t.isReplaceDestination())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<Result<TableDescription>> describeTable(String path, DescribeTableSettings settings) {
        YdbTable.DescribeTableRequest request = YdbTable.DescribeTableRequest.newBuilder()
                .setSessionId(id)
                .setPath(path)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setIncludeTableStats(settings.isIncludeTableStats())
                .setIncludeShardKeyBounds(settings.isIncludeShardKeyBounds())
                .setIncludePartitionStats(settings.isIncludePartitionStats())
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return tableRpc.describeTable(request, grpcRequestSettings)
                .thenApply(result -> result.map(desc -> mapDescribeTable(desc, settings)));
    }

    private static TableDescription mapDescribeTable(
            YdbTable.DescribeTableResult result,
            DescribeTableSettings describeTableSettings
    ) {
        TableDescription.Builder description = TableDescription.newBuilder();
        for (int i = 0; i < result.getColumnsCount(); i++) {
            YdbTable.ColumnMeta column = result.getColumns(i);
            description.addNonnullColumn(column.getName(), ProtoType.fromPb(column.getType()), column.getFamily());
        }
        description.setPrimaryKeys(result.getPrimaryKeyList());
        for (int i = 0; i < result.getIndexesCount(); i++) {
            YdbTable.TableIndexDescription idx = result.getIndexes(i);

            if (idx.hasGlobalIndex()) {
                description.addGlobalIndex(idx.getName(), idx.getIndexColumnsList(), idx.getDataColumnsList());
            }

            if (idx.hasGlobalAsyncIndex()) {
                description.addGlobalAsyncIndex(idx.getName(), idx.getIndexColumnsList(), idx.getDataColumnsList());
            }
        }
        YdbTable.TableStats tableStats = result.getTableStats();
        if (describeTableSettings.isIncludeTableStats() && tableStats != null) {
            Timestamp creationTime = tableStats.getCreationTime();
            Instant createdAt = creationTime == null ? null : Instant.ofEpochSecond(creationTime.getSeconds(),
                    creationTime.getNanos());
            Timestamp modificationTime = tableStats.getCreationTime();
            Instant modifiedAt = modificationTime == null ? null : Instant.ofEpochSecond(modificationTime.getSeconds(),
                    modificationTime.getNanos());
            TableDescription.TableStats stats = new TableDescription.TableStats(
                    createdAt, modifiedAt, tableStats.getRowsEstimate(), tableStats.getStoreSize());
            description.setTableStats(stats);

            List<YdbTable.PartitionStats> partitionStats = tableStats.getPartitionStatsList();
            if (describeTableSettings.isIncludePartitionStats() && partitionStats != null) {
                for (YdbTable.PartitionStats stat : partitionStats) {
                    description.addPartitionStat(stat.getRowsEstimate(), stat.getStoreSize());
                }
            }
        }
        YdbTable.PartitioningSettings partitioningSettings = result.getPartitioningSettings();
        if (partitioningSettings != null) {
            PartitioningSettings settings = new PartitioningSettings();
            settings.setPartitionSize(partitioningSettings.getPartitionSizeMb());
            settings.setMinPartitionsCount(partitioningSettings.getMinPartitionsCount());
            settings.setMaxPartitionsCount(partitioningSettings.getMaxPartitionsCount());
            settings.setPartitioningByLoad(partitioningSettings.getPartitioningByLoad() == CommonProtos.
                    FeatureFlag.Status.ENABLED);
            settings.setPartitioningBySize(partitioningSettings.getPartitioningBySize() == CommonProtos.
                    FeatureFlag.Status.ENABLED);
            description.setPartitioningSettings(settings);
        }

        List<YdbTable.ColumnFamily> columnFamiliesList = result.getColumnFamiliesList();
        if (columnFamiliesList != null) {
            for (YdbTable.ColumnFamily family : columnFamiliesList) {
                ColumnFamily.Compression compression;
                switch (family.getCompression()) {
                    case COMPRESSION_LZ4:
                        compression = ColumnFamily.Compression.COMPRESSION_LZ4;
                        break;
                    default:
                        compression = ColumnFamily.Compression.COMPRESSION_NONE;
                }
                description.addColumnFamily(
                        new ColumnFamily(family.getName(), new StoragePool(family.getData().getMedia()), compression)
                );
            }
        }
        if (describeTableSettings.isIncludeShardKeyBounds()) {
            List<ValueProtos.TypedValue> shardKeyBoundsList = result.getShardKeyBoundsList();
            if (shardKeyBoundsList != null) {
                Optional<Value<?>> leftValue = Optional.empty();
                for (ValueProtos.TypedValue typedValue : shardKeyBoundsList) {
                    Optional<KeyBound> fromBound = leftValue.map(KeyBound::inclusive);
                    Value<?> value = ProtoValue.fromPb(
                            ProtoType.fromPb(typedValue.getType()),
                            typedValue.getValue()
                    );
                    Optional<KeyBound> toBound = Optional.of(KeyBound.exclusive(value));
                    description.addKeyRange(new KeyRange(fromBound, toBound));
                    leftValue = Optional.of(value);
                }
                description.addKeyRange(
                        new KeyRange(leftValue.map(KeyBound::inclusive), Optional.empty())
                );
            }
        }

        YdbTable.TtlSettings ttlSettings = result.getTtlSettings();
        int ttlModeCase = ttlSettings.getModeCase().getNumber();
        switch (ttlSettings.getModeCase()) {
            case DATE_TYPE_COLUMN:
                YdbTable.DateTypeColumnModeSettings dateTypeColumn = ttlSettings.getDateTypeColumn();
                description.setTtlSettings(ttlModeCase, dateTypeColumn.getColumnName(),
                        dateTypeColumn.getExpireAfterSeconds());
                break;
            case VALUE_SINCE_UNIX_EPOCH:
                YdbTable.ValueSinceUnixEpochModeSettings valueSinceUnixEpoch = ttlSettings.getValueSinceUnixEpoch();
                description.setTtlSettings(ttlModeCase, valueSinceUnixEpoch.getColumnName(),
                        valueSinceUnixEpoch.getExpireAfterSeconds());
                break;
            default:
                break;
        }

        return description.build();
    }

    private static YdbTable.TransactionSettings txSettings(Transaction.Mode transactionMode) {
        YdbTable.TransactionSettings.Builder settings = YdbTable.TransactionSettings.newBuilder();
        if (transactionMode != null) {
            switch (transactionMode) {
                case SERIALIZABLE_READ_WRITE:
                    settings.setSerializableReadWrite(YdbTable.SerializableModeSettings.getDefaultInstance());
                    break;
                case ONLINE_READ_ONLY:
                    settings.setOnlineReadOnly(YdbTable.OnlineModeSettings.getDefaultInstance());
                    break;
                case STALE_READ_ONLY:
                    settings.setStaleReadOnly(YdbTable.StaleModeSettings.getDefaultInstance());
                    break;
                case SNAPSHOT_READ_ONLY:
                    settings.setSnapshotReadOnly(YdbTable.SnapshotModeSettings.getDefaultInstance());
                    break;
                default:
                    break;
            }
        }
        return settings.build();
    }

    private CompletableFuture<Result<DataQueryResult>> executeDataQueryInternal(
            String query, YdbTable.TransactionControl txControl, Params params, ExecuteDataQuerySettings settings) {
        YdbTable.ExecuteDataQueryRequest.Builder request = YdbTable.ExecuteDataQueryRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxControl(txControl)
                .setQuery(YdbTable.Query.newBuilder().setYqlText(query))
                .setCollectStats(settings.collectStats().toPb())
                .putAllParameters(params.toPb());

        final boolean keepInServerQueryCache = settings.isKeepInQueryCache();
        if (keepInServerQueryCache) {
            request.getQueryCachePolicyBuilder()
                    .setKeepInCache(true);
        }

        String msg = "query";
        if (logger.isDebugEnabled() && keepQueryText) {
            StringBuilder sb = new StringBuilder(query.replaceAll("\\s", " "));
            if (!params.isEmpty()) {
                sb.append(" [");
                boolean one = true;
                for (Map.Entry<String, Value<?>> entry : params.values().entrySet()) {
                    if (!one) {
                        sb.append(", ");
                    }
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                    one = false;
                }
                sb.append("]");
            }
            msg = sb.toString();
        }

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptResultWithLog(msg, tableRpc.executeDataQuery(request.build(), grpcRequestSettings))
                .thenApply(result -> result.map(DataQueryResult::new));
    }

    @Override
    public CompletableFuture<Result<DataQueryResult>> executeDataQuery(
            String query, TxControl<?> txControl, Params params, ExecuteDataQuerySettings settings) {
        return executeDataQueryInternal(query, txControl.toPb(), params, settings);
    }

    @Override
    public CompletableFuture<Result<ReadRowsResult>> readRows(String pathToTable, ReadRowsSettings settings) {
        YdbTable.ReadRowsRequest.Builder requestBuilder = YdbTable.ReadRowsRequest.newBuilder()
                .setSessionId(id)
                .setPath(pathToTable)
                .addAllColumns(settings.getColumns())
                .setKeys(settings.getKeys().isEmpty() ? TypedValue.newBuilder().build() :
                    ValueProtos.TypedValue.newBuilder()
                            .setType(ListType.of(settings.getKeys().get(0).getType()).toPb())
                            .setValue(ValueProtos.Value.newBuilder()
                                    .addAllItems(settings.getKeys().stream().map(StructValue::toPb)
                                            .collect(Collectors.toList())))
                            .build());
        return interceptResult(
                tableRpc.readRows(requestBuilder.build(), makeGrpcRequestSettings(settings)))
                .thenApply(result -> result.map(ReadRowsResult::new));
    }

    CompletableFuture<Result<DataQueryResult>> executePreparedDataQuery(String queryId, @Nullable String queryText,
                                                                        TxControl<?> txControl, Params params,
                                                                        ExecuteDataQuerySettings settings) {
        YdbTable.ExecuteDataQueryRequest.Builder request = YdbTable.ExecuteDataQueryRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxControl(txControl.toPb())
                .setCollectStats(settings.collectStats().toPb());

        request.getQueryBuilder().setId(queryId);
        request.putAllParameters(params.toPb());

        final boolean keepInServerQueryCache = settings.isKeepInQueryCache();
        if (keepInServerQueryCache) {
            request.getQueryCachePolicyBuilder()
                    .setKeepInCache(true);
        }

        String msg = "prepared query";
        if (logger.isDebugEnabled() && keepQueryText) {
            StringBuilder sb = new StringBuilder("prepared,");
            if (queryText != null) {
                sb.append(queryText.replaceAll("\\s", " "));
            }
            if (!params.isEmpty()) {
                sb.append(" [");
                boolean one = true;
                for (Map.Entry<String, Value<?>> entry : params.values().entrySet()) {
                    if (!one) {
                        sb.append(", ");
                    }
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                    one = false;
                }
                sb.append("]");
            }
            msg = sb.toString();
        }

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptResultWithLog(msg, tableRpc.executeDataQuery(request.build(), grpcRequestSettings))
                .thenApply(result -> result.map(DataQueryResult::new));
    }

    @Override
    public CompletableFuture<Result<DataQuery>> prepareDataQuery(String query, PrepareDataQuerySettings settings) {
        YdbTable.PrepareDataQueryRequest.Builder request = YdbTable.PrepareDataQueryRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setYqlText(query);

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptResult(tableRpc.prepareDataQuery(request.build(), grpcRequestSettings))
                .thenApply(result -> result.map((value) -> {
                    String queryId = value.getQueryId();
                    Map<String, ValueProtos.Type> types = value.getParametersTypesMap();
                    return new DataQueryImpl(this, queryId, query, keepQueryText, types);
                }));
    }

    @Override
    public CompletableFuture<Status> executeSchemeQuery(String query, ExecuteSchemeQuerySettings settings) {
        YdbTable.ExecuteSchemeQueryRequest request = YdbTable.ExecuteSchemeQueryRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setYqlText(query)
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptStatus(tableRpc.executeSchemeQuery(request, grpcRequestSettings));
    }

    @Override
    public CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(String query,
                                                                              ExplainDataQuerySettings settings) {
        YdbTable.ExplainDataQueryRequest request = YdbTable.ExplainDataQueryRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setYqlText(query)
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptResult(tableRpc.explainDataQuery(request, grpcRequestSettings))
                .thenApply(result -> result.map(ExplainDataQueryResult::new));
    }

    @Override
    public CompletableFuture<Result<Transaction>> beginTransaction(Transaction.Mode transactionMode,
                                                                   BeginTxSettings settings) {
        YdbTable.BeginTransactionRequest request = YdbTable.BeginTransactionRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxSettings(txSettings(transactionMode))
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptResultWithLog("begin transaction",
                tableRpc.beginTransaction(request, grpcRequestSettings))
                .thenApply(result -> result.map(tx -> new DeprecatedTransactionImpl(tx.getTxMeta().getId())));
    }

    @Override
    public TableTransaction createNewTransaction(TxMode txMode) {
        return new TableTransactionImpl(txMode, null);
    }

    @Override
    public CompletableFuture<Result<TableTransaction>> beginTransaction(TxMode txMode, BeginTxSettings settings) {
        YdbTable.BeginTransactionRequest request = YdbTable.BeginTransactionRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxSettings(TxControlToPb.txSettings(txMode))
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptResultWithLog("begin transaction",
                tableRpc.beginTransaction(request, grpcRequestSettings))
                .thenApply(result -> result.map(tx -> new TableTransactionImpl(txMode, tx.getTxMeta().getId())));
    }

    @Override
    public GrpcReadStream<ReadTablePart> executeReadTable(String tablePath, ReadTableSettings settings) {
        YdbTable.ReadTableRequest.Builder request = YdbTable.ReadTableRequest.newBuilder()
                .setSessionId(id)
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

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        final GrpcReadStream<YdbTable.ReadTableResponse> origin = tableRpc.streamReadTable(
                request.build(), grpcRequestSettings
        );

        return new ProxyReadStream<>(origin, (response, future, observer) -> {
            StatusIds.StatusCode statusCode = response.getStatus();
            if (statusCode == StatusIds.StatusCode.SUCCESS) {
                try {
                    observer.onNext(new ReadTablePart(response.getResult(), response.getSnapshot()));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                    origin.cancel();
                }
            } else {
                Issue[] issues = Issue.fromPb(response.getIssuesList());
                StatusCode code = StatusCode.fromProto(statusCode);
                future.complete(Status.of(code, null, issues));
                origin.cancel();
            }
        });
    }

    @Override
    public GrpcReadStream<ResultSetReader> executeScanQuery(String query, Params params,
                                                            ExecuteScanQuerySettings settings
    ) {
        YdbTable.ExecuteScanQueryRequest request = YdbTable.ExecuteScanQueryRequest.newBuilder()
                .setQuery(YdbTable.Query.newBuilder().setYqlText(query))
                .setMode(settings.getMode().toPb())
                .putAllParameters(params.toPb())
                .setCollectStats(settings.getCollectStats().toPb())
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        final GrpcReadStream<YdbTable.ExecuteScanQueryPartialResponse> origin = tableRpc.streamExecuteScanQuery(
                request, grpcRequestSettings
        );

        return new ProxyReadStream<>(origin, (response, future, observer) -> {
            StatusIds.StatusCode statusCode = response.getStatus();
            if (statusCode == StatusIds.StatusCode.SUCCESS) {
                try {
                    observer.onNext(ProtoValueReaders.forResultSet(response.getResult().getResultSet()));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                    origin.cancel();
                }
            } else {
                Issue[] issues = Issue.fromPb(response.getIssuesList());
                StatusCode code = StatusCode.fromProto(statusCode);
                future.complete(Status.of(code, null, issues));
                origin.cancel();
            }
        });
    }

    private CompletableFuture<Status> commitTransactionInternal(String txId, CommitTxSettings settings) {
        YdbTable.CommitTransactionRequest request = YdbTable.CommitTransactionRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxId(txId)
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptStatusWithLog("commit transaction",
                tableRpc.commitTransaction(request, grpcRequestSettings));
    }

    @Override
    public CompletableFuture<Status> commitTransaction(String txId, CommitTxSettings settings) {
        return commitTransactionInternal(txId, settings);
    }

    private CompletableFuture<Status> rollbackTransactionInternal(String txId, RollbackTxSettings settings) {
        YdbTable.RollbackTransactionRequest request = YdbTable.RollbackTransactionRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .setTxId(txId)
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptStatusWithLog("rollback transaction",
                tableRpc.rollbackTransaction(request, grpcRequestSettings));
    }

    @Override
    @Deprecated
    public CompletableFuture<Status> rollbackTransaction(String txId, RollbackTxSettings settings) {
        return rollbackTransactionInternal(txId, settings);
    }

    @Override
    public CompletableFuture<Result<State>> keepAlive(KeepAliveSessionSettings settings) {
        YdbTable.KeepAliveRequest request = YdbTable.KeepAliveRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptResult(tableRpc.keepAlive(request, grpcRequestSettings))
                .thenApply(result -> result.map(BaseSession::mapSessionStatus));
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

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);

        return interceptStatus(tableRpc.bulkUpsert(request, grpcRequestSettings));
    }

    private static State mapSessionStatus(YdbTable.KeepAliveResult result) {
        switch (result.getSessionStatus()) {
            case UNRECOGNIZED:
            case SESSION_STATUS_UNSPECIFIED:
                return State.UNSPECIFIED;
            case SESSION_STATUS_BUSY:
                return State.BUSY;
            case SESSION_STATUS_READY:
                return State.READY;
            default:
                throw new IllegalStateException("unknown session status: " + result.getSessionStatus());
        }
    }

    public CompletableFuture<Status> delete(DeleteSessionSettings settings) {
        YdbTable.DeleteSessionRequest request = YdbTable.DeleteSessionRequest.newBuilder()
                .setSessionId(id)
                .setOperationParams(Operation.buildParams(settings.toOperationSettings()))
                .build();

        final GrpcRequestSettings grpcRequestSettings = makeGrpcRequestSettings(settings);
        return interceptStatus(tableRpc.deleteSession(request, grpcRequestSettings));
    }

    private <T> CompletableFuture<Result<T>> interceptResultWithLog(String msg, CompletableFuture<Result<T>> future) {
        final long start = Instant.now().toEpochMilli();
        return future.whenComplete((r, t) -> {
            long ms = Instant.now().toEpochMilli() - start;
            logger.debug("Session[{}] {} => {}, took {} ms", getId(), msg, r.getStatus().getCode(), ms);
            updateSessionState(t, r.getStatus().getCode(), shutdownHandler.isGracefulShutdown());
        });
    }

    private <T> CompletableFuture<Result<T>> interceptResult(CompletableFuture<Result<T>> future) {
        return future.whenComplete((r, t) -> {
            updateSessionState(t, r.getStatus().getCode(), shutdownHandler.isGracefulShutdown());
        });
    }

    private CompletableFuture<Status> interceptStatus(CompletableFuture<Status> future) {
        return future.whenComplete((r, t) -> {
            updateSessionState(t, r.getCode(), shutdownHandler.isGracefulShutdown());
        });
    }

    private CompletableFuture<Status> interceptStatusWithLog(String msg, CompletableFuture<Status> future) {
        final long start = Instant.now().toEpochMilli();
        return future.whenComplete((r, t) -> {
            long ms = Instant.now().toEpochMilli() - start;
            logger.debug("Session[{}] {} => {}, took {} ms", getId(), msg, r.getCode(), ms);
            updateSessionState(t, r.getCode(), shutdownHandler.isGracefulShutdown());
        });
    }

    protected abstract void updateSessionState(Throwable th, StatusCode code, boolean shutdownHint);

    @Override
    public String toString() {
        return "Session{" + id + "}";
    }

    class TableTransactionImpl extends YdbTransactionImpl implements TableTransaction {

        TableTransactionImpl(TxMode txMode, String txId) {
            super(txMode, txId);
        }

        @Override
        public String getSessionId() {
            return id;
        }

        @Override
        public Session getSession() {
            return BaseSession.this;
        }

        @Override
        public CompletableFuture<Result<DataQueryResult>> executeDataQuery(
                String query, boolean commitAtEnd, Params params, ExecuteDataQuerySettings settings) {
            // If we intend to commit, statusFuture is reset to reflect only future actions in transaction
            CompletableFuture<Status> currentStatusFuture = commitAtEnd
                    ? statusFuture.getAndSet(new CompletableFuture<>())
                    : statusFuture.get();
            final String currentId = txId.get();
            YdbTable.TransactionControl transactionControl = currentId != null
                    ? TxControlToPb.txIdCtrl(currentId, commitAtEnd)
                    : TxControlToPb.txModeCtrl(txMode, commitAtEnd);
            return executeDataQueryInternal(query, transactionControl, params, settings)
                    .whenComplete((result, th) -> {
                        if (th != null) {
                            currentStatusFuture.completeExceptionally(
                                    new RuntimeException("ExecuteDataQuery on transaction failed with exception ", th));
                            setNewId(currentId, null);
                        } else if (result.isSuccess()) {
                            setNewId(currentId, result.getValue().getTxId());
                            if (commitAtEnd) {
                                currentStatusFuture.complete(Status.SUCCESS);
                            }
                        } else {
                            setNewId(currentId, null);
                            currentStatusFuture.complete(Status
                                    .of(StatusCode.ABORTED)
                                    .withIssues(Issue.of("ExecuteDataQuery on transaction failed with status "
                                                    + result.getStatus(), Issue.Severity.ERROR)));
                        }
                    });
        }

        private void setNewId(String currentId, String newId) {
            if (!txId.compareAndSet(currentId, newId)) {
                logger.warn("{} Couldn't change transaction id from {} to {}", BaseSession.this, currentId, newId);
            }
        }

        @Override
        public CompletableFuture<Status> commit(CommitTxSettings settings) {
            CompletableFuture<Status> currentStatusFuture = statusFuture.getAndSet(new CompletableFuture<>());
            final String transactionId = txId.get();
            if (transactionId == null) {
                Issue issue = Issue.of("Transaction is not started", Issue.Severity.WARNING);
                return CompletableFuture.completedFuture(Status.of(StatusCode.SUCCESS, null, issue));
            }
            return commitTransactionInternal(transactionId, settings).whenComplete(((status, th) -> {
                if (th != null) {
                    currentStatusFuture.completeExceptionally(th);
                } else {
                    currentStatusFuture.complete(status);
                }
            }));
        }

        @Override
        public CompletableFuture<Status> rollback(RollbackTxSettings settings) {
            CompletableFuture<Status> currentStatusFuture = statusFuture.getAndSet(new CompletableFuture<>());
            final String transactionId = txId.get();
            if (transactionId == null) {
                Issue issue = Issue.of("Transaction is not started", Issue.Severity.WARNING);
                return CompletableFuture.completedFuture(Status.of(StatusCode.SUCCESS, null, issue));
            }
            return rollbackTransactionInternal(transactionId, settings)
                    .whenComplete((status, th) -> currentStatusFuture.complete(Status
                            .of(StatusCode.ABORTED)
                            .withIssues(Issue.of("Transaction was rolled back", Issue.Severity.ERROR))));
        }
    }

    public final class DeprecatedTransactionImpl implements Transaction {
        private final String txId;

        DeprecatedTransactionImpl(String txId) {
            this.txId = txId;
        }

        @Override
        public String getId() {
            return txId;
        }

        @Override
        public CompletableFuture<Status> commit(CommitTxSettings settings) {
            return commitTransactionInternal(txId, settings);
        }

        @Override
        public CompletableFuture<Status> rollback(RollbackTxSettings settings) {
            return rollbackTransactionInternal(txId, settings);
        }
    }

    private static class ShutdownHandler implements Consumer<Metadata> {
        private static final String GRACEFUL_SHUTDOWN_HINT = "session-close";
        private volatile boolean needShutdown;

        public boolean isGracefulShutdown() {
            return needShutdown;
        }

        @Override
        public void accept(Metadata metadata) {
            if (metadata == null) {
                return;
            }

            Iterable<String> serverHints = metadata.getAll(YdbHeaders.YDB_SERVER_HINTS);
            if (serverHints != null) {
                for (String value : serverHints) {
                    if (GRACEFUL_SHUTDOWN_HINT.equals(value)) {
                        needShutdown = true;
                        return;
                    }
                }
            }
        }
    }
}
